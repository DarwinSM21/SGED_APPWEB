import { Component, computed, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DiaAsistencia, MapaAsistencia } from './dashboard.models';

const DIAS_ISO = ['lun', 'mar', 'mié', 'jue', 'vie', 'sáb', 'dom'];
const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
const MS_DIA = 86400000;

function aFecha(iso: string): Date {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return new Date(anio, mes - 1, dia);
}

function aIso(f: Date): string {
  const mes = String(f.getMonth() + 1).padStart(2, '0');
  const dia = String(f.getDate()).padStart(2, '0');
  return f.getFullYear() + '-' + mes + '-' + dia;
}

function diaIso(f: Date): number {
  return ((f.getDay() + 6) % 7) + 1;
}

function nivelDe(porcentaje: number): number {
  if (porcentaje >= 90) return 5;
  if (porcentaje >= 80) return 4;
  if (porcentaje >= 70) return 3;
  if (porcentaje >= 60) return 2;
  return 1;
}

interface Celda { iso: string; dia: DiaAsistencia | null; nivel: number; }
interface Fila { etiqueta: string; celdas: Celda[]; }
interface Globo { texto: string; detalle: string; x: number; y: number; debajo: boolean; }

@Component({
  selector: 'app-mapa-asistencia',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="card mapa">
      <header class="mapa__cabecera">
        <div>
          <h2>Pulso de asistencia</h2>
          <p class="mapa__sub">
            {{ datos().dias.length }} días de entrenamiento · promedio {{ datos().promedio | number: '1.0-0' }}%
          </p>
        </div>
        <div class="mapa__extremos">
          @if (datos().mejorDia; as mejor) {
            <span class="extremo">
              <span class="extremo__etiqueta">mejor día</span>
              <strong>{{ fechaCorta(mejor.fecha) }}</strong>
              <span class="extremo__cifra">{{ mejor.porcentaje | number: '1.0-0' }}%</span>
            </span>
          }
          @if (datos().peorDia; as peor) {
            <span class="extremo">
              <span class="extremo__etiqueta">más flojo</span>
              <strong>{{ fechaCorta(peor.fecha) }}</strong>
              <span class="extremo__cifra">{{ peor.porcentaje | number: '1.0-0' }}%</span>
            </span>
          }
        </div>
      </header>

      <div class="rejilla-marco">
        <div class="cuerpo" (pointerleave)="globo.set(null)">
          <div class="meses">
            @for (m of meses(); track m.columna) {
              <span class="mes-etiqueta" [style.grid-column]="m.columna + 1">{{ m.texto }}</span>
            }
          </div>

          <div class="dias-semana">
            @for (f of filas(); track f.etiqueta) { <span>{{ f.etiqueta }}</span> }
          </div>

          <div class="rejilla" [style.grid-template-columns]="'repeat(' + semanas() + ', 17px)'">
            @for (f of filas(); track f.etiqueta) {
              @for (c of f.celdas; track c.iso) {
                <div class="celda"
                     [class.celda--vacia]="!c.dia"
                     [attr.data-nivel]="c.dia ? c.nivel : null"
                     [attr.tabindex]="c.dia ? 0 : null"
                     [attr.aria-label]="c.dia ? etiquetaAccesible(c.dia) : null"
                     (pointerenter)="mostrar($event, c)"
                     (focus)="mostrar($event, c)"></div>
              }
            }
          </div>

          @if (globo(); as g) {
            <div class="globo" [class.globo--debajo]="g.debajo"
                 [style.left.px]="g.x" [style.top.px]="g.y">
              <strong>{{ g.texto }}</strong>
              <span>{{ g.detalle }}</span>
            </div>
          }
        </div>
      </div>

      <footer class="mapa__pie">
        <!-- La escala es fija, no relativa a los datos del rango: si un mes
             la asistencia es buena, el mapa tiene que verse oscuro, no
             reescalarse para volver a llenar los cinco tonos. Por eso la
             leyenda lleva los porcentajes y no un "menos/más" sin referencia. -->
        <div class="leyenda">
          <span class="leyenda__texto">&lt;60%</span>
          @for (n of niveles; track n) {
            <span class="muestra" [attr.data-nivel]="n" [title]="rangoDe(n)"></span>
          }
          <span class="leyenda__texto">90%+</span>
        </div>
        <div class="leyenda">
          <span class="muestra muestra--vacia"></span>
          <span class="leyenda__texto">sin entrenamiento</span>
        </div>
      </footer>

      <details class="tabla-alterna">
        <summary>Ver los datos como tabla</summary>
        <div class="tabla-desplazable">
          <table>
            <caption class="oculto">Asistencia por día de entrenamiento</caption>
            <thead>
              <tr>
                <th scope="col">Día</th><th scope="col">Presentes</th>
                <th scope="col">Esperados</th><th scope="col">%</th>
              </tr>
            </thead>
            <tbody>
              @for (d of datos().dias; track d.fecha) {
                <tr>
                  <th scope="row">{{ fechaCorta(d.fecha) }}</th>
                  <td>{{ d.presentes }}</td>
                  <td>{{ d.esperados }}</td>
                  <td>{{ d.porcentaje | number: '1.0-0' }}%</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </details>
    </section>
  `,
  styles: [`
    :host { display: block; margin-bottom: 1.5rem; }
    .mapa { padding: 1.15rem 1.3rem 1rem; }
    .mapa__cabecera { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
    .mapa__cabecera h2 { font-size: 1rem; }
    .mapa__sub { margin: .15rem 0 0; font-size: .76rem; color: var(--color-text-muted); }
    .mapa__extremos { display: flex; gap: 1.2rem; }
    .extremo { display: flex; flex-direction: column; align-items: flex-end; line-height: 1.3; }
    .extremo__etiqueta { font-size: .66rem; text-transform: uppercase; letter-spacing: .05em; color: var(--color-text-faint); }
    .extremo strong { font-size: .82rem; }
    .extremo__cifra { font-size: .78rem; color: var(--color-text-muted); font-variant-numeric: tabular-nums; }
    .rejilla-marco { margin-top: .9rem; overflow-x: auto; padding: 4px 0 6px; }
    .cuerpo {
      position: relative; width: max-content;
      display: grid; grid-template-columns: 26px auto; gap: 4px 6px;
      grid-template-areas: ". meses" "dias rejilla";
    }
    .meses {
      grid-area: meses; display: grid; grid-auto-flow: column;
      grid-auto-columns: 17px; gap: 4px; height: 12px;
    }
    .mes-etiqueta { font-size: .66rem; color: var(--color-text-faint); white-space: nowrap; }
    .dias-semana { grid-area: dias; display: grid; grid-auto-rows: 17px; gap: 4px; }
    .dias-semana span {
      font-size: .62rem; color: var(--color-text-faint);
      display: flex; align-items: center; justify-content: flex-end;
    }
    .rejilla { grid-area: rejilla; display: grid; grid-auto-rows: 17px; gap: 4px; }
    .celda {
      border-radius: 3px; background: var(--n1); cursor: default;
      transition: transform var(--transition);
    }
    .celda[data-nivel="2"] { background: var(--n2); }
    .celda[data-nivel="3"] { background: var(--n3); }
    .celda[data-nivel="4"] { background: var(--n4); }
    .celda[data-nivel="5"] { background: var(--n5); }
    .celda--vacia { background: var(--color-border-light); }
    .celda:not(.celda--vacia):hover, .celda:focus-visible { transform: scale(1.35); }
    .celda:focus-visible { outline: 2px solid var(--color-primary-500); outline-offset: 1px; }
    .globo {
      position: absolute; transform: translate(-50%, -100%);
      background: var(--color-text); color: var(--color-surface);
      border-radius: var(--radius-sm); padding: .4rem .6rem; margin-top: -7px;
      display: flex; flex-direction: column; font-size: .74rem; line-height: 1.35;
      pointer-events: none; white-space: nowrap; box-shadow: var(--shadow-md); z-index: 2;
    }
    .globo--debajo { transform: translate(-50%, 0); margin-top: 7px; }
    .globo span { opacity: .78; }
    .mapa__pie { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-top: .8rem; flex-wrap: wrap; }
    .leyenda { display: flex; align-items: center; gap: 5px; }
    .leyenda__texto { font-size: .7rem; color: var(--color-text-faint); }
    .muestra { width: 14px; height: 14px; border-radius: 3px; background: var(--n1); }
    .muestra[data-nivel="2"] { background: var(--n2); }
    .muestra[data-nivel="3"] { background: var(--n3); }
    .muestra[data-nivel="4"] { background: var(--n4); }
    .muestra[data-nivel="5"] { background: var(--n5); }
    .muestra--vacia { background: var(--color-border-light); }
    .tabla-alterna { margin-top: .5rem; }
    .tabla-alterna summary { font-size: .76rem; color: var(--color-text-muted); cursor: pointer; }
    .tabla-desplazable { overflow-x: auto; }
    .tabla-alterna table { width: 100%; border-collapse: collapse; margin-top: .5rem; font-size: .8rem; }
    .tabla-alterna th, .tabla-alterna td { text-align: left; padding: .3rem .5rem; border-bottom: 1px solid var(--color-border-light); }
    .tabla-alterna td { font-variant-numeric: tabular-nums; }
    .oculto { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; }
    .mapa {
      --n1: var(--color-primary-100);
      --n2: var(--color-primary-200);
      --n3: var(--color-primary-400);
      --n4: var(--color-primary-600);
      --n5: var(--color-primary-700);
    }
    :host-context([data-theme="oscuro"]) .mapa {
      --n1: #312e81;
      --n2: #3730a3;
      --n3: #4f46e5;
      --n4: #818cf8;
      --n5: #c7d2fe;
    }

    @media (prefers-reduced-motion: reduce) {
      .celda { transition: none; }
      .celda:not(.celda--vacia):hover, .celda:focus-visible { transform: none; }
    }
  `],
})
export class MapaAsistenciaComponent {
  readonly datos = input.required<MapaAsistencia>();

  readonly niveles = [1, 2, 3, 4, 5];
  readonly globo = signal<Globo | null>(null);

  private readonly porFecha = computed(() => {
    const mapa = new Map<string, DiaAsistencia>();
    for (const d of this.datos().dias) mapa.set(d.fecha, d);
    return mapa;
  });

  private readonly primerLunes = computed(() => {
    const inicio = aFecha(this.datos().desde);
    inicio.setDate(inicio.getDate() - (diaIso(inicio) - 1));
    return inicio;
  });

  readonly semanas = computed(() => {
    const fin = aFecha(this.datos().hasta);
    const dias = Math.floor((fin.getTime() - this.primerLunes().getTime()) / MS_DIA);
    return Math.max(1, Math.floor(dias / 7) + 1);
  });

  private readonly diasConSesion = computed(() => {
    const presentes = new Set<number>();
    for (const d of this.datos().dias) presentes.add(diaIso(aFecha(d.fecha)));
    return presentes.size === 0 ? [1, 2, 3, 4, 5] : [...presentes].sort((a, b) => a - b);
  });

  readonly filas = computed<Fila[]>(() => {
    const lunes = this.primerLunes();
    const semanas = this.semanas();
    const porFecha = this.porFecha();

    return this.diasConSesion().map((iso) => {
      const celdas: Celda[] = [];
      for (let semana = 0; semana < semanas; semana++) {
        const f = new Date(lunes);
        f.setDate(lunes.getDate() + semana * 7 + (iso - 1));
        const clave = aIso(f);
        const dia = porFecha.get(clave) ?? null;
        celdas.push({ iso: clave, dia, nivel: dia ? nivelDe(dia.porcentaje) : 0 });
      }
      return { etiqueta: DIAS_ISO[iso - 1], celdas };
    });
  });

  readonly meses = computed(() => {
    const lunes = this.primerLunes();
    const etiquetas: { texto: string; columna: number }[] = [];
    let ultimoMes = -1;
    for (let semana = 0; semana < this.semanas(); semana++) {
      const f = new Date(lunes);
      f.setDate(lunes.getDate() + semana * 7);
      if (f.getMonth() !== ultimoMes) {
        ultimoMes = f.getMonth();
        etiquetas.push({ texto: MESES[ultimoMes], columna: semana });
      }
    }
    return etiquetas;
  });

  mostrar(evento: Event, celda: Celda): void {
    if (!celda.dia) { this.globo.set(null); return; }
    const elemento = evento.target as HTMLElement;
    const primeraFila = elemento.offsetTop <= elemento.offsetHeight;
    this.globo.set({
      texto: this.fechaCorta(celda.dia.fecha),
      detalle: celda.dia.presentes + ' de ' + celda.dia.esperados
        + ' · ' + Math.round(celda.dia.porcentaje) + '%',
      x: elemento.offsetLeft + elemento.offsetWidth / 2,
      y: primeraFila ? elemento.offsetTop + elemento.offsetHeight : elemento.offsetTop,
      debajo: primeraFila,
    });
  }

  fechaCorta(iso: string): string {
    const f = aFecha(iso);
    return DIAS_ISO[diaIso(f) - 1] + ' ' + f.getDate() + ' ' + MESES[f.getMonth()];
  }

  rangoDe(nivel: number): string {
    return ['menos de 60%', '60% a 69%', '70% a 79%', '80% a 89%', '90% o más'][nivel - 1];
  }

  etiquetaAccesible(dia: DiaAsistencia): string {
    return this.fechaCorta(dia.fecha) + ': ' + dia.presentes + ' de ' + dia.esperados
      + ' presentes, ' + Math.round(dia.porcentaje) + ' por ciento';
  }
}
