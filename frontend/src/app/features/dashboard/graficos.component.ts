import { Component, computed, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HistoricoIngresos } from './dashboard.models';

const MESES_CORTOS = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

const W = 480;
const H = 216;
const M = { arriba: 10, derecha: 10, abajo: 26, izquierda: 46 };
const ANCHO_BARRA = 24;

interface Barra {
  etiqueta: string; total: number; cantidad: number;
  x: number; y: number; alto: number; centro: number; destacada: boolean;
}

@Component({
  selector: 'app-graficos-ingresos',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="graficos">
      <section class="card grafico">
        <header class="grafico__cabecera">
          <div>
            <h2>Recaudación por mes</h2>
            <p class="grafico__sub">Últimos {{ datos().meses.length }} meses · por fecha de cobro</p>
          </div>
          <div class="grafico__resumen">
            <span class="resumen__valor">{{ datos().total | number: '1.2-2' }}</span>
            <span class="resumen__etiqueta">total · promedio {{ datos().promedioMensual | number: '1.0-0' }}/mes</span>
          </div>
        </header>

        <div class="lienzo">
          <svg [attr.viewBox]="'0 0 ' + W + ' ' + H" role="img"
               [attr.aria-label]="'Recaudación de los últimos ' + datos().meses.length + ' meses'">
            @for (t of ticks(); track t.valor) {
              <line class="rejilla" [attr.x1]="M.izquierda" [attr.x2]="W - M.derecha" [attr.y1]="t.y" [attr.y2]="t.y" />
              <text class="tick" [attr.x]="M.izquierda - 8" [attr.y]="t.y + 3.5" text-anchor="end">{{ t.etiqueta }}</text>
            }
            <line class="eje" [attr.x1]="M.izquierda" [attr.x2]="W - M.derecha"
                  [attr.y1]="H - M.abajo" [attr.y2]="H - M.abajo" />

            @for (b of barras(); track b.etiqueta; let i = $index) {
              <g class="barra" [class.barra--destacada]="b.destacada" [class.barra--activa]="activa() === i"
                 (pointerenter)="activa.set(i)" (pointerleave)="activa.set(null)">
                <!-- Franja invisible de ancho completo: el area sensible al
                     puntero es la banda del mes, no los 24px de la barra. -->
                <rect class="zona" [attr.x]="b.centro - 30" [attr.y]="M.arriba"
                      width="60" [attr.height]="H - M.abajo - M.arriba" />
                <rect class="pilar" [attr.x]="b.x" [attr.y]="b.y" [attr.width]="ANCHO_BARRA"
                      [attr.height]="b.alto" rx="4" />
                <!-- Tapa cuadrada sobre la linea de base: el redondeo va solo
                     en el extremo del dato, no en el arranque. -->
                @if (b.alto > 4) {
                  <rect class="pilar" [attr.x]="b.x" [attr.y]="H - M.abajo - 4" [attr.width]="ANCHO_BARRA" height="4" />
                }
                @if (b.destacada && b.alto > 0) {
                  <text class="valor" [attr.x]="b.centro" [attr.y]="b.y - 6" text-anchor="middle">
                    {{ b.total | number: '1.0-0' }}
                  </text>
                }
                <text class="mes" [attr.x]="b.centro" [attr.y]="H - M.abajo + 15" text-anchor="middle">{{ b.etiqueta }}</text>
              </g>
            }
          </svg>

          @if (activa() !== null && barras()[activa()!]; as b) {
            <div class="globo" [style.left.%]="(b.centro / W) * 100">
              <strong>{{ b.etiqueta }}</strong>
              <span>{{ b.total | number: '1.2-2' }}</span>
              <span class="globo__pagos">{{ b.cantidad }} {{ b.cantidad === 1 ? 'pago' : 'pagos' }}</span>
            </div>
          }
        </div>

        <details class="tabla-alterna">
          <summary>Ver los datos como tabla</summary>
          <table>
            <caption class="oculto">Recaudación por mes</caption>
            <thead><tr><th scope="col">Mes</th><th scope="col">Recaudado</th><th scope="col">Pagos</th></tr></thead>
            <tbody>
              @for (b of barras(); track b.etiqueta) {
                <tr><th scope="row">{{ b.etiqueta }}</th><td>{{ b.total | number: '1.2-2' }}</td><td>{{ b.cantidad }}</td></tr>
              }
            </tbody>
          </table>
        </details>
      </section>

      <!-- El estado vive en la seccion, no en el anillo: los puntos de la
           leyenda son hermanos del medidor y si el color de severidad se
           declarara mas abajo no les llegaria, quedando indigo mientras el
           anillo ya esta en rojo. -->
      <section class="card grafico grafico--medidor" [attr.data-estado]="estadoCobranza()">
        <header class="grafico__cabecera">
          <div>
            <h2>Cobranza del mes</h2>
            <p class="grafico__sub">Membresía de {{ nombreMesLargo() }}</p>
          </div>
        </header>

        <div class="medidor">
          <svg viewBox="0 0 160 160" role="img"
               [attr.aria-label]="'Cobranza del mes: ' + (porcentajeCobrado() | number: '1.0-0') + ' por ciento'">
            <!-- Pista: paso claro del mismo tono que el relleno, para que el
                 estado se lea a lo largo de todo el anillo y no solo en la
                 parte llena. -->
            <circle class="pista" cx="80" cy="80" r="62" fill="none" stroke-width="14" />
            <circle class="relleno" cx="80" cy="80" r="62" fill="none" stroke-width="14"
                    stroke-linecap="round" transform="rotate(-90 80 80)"
                    [attr.stroke-dasharray]="circunferencia"
                    [attr.stroke-dashoffset]="circunferencia * (1 - porcentajeCobrado() / 100)" />
          </svg>
          <div class="medidor__centro">
            <span class="medidor__cifra">{{ porcentajeCobrado() | number: '1.0-0' }}%</span>
            <span class="medidor__pie">al día</span>
          </div>
        </div>

        <div class="medidor__detalle">
          <div class="detalle-fila">
            <span class="punto punto--pagado"></span>
            <span class="detalle-texto">Al día</span>
            <strong>{{ alDia() }}</strong>
          </div>
          <div class="detalle-fila">
            <span class="punto punto--pendiente"></span>
            <span class="detalle-texto">Pendientes</span>
            <strong>{{ pendientes() }}</strong>
          </div>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .graficos {
      --barra: var(--color-primary-200);
      --barra-fuerte: var(--color-primary-600);
      --barra-activa: var(--color-primary-500);
      --pista: var(--color-primary-100);
      --relleno: var(--color-primary-600);
    }
    :host-context([data-theme="oscuro"]) .graficos {
      --barra: #3730a3;
      --barra-fuerte: #818cf8;
      --barra-activa: #a5b4fc;
      --pista: #312e81;
      --relleno: #818cf8;
    }
    .graficos { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(0, 1fr); gap: 1rem; margin-bottom: 1.5rem; }
    @media (max-width: 900px) { .graficos { grid-template-columns: 1fr; } }
    .grafico { padding: 1.15rem 1.3rem 1rem; }
    .grafico__cabecera { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: .5rem; }
    .grafico__cabecera h2 { font-size: 1rem; }
    .grafico__sub { margin: .15rem 0 0; font-size: .76rem; color: var(--color-text-muted); }
    .grafico__resumen { text-align: right; display: flex; flex-direction: column; }
    .resumen__valor { font-size: 1.15rem; font-weight: 700; }
    .resumen__etiqueta { font-size: .72rem; color: var(--color-text-faint); }
    .lienzo { position: relative; }
    .lienzo svg { display: block; width: 100%; height: auto; overflow: visible; }
    .rejilla { stroke: var(--color-border-light); stroke-width: 1; }
    .eje { stroke: var(--color-border); stroke-width: 1; }
    .tick, .mes { fill: var(--color-text-faint); font-size: 9px; font-variant-numeric: tabular-nums; }
    .valor { fill: var(--color-text); font-size: 10px; font-weight: 700; }
    .zona { fill: transparent; }
    .barra { cursor: default; }
    .pilar { fill: var(--barra); transition: fill var(--transition); }
    .barra--destacada .pilar { fill: var(--barra-fuerte); }
    .barra--activa .pilar { fill: var(--barra-activa); }
    .globo {
      position: absolute; top: 0; transform: translateX(-50%);
      background: var(--color-text); color: var(--color-surface);
      border-radius: var(--radius-sm); padding: .4rem .65rem;
      display: flex; flex-direction: column; gap: .05rem;
      font-size: .74rem; line-height: 1.35; pointer-events: none; white-space: nowrap;
      box-shadow: var(--shadow-md);
    }
    .globo strong { font-size: .78rem; }
    .globo__pagos { opacity: .75; }
    .tabla-alterna { margin-top: .7rem; }
    .tabla-alterna summary { font-size: .76rem; color: var(--color-text-muted); cursor: pointer; }
    .tabla-alterna table { width: 100%; border-collapse: collapse; margin-top: .5rem; font-size: .8rem; }
    .tabla-alterna th, .tabla-alterna td { text-align: left; padding: .35rem .5rem; border-bottom: 1px solid var(--color-border-light); }
    .tabla-alterna td { font-variant-numeric: tabular-nums; }
    .oculto { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
    .grafico--medidor { display: flex; flex-direction: column; }
    .medidor { position: relative; align-self: center; width: 160px; margin: .2rem 0 .6rem; }
    .medidor svg { display: block; width: 100%; height: auto; }
    .medidor__centro {
      position: absolute; inset: 0; display: flex; flex-direction: column;
      align-items: center; justify-content: center; gap: .05rem;
    }
    .medidor__cifra { font-size: 1.9rem; font-weight: 700; line-height: 1; }
    .medidor__pie { font-size: .72rem; color: var(--color-text-muted); }
    .medidor .pista { stroke: var(--pista); }
    .medidor .relleno { stroke: var(--relleno); transition: stroke-dashoffset .6s ease; }
    .grafico--medidor[data-estado="atencion"] { --relleno: var(--color-warning); --pista: var(--color-warning-bg); }
    .grafico--medidor[data-estado="critico"] { --relleno: var(--color-danger); --pista: var(--color-danger-bg); }
    .medidor__detalle { display: flex; flex-direction: column; gap: .4rem; margin-top: auto; }
    .detalle-fila { display: flex; align-items: center; gap: .5rem; font-size: .82rem; }
    .detalle-texto { flex: 1; color: var(--color-text-muted); }
    .detalle-fila strong { font-variant-numeric: tabular-nums; }
    .punto { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }
    .punto--pagado { background: var(--relleno); }
    .punto--pendiente { background: var(--pista); border: 1px solid var(--color-border); }

    @media (prefers-reduced-motion: reduce) {
      .medidor .relleno { transition: none; }
    }
  `],
})
export class GraficosIngresosComponent {
  readonly datos = input.required<HistoricoIngresos>();
  readonly estudiantesActivos = input.required<number>();
  readonly pendientes = input.required<number>();

  readonly W = W;
  readonly H = H;
  readonly M = M;
  readonly ANCHO_BARRA = ANCHO_BARRA;
  readonly circunferencia = 2 * Math.PI * 62;

  readonly activa = signal<number | null>(null);

  readonly alDia = computed(() => Math.max(0, this.estudiantesActivos() - this.pendientes()));

  readonly porcentajeCobrado = computed(() => {
    const total = this.estudiantesActivos();
    return total === 0 ? 0 : (this.alDia() / total) * 100;
  });

  readonly estadoCobranza = computed(() => {
    const p = this.porcentajeCobrado();
    if (p >= 90) return 'bien';
    return p >= 70 ? 'atencion' : 'critico';
  });

  private readonly techo = computed(() => {
    const max = Math.max(...this.datos().meses.map((m) => m.total), 0);
    if (max <= 0) return 100;
    const magnitud = Math.pow(10, Math.floor(Math.log10(max / 4)));
    for (const factor of [1, 2, 2.5, 5, 10]) {
      const paso = factor * magnitud;
      if (paso * 4 >= max) return paso * 4;
    }
    return magnitud * 40;
  });

  readonly ticks = computed(() => {
    const techo = this.techo();
    const alto = H - M.abajo - M.arriba;
    return [0, 0.25, 0.5, 0.75, 1].map((f) => {
      const valor = techo * f;
      return {
        valor,
        etiqueta: valor >= 1000 ? (valor / 1000) + 'k' : String(Math.round(valor)),
        y: H - M.abajo - alto * f,
      };
    });
  });

  readonly barras = computed<Barra[]>(() => {
    const meses = this.datos().meses;
    const techo = this.techo();
    const alto = H - M.abajo - M.arriba;
    const banda = (W - M.izquierda - M.derecha) / Math.max(1, meses.length);
    const mejor = this.datos().mejorMes;

    return meses.map((m, i) => {
      const centro = M.izquierda + banda * i + banda / 2;
      const altoBarra = techo === 0 ? 0 : (m.total / techo) * alto;
      return {
        etiqueta: MESES_CORTOS[m.mes - 1] ?? String(m.mes),
        total: m.total,
        cantidad: m.cantidadPagos,
        x: centro - ANCHO_BARRA / 2,
        y: H - M.abajo - altoBarra,
        alto: altoBarra,
        centro,
        destacada: !!mejor && mejor.anio === m.anio && mejor.mes === m.mes,
      };
    });
  });

  nombreMesLargo(): string {
    const meses = this.datos().meses;
    const ultimo = meses[meses.length - 1];
    const largos = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio',
      'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
    return ultimo ? `${largos[ultimo.mes - 1]} ${ultimo.anio}` : '';
  }
}
