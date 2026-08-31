import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { BuscadorOpcionesComponent, OpcionBuscable } from './buscador-opciones.component';

const ALUMNOS: OpcionBuscable[] = [
  { id: 1, titulo: 'Ángel Pincay', subtitulo: 'SUB-12' },
  { id: 2, titulo: 'María López', subtitulo: 'SUB-14' },
  { id: 3, titulo: 'Carlos Mora', subtitulo: 'SUB-12' },
  { id: 4, titulo: 'Adrián Suárez', subtitulo: 'SUB-14' },
];

describe('BuscadorOpcionesComponent', () => {
  let fixture: ComponentFixture<BuscadorOpcionesComponent>;
  let componente: BuscadorOpcionesComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [BuscadorOpcionesComponent] }).compileComponents();
    fixture = TestBed.createComponent(BuscadorOpcionesComponent);
    fixture.componentRef.setInput('opciones', ALUMNOS);
    fixture.detectChanges();
    componente = fixture.componentInstance;
  });

  function escribir(valor: string) {
    componente.alEscribir({ target: { value: valor } } as unknown as Event);
  }

  it('sin texto ofrece todas las opciones', () => {
    expect(componente.filtradas()).toHaveLength(4);
  });

  it('encuentra un nombre con tilde aunque se escriba sin ella', () => {
    escribir('angel');
    expect(componente.filtradas().map((o) => o.id)).toEqual([1]);

    escribir('suarez');
    expect(componente.filtradas().map((o) => o.id)).toEqual([4]);
  });

  it('tambien busca por el subtitulo, no solo por el nombre', () => {
    escribir('sub-14');
    expect(componente.filtradas().map((o) => o.id)).toEqual([2, 4]);
  });

  it('ignora mayusculas y espacios sobrantes', () => {
    escribir('  MORA  ');
    expect(componente.filtradas().map((o) => o.id)).toEqual([3]);
  });

  it('vuelve a la primera fila al cambiar la busqueda', () => {
    componente.abrir();
    componente.activa.set(3);
    escribir('sub-12');

    expect(componente.activa()).toBe(0);
  });

  it('las flechas dan la vuelta al llegar al extremo', () => {
    componente.abrir();
    const abajo = new KeyboardEvent('keydown', { key: 'ArrowDown' });
    for (let i = 0; i < 4; i++) componente.alTeclear(abajo);
    expect(componente.activa()).toBe(0);

    componente.alTeclear(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
    expect(componente.activa()).toBe(3);
  });

  it('Escape cierra sin elegir nada', () => {
    componente.abrir();
    let emitida: OpcionBuscable | null = null;
    componente.seleccionada.subscribe((o) => (emitida = o));

    componente.alTeclear(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(componente.abierto()).toBe(false);
    expect(emitida).toBeNull();
  });

  it('al elegir emite la opcion, cierra y limpia el texto', () => {
    componente.abrir();
    escribir('mora');
    let emitida: OpcionBuscable | null = null;
    componente.seleccionada.subscribe((o) => (emitida = o));

    componente.alTeclear(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(emitida).toEqual(ALUMNOS[2]);
    expect(componente.abierto()).toBe(false);

    expect(componente.texto()).toBe('');
  });

  it('Enter no elige nada si la busqueda no dio resultados', () => {
    componente.abrir();
    escribir('zzz');
    let emitida: OpcionBuscable | null = null;
    componente.seleccionada.subscribe((o) => (emitida = o));

    componente.alTeclear(new KeyboardEvent('keydown', { key: 'Enter' }));

    expect(emitida).toBeNull();
    expect(componente.filtradas()).toHaveLength(0);
  });

  it('muestra lo elegido cuando esta cerrado y la consulta mientras se busca', () => {
    fixture.componentRef.setInput('textoSeleccionado', 'Carlos Mora');
    fixture.detectChanges();
    const input = (): HTMLInputElement =>
      fixture.nativeElement.querySelector('input');

    expect(input().value).toBe('Carlos Mora');

    componente.abrir();
    fixture.detectChanges();
    expect(input().value).toBe('');

    componente.alTeclear(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(input().value).toBe('Carlos Mora');
  });

  it('limpiar avisa al padre para que vuelva a "nada"', () => {
    fixture.componentRef.setInput('textoSeleccionado', 'Carlos Mora');
    fixture.detectChanges();
    let limpiado = false;
    componente.limpiada.subscribe(() => (limpiado = true));

    componente.limpiar();

    expect(limpiado).toBe(true);
    expect(componente.texto()).toBe('');
  });
});
