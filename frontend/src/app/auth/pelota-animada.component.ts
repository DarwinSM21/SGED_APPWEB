import {
  AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, inject,
} from '@angular/core';

interface Chispa {
  x: number; y: number; vx: number; vy: number; vida: number; maxVida: number; tono: number;
}

interface Brillo {
  x: number; y: number; r: number; fase: number; velocidad: number;
}

/** Anillo que se expande desde el punto de impacto y se desvanece. */
interface Onda {
  x: number; y: number; r: number; vida: number; maxVida: number;
}

/**
 * Fondo animado del panel de marca: un balon con fisica real que rebota
 * dentro del panel, deja una estela luminosa y revienta en chispas en cada
 * impacto.
 *
 * Va sobre un <canvas> y no sobre elementos del DOM por tres razones:
 * dibujar ~200 particulas como divs obligaria al navegador a recalcular
 * estilos en cada cuadro; el canvas queda fuera del arbol de componentes,
 * asi que el bucle de animacion no despierta la deteccion de cambios de
 * Angular (el proyecto corre sin zone.js); y permite efectos -estela por
 * superposicion, degradados radiales- que en CSS costarian mucho mas.
 *
 * Es decorativo: va marcado aria-hidden y detras del contenido, de modo que
 * no agrega nada que un lector de pantalla tenga que anunciar.
 */
@Component({
  selector: 'app-pelota-animada',
  standalone: true,
  template: `<canvas #lienzo class="pelota-lienzo" aria-hidden="true"></canvas>`,
  styles: [`
    :host {
      position: absolute;
      inset: 0;
      overflow: hidden;
      pointer-events: none;
    }
    .pelota-lienzo { display: block; width: 100%; height: 100%; }
  `],
})
export class PelotaAnimadaComponent implements AfterViewInit, OnDestroy {

  @ViewChild('lienzo') private lienzoRef!: ElementRef<HTMLCanvasElement>;
  private readonly host = inject(ElementRef<HTMLElement>);

  private ctx!: CanvasRenderingContext2D;
  private animacion = 0;
  private observador?: ResizeObserver;

  /** Medidas en pixeles CSS; el canvas se escala aparte por devicePixelRatio. */
  private ancho = 0;
  private alto = 0;

  private readonly balon = { x: 0, y: 0, vx: 0, vy: 0, r: 32, giro: 0 };
  private estela: { x: number; y: number }[] = [];
  private chispas: Chispa[] = [];
  private brillos: Brillo[] = [];
  private ondas: Onda[] = [];

  private readonly GRAVEDAD = 0.42;
  private readonly REBOTE = 0.82;
  private readonly MAX_ESTELA = 22;
  private readonly MAX_CHISPAS = 190;

  /**
   * Con "reducir movimiento" activado se pinta un unico cuadro estatico en
   * vez de animar: la pantalla no queda vacia, pero nadie recibe una
   * animacion continua que no pidio.
   */
  private readonly prefiereQuieto =
    typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches;

