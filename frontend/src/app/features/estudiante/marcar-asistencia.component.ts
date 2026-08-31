import { Component, ElementRef, OnDestroy, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import jsQR from 'jsqr';
import { MarcarAsistenciaService } from './marcar-asistencia.service';
import { MarcarAsistenciaResponse } from './marcar-asistencia.models';

@Component({
  selector: 'app-marcar-asistencia',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="pantalla">
      <h1 class="titulo-pantalla">Marcar asistencia</h1>

      @if (resultado(); as r) {
        <div class="card resultado" [class.resultado--tarde]="r.estado === 'TARDE'">
          <span class="resultado__icono">
            @if (r.estado === 'PRESENTE') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
            } @else {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
            }
          </span>
          <p class="resultado__estado">{{ r.estado === 'PRESENTE' ? '¡Presente!' : 'Marcado como tarde' }}</p>
          <p class="resultado__detalle">Tu asistencia quedó registrada.</p>
          <button class="btn btn--secondary" (click)="reiniciar()">Escanear otro código</button>
        </div>
      } @else if (fallo(); as mensaje) {
        <div class="card resultado resultado--fallo">
          <span class="resultado__icono resultado__icono--fallo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>
          </span>
          <p class="resultado__estado">No se pudo marcar</p>
          <p class="resultado__detalle">{{ mensaje }}</p>
          <button class="btn btn--secondary" (click)="reintentar()">Reintentar</button>
        </div>
      } @else {
        <div class="card camara-tarjeta">
          <div class="camara-envoltura" [class.oculta]="!camaraActiva()">
            <video #video class="camara" autoplay playsinline muted></video>
            <div class="camara-marco"></div>
          </div>
          <canvas #canvas hidden></canvas>

          @if (!camaraActiva()) {
            <div class="vacio">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg>
              <p>Activa la cámara y enfoca el código QR que muestra recepción.</p>
              <button class="btn btn--primary btn--block" (click)="iniciarCamara()">Activar cámara</button>
            </div>
          } @else if (enviando()) {
            <p class="aviso"><span class="spinner spinner--dark"></span> Verificando…</p>
          } @else {
            <p class="aviso">Buscando código…</p>
          }

          @if (error()) {
            <div class="alert alert--danger">{{ error() }}</div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .pantalla { max-width: 480px; margin: 0 auto; padding: 1.5rem 1.25rem 3rem; }
    .titulo-pantalla { font-size: 1.2rem; margin-bottom: 1.1rem; }
    .camara-tarjeta { padding: 1.5rem; display: flex; flex-direction: column; align-items: center; gap: 1rem; }
    .camara-envoltura {
      position: relative; width: 100%; aspect-ratio: 1 / 1; border-radius: var(--radius-md);
      overflow: hidden; background: #0f172a;
    }
    .camara-envoltura.oculta { display: none; }
    .camara { width: 100%; height: 100%; object-fit: cover; display: block; }
    .camara-marco {
      position: absolute; inset: 12%; border: 3px solid rgba(255,255,255,.85); border-radius: var(--radius-md);
      box-shadow: 0 0 0 999px rgba(15,23,42,.35);
      pointer-events: none;
    }
    .vacio { display: flex; flex-direction: column; align-items: center; gap: .85rem; text-align: center; padding: 1rem 0; width: 100%; }
    .vacio svg { width: 34px; height: 34px; color: var(--color-text-faint); }
    .vacio p { font-size: .88rem; color: var(--color-text-muted); }
    .aviso { display: flex; align-items: center; gap: .5rem; color: var(--color-text-muted); font-size: .88rem; }
    .resultado { padding: 2.5rem 1.75rem; display: flex; flex-direction: column; align-items: center; gap: .5rem; text-align: center; }
    .resultado__icono {
      width: 56px; height: 56px; border-radius: 50%; background: var(--color-success-bg); color: var(--color-success);
      display: flex; align-items: center; justify-content: center; margin-bottom: .5rem;
    }
    .resultado--tarde .resultado__icono { background: var(--color-warning-bg); color: var(--color-warning); }
    .resultado__icono--fallo { background: var(--color-danger-bg); color: var(--color-danger); }
    .resultado__icono svg { width: 30px; height: 30px; }
    .resultado__estado { font-size: 1.2rem; font-weight: 700; }
    .resultado__detalle { color: var(--color-text-muted); font-size: .88rem; margin-bottom: 1rem; }
  `]
})
export class MarcarAsistenciaComponent implements OnDestroy {
  @ViewChild('video') private videoRef!: ElementRef<HTMLVideoElement>;
  @ViewChild('canvas') private canvasRef!: ElementRef<HTMLCanvasElement>;

  private readonly servicio = inject(MarcarAsistenciaService);

  readonly camaraActiva = signal(false);
  readonly enviando = signal(false);
  readonly error = signal('');
  readonly resultado = signal<MarcarAsistenciaResponse | null>(null);
  readonly fallo = signal<string | null>(null);

  private stream: MediaStream | null = null;
  private cuadroAnimacion = 0;

  async iniciarCamara(): Promise<void> {
    this.error.set('');
    this.fallo.set(null);
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      this.videoRef.nativeElement.srcObject = this.stream;
      await this.videoRef.nativeElement.play();
      this.camaraActiva.set(true);
      this.escanearCuadro();
    } catch {
      this.error.set('No se pudo acceder a la cámara. Revisa los permisos del navegador.');
    }
  }

  private escanearCuadro = (): void => {
    if (!this.camaraActiva() || this.enviando()) {
      return;
    }

    const video = this.videoRef.nativeElement;
    const canvas = this.canvasRef.nativeElement;

    if (video.readyState === video.HAVE_ENOUGH_DATA) {
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      const contexto = canvas.getContext('2d');
      if (contexto) {
        contexto.drawImage(video, 0, 0, canvas.width, canvas.height);
        const datosImagen = contexto.getImageData(0, 0, canvas.width, canvas.height);
        const codigo = jsQR(datosImagen.data, datosImagen.width, datosImagen.height);
        if (codigo) {
          this.enviarToken(codigo.data);
          return;
        }
      }
    }

    this.cuadroAnimacion = requestAnimationFrame(this.escanearCuadro);
  };

  private enviarToken(token: string): void {
    this.enviando.set(true);
    this.detenerCamara();

    this.servicio.marcar(token).subscribe({
      next: (respuesta) => {
        this.enviando.set(false);
        this.resultado.set(respuesta);
      },
      error: (err) => {
        this.enviando.set(false);

        this.fallo.set(this.mensajeDeError(err.status));
      },
    });
  }

  reintentar(): void {
    this.fallo.set(null);
    this.iniciarCamara();
  }

  reiniciar(): void {
    this.resultado.set(null);
    this.iniciarCamara();
  }

  private detenerCamara(): void {
    cancelAnimationFrame(this.cuadroAnimacion);
    this.stream?.getTracks().forEach((pista) => pista.stop());
    this.stream = null;
    this.camaraActiva.set(false);
  }

  ngOnDestroy(): void {
    this.detenerCamara();
  }

  private mensajeDeError(status: number): string {
    switch (status) {
      case 410: return 'Ese código ya expiró o ya se usó. Pide uno nuevo en recepción y vuelve a intentar.';
      case 400: return 'Ya marcaste tu asistencia en esta sesión.';
      case 404: return 'Tu cuenta no está vinculada a un estudiante. Contacta a un administrador.';
      default: return 'No se pudo marcar tu asistencia. Intenta de nuevo.';
    }
  }
}
