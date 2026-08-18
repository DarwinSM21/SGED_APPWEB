import { TestBed } from '@angular/core/testing';
import { ComponentRef } from '@angular/core';
import { GraficosIngresosComponent } from './graficos.component';
import { HistoricoIngresos, IngresoMes } from './dashboard.models';

function mes(anio: number, m: number, total: number, pagos = 1): IngresoMes {
  return { anio, mes: m, total, cantidadPagos: pagos };
}

function serie(meses: IngresoMes[]): HistoricoIngresos {
  const conMovimiento = meses.filter((m) => m.total > 0);
  return {
    meses,
    total: meses.reduce((a, m) => a + m.total, 0),
    promedioMensual: meses.reduce((a, m) => a + m.total, 0) / Math.max(1, meses.length),
    mejorMes: conMovimiento.length
      ? conMovimiento.reduce((a, b) => (b.total > a.total ? b : a))
      : null,
  };
}

/**
 * Se prueba la geometria y no el pintado: es la parte que puede equivocarse
 * en silencio -un grafico con las barras mal escaladas se ve perfectamente
 * bien y miente-, mientras que un color mal puesto salta a la vista.
 */
describe('GraficosIngresosComponent', () => {
  let ref: ComponentRef<GraficosIngresosComponent>;
  let componente: GraficosIngresosComponent;

  function montar(datos: HistoricoIngresos, activos = 25, pendientes = 3) {
    const fixture = TestBed.createComponent(GraficosIngresosComponent);
    ref = fixture.componentRef;
    ref.setInput('datos', datos);
    ref.setInput('estudiantesActivos', activos);
    ref.setInput('pendientes', pendientes);
    fixture.detectChanges();
    componente = fixture.componentInstance;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [GraficosIngresosComponent] }).compileComponents();
  });

  it('lleva las marcas del eje a cifras redondas en vez de repartir el maximo', () => {
    montar(serie([mes(2026, 3, 325), mes(2026, 8, 680)]));

    // Repartir 680 en cuartos daria 170/340/510: exacto pero ilegible.
    expect(componente.ticks().map((t) => t.etiqueta)).toEqual(['0', '200', '400', '600', '800']);
  });

  it('marca solo el mejor mes y deja el resto sin numero encima', () => {
    montar(serie([mes(2026, 6, 475), mes(2026, 7, 400), mes(2026, 8, 680)]));

    const destacadas = componente.barras().filter((b) => b.destacada);
    expect(destacadas).toHaveLength(1);
    expect(destacadas[0].etiqueta).toBe('ago');
  });

  it('dibuja en cero los meses sin cobros sin sacarlos del eje', () => {
    montar(serie([mes(2026, 5, 0, 0), mes(2026, 6, 0, 0), mes(2026, 7, 400)]));

    const barras = componente.barras();
    expect(barras.map((b) => b.etiqueta)).toEqual(['may', 'jun', 'jul']);
    expect(barras[0].alto).toBe(0);
    // El mes vacio conserva su lugar: si se omitiera, mayo y julio quedarian
    // contiguos y la pendiente que se lee seria falsa.
    expect(barras[1].centro).toBeGreaterThan(barras[0].centro);
    expect(barras[2].centro).toBeGreaterThan(barras[1].centro);
  });

  it('escala las alturas en proporcion al monto', () => {
    montar(serie([mes(2026, 7, 200), mes(2026, 8, 400)]));

    const [julio, agosto] = componente.barras();
    expect(agosto.alto).toBeCloseTo(julio.alto * 2, 5);
  });

  it('no destaca ningun mes cuando no hubo un solo cobro', () => {
    montar(serie([mes(2026, 7, 0, 0), mes(2026, 8, 0, 0)]));

    expect(componente.barras().every((b) => !b.destacada)).toBe(true);
    expect(componente.barras().every((b) => b.alto === 0)).toBe(true);
  });

  it('calcula la cobranza como los que estan al dia sobre el total', () => {
    montar(serie([mes(2026, 8, 680)]), 25, 3);

    expect(componente.alDia()).toBe(22);
    expect(componente.porcentajeCobrado()).toBe(88);
  });

  it('sube la severidad del medidor conforme cae la cobranza', () => {
    const datos = serie([mes(2026, 8, 680)]);

    montar(datos, 100, 5);
    expect(componente.estadoCobranza()).toBe('bien');

    ref.setInput('pendientes', 20);
    expect(componente.estadoCobranza()).toBe('atencion');

    ref.setInput('pendientes', 45);
    expect(componente.estadoCobranza()).toBe('critico');
  });

  it('no divide por cero cuando no hay estudiantes activos', () => {
    montar(serie([mes(2026, 8, 0, 0)]), 0, 0);

    expect(componente.porcentajeCobrado()).toBe(0);
    expect(componente.alDia()).toBe(0);
  });
});