  ngAfterViewInit(): void {
    const canvas = this.lienzoRef.nativeElement;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    this.ctx = ctx;

    this.medir();
    this.sembrar();

    if (typeof ResizeObserver === 'function') {
      this.observador = new ResizeObserver(() => this.medir());
      this.observador.observe(this.host.nativeElement);
    }

    if (this.prefiereQuieto) {
      this.dibujar();
      return;
    }

    document.addEventListener('visibilitychange', this.alCambiarVisibilidad);
    this.animar();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animacion);
    this.observador?.disconnect();
    document.removeEventListener('visibilitychange', this.alCambiarVisibilidad);
  }

  /** Con la pestaña oculta no tiene sentido gastar bateria dibujando. */
  private readonly alCambiarVisibilidad = (): void => {
    cancelAnimationFrame(this.animacion);
    if (!document.hidden) this.animar();
  };

  private medir(): void {
    const canvas = this.lienzoRef.nativeElement;
    const rect = this.host.nativeElement.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) return;

    // Escalar por devicePixelRatio evita que el balon se vea borroso en
    // pantallas retina; el ctx trabaja siempre en pixeles CSS.
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    canvas.width = Math.round(rect.width * dpr);
    canvas.height = Math.round(rect.height * dpr);
    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    const primeraVez = this.ancho === 0;
    this.ancho = rect.width;
    this.alto = rect.height;
    if (primeraVez) this.reposicionar();
  }

  private reposicionar(): void {
    this.balon.x = this.ancho * 0.5;
    this.balon.y = this.alto * 0.32;
    this.balon.vx = 3.1;
    this.balon.vy = 0;
  }

  private sembrar(): void {
    this.reposicionar();
    this.brillos = Array.from({ length: 26 }, () => ({
      x: Math.random() * this.ancho,
      y: Math.random() * this.alto,
      r: 0.7 + Math.random() * 1.9,
      fase: Math.random() * Math.PI * 2,
      velocidad: 0.006 + Math.random() * 0.014,
    }));
  }

  private animar = (): void => {
    this.actualizar();
    this.dibujar();
    this.animacion = requestAnimationFrame(this.animar);
  };

  private actualizar(): void {
    const b = this.balon;

    b.vy += this.GRAVEDAD;
    b.x += b.vx;
    b.y += b.vy;
    b.giro += b.vx * 0.022;

    if (b.x - b.r < 0) { b.x = b.r; b.vx = Math.abs(b.vx) * this.REBOTE; this.reventar(b.x, b.y); }
    if (b.x + b.r > this.ancho) { b.x = this.ancho - b.r; b.vx = -Math.abs(b.vx) * this.REBOTE; this.reventar(b.x, b.y); }
    if (b.y - b.r < 0) { b.y = b.r; b.vy = Math.abs(b.vy) * this.REBOTE; this.reventar(b.x, b.y); }
    if (b.y + b.r > this.alto) {
      b.y = this.alto - b.r;
      b.vy = -Math.abs(b.vy) * this.REBOTE;
      this.reventar(b.x, b.y);
      // Sin este empujon el balon pierde energia y termina quieto contra el
      // piso: la animacion se "muere" a los pocos segundos de cargar.
      if (Math.abs(b.vy) < 7.5) b.vy = -(7.5 + Math.random() * 3.4);
      b.vx += (Math.random() - 0.5) * 1.5;
      b.vx = Math.max(-6.5, Math.min(6.5, b.vx));
      if (Math.abs(b.vx) < 1.1) b.vx = b.vx < 0 ? -1.6 : 1.6;
    }

    this.estela.unshift({ x: b.x, y: b.y });
    if (this.estela.length > this.MAX_ESTELA) this.estela.pop();

    this.chispas = this.chispas.filter((c) => {
      c.vx *= 0.96;
      c.vy = c.vy * 0.96 + 0.16;
      c.x += c.vx;
      c.y += c.vy;
      c.vida -= 1;
      return c.vida > 0;
    });

    this.ondas = this.ondas.filter((o) => {
      o.r += 3.4;
      o.vida -= 1;
      return o.vida > 0;
    });

    for (const g of this.brillos) g.fase += g.velocidad;
  }

  private reventar(x: number, y: number): void {
    this.ondas.push({ x, y, r: this.balon.r * 0.7, vida: 26, maxVida: 26 });

    const cuantas = Math.min(28, this.MAX_CHISPAS - this.chispas.length);
    for (let i = 0; i < cuantas; i++) {
      const angulo = Math.random() * Math.PI * 2;
      const fuerza = 1.6 + Math.random() * 4.6;
      const vida = 26 + Math.random() * 24;
      this.chispas.push({
        x, y,
        vx: Math.cos(angulo) * fuerza,
        vy: Math.sin(angulo) * fuerza - 1.1,
        vida, maxVida: vida,
        // Del cian al violeta: los mismos tonos que los halos del panel.
        tono: 185 + Math.random() * 95,
      });
    }
  }

  private dibujar(): void {
    const ctx = this.ctx;
    ctx.clearRect(0, 0, this.ancho, this.alto);

    for (const g of this.brillos) {
      const alfa = 0.18 + Math.sin(g.fase) * 0.16;
      if (alfa <= 0) continue;
      ctx.beginPath();
      ctx.arc(g.x, g.y, g.r, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(255,255,255,${alfa})`;
      ctx.fill();
    }

    ctx.globalCompositeOperation = 'lighter';

    for (const o of this.ondas) {
      const t = o.vida / o.maxVida;
      ctx.beginPath();
      ctx.arc(o.x, o.y, o.r, 0, Math.PI * 2);
      ctx.strokeStyle = `rgba(190, 245, 255, ${0.45 * t})`;
      ctx.lineWidth = 1 + t * 2.4;
      ctx.stroke();
    }

    // La estela va del cian (cabeza, junto al balon) al violeta del panel
    // en la cola: da sensacion de velocidad sin ensuciar el fondo.
    this.estela.forEach((p, i) => {
      const t = 1 - i / this.MAX_ESTELA;
      const radio = this.balon.r * (0.3 + t * 0.78);
      const tono = 190 + (1 - t) * 80;
      const halo = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, radio);
      halo.addColorStop(0, `hsla(${tono}, 100%, 72%, ${0.3 * t})`);
      halo.addColorStop(1, `hsla(${tono}, 100%, 72%, 0)`);
      ctx.beginPath();
      ctx.arc(p.x, p.y, radio, 0, Math.PI * 2);
      ctx.fillStyle = halo;
      ctx.fill();
    });

    for (const c of this.chispas) {
      const t = c.vida / c.maxVida;
      ctx.beginPath();
      ctx.arc(c.x, c.y, 1.1 + t * 1.9, 0, Math.PI * 2);
      ctx.fillStyle = `hsla(${c.tono}, 100%, ${62 + t * 24}%, ${t})`;
      ctx.fill();
    }

    ctx.globalCompositeOperation = 'source-over';
    this.dibujarBalon();
  }

  /** Balon clasico: casquetes oscuros sobre blanco, girando con el avance. */
  private dibujarBalon(): void {
    const ctx = this.ctx;
    const { x, y, r, giro } = this.balon;

    ctx.save();
    ctx.translate(x, y);

    const resplandor = ctx.createRadialGradient(0, 0, r * 0.5, 0, 0, r * 2.5);
    resplandor.addColorStop(0, 'rgba(160, 240, 255, 0.42)');
    resplandor.addColorStop(1, 'rgba(160, 240, 255, 0)');
    ctx.beginPath();
    ctx.arc(0, 0, r * 2.5, 0, Math.PI * 2);
    ctx.fillStyle = resplandor;
    ctx.fill();

    ctx.rotate(giro);

    const cuerpo = ctx.createRadialGradient(-r * 0.35, -r * 0.4, r * 0.1, 0, 0, r);
    cuerpo.addColorStop(0, '#ffffff');
    cuerpo.addColorStop(1, '#c9d4e5');
    ctx.beginPath();
    ctx.arc(0, 0, r, 0, Math.PI * 2);
    ctx.fillStyle = cuerpo;
    ctx.fill();

    ctx.fillStyle = '#151a2b';
    this.pentagono(0, 0, r * 0.36, 0);
    for (let i = 0; i < 5; i++) {
      const a = (i / 5) * Math.PI * 2 - Math.PI / 2;
      this.pentagono(Math.cos(a) * r * 0.72, Math.sin(a) * r * 0.72, r * 0.24, a + Math.PI / 5);
    }

    ctx.restore();
  }

  private pentagono(cx: number, cy: number, radio: number, rotacion: number): void {
    const ctx = this.ctx;
    ctx.beginPath();
    for (let i = 0; i < 5; i++) {
      const a = rotacion + (i / 5) * Math.PI * 2 - Math.PI / 2;
      const px = cx + Math.cos(a) * radio;
      const py = cy + Math.sin(a) * radio;
      i === 0 ? ctx.moveTo(px, py) : ctx.lineTo(px, py);
    }
    ctx.closePath();
    ctx.fill();
  }
}
