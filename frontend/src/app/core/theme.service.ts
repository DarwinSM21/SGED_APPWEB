import { Injectable, signal } from '@angular/core';

export type Tema = 'claro' | 'oscuro';
export type Fuente = 'sans' | 'serif' | 'mono';
export type TamanoFuente = 'normal' | 'grande' | 'extra-grande';

const CLAVE_TEMA = 'sged.apariencia.tema';
const CLAVE_FUENTE = 'sged.apariencia.fuente';
const CLAVE_TAMANO_FUENTE = 'sged.apariencia.tamanoFuente';

/**
 * Preferencias de apariencia (tema, fuente, tamaño de fuente), aplicadas
 * como atributo/clases en <html> y persistidas en localStorage -- mismo
 * patrón que el colapso del sidebar en AppShellComponent, pero para
 * preferencias válidas en toda la app, no solo el shell.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly tema = signal<Tema>((localStorage.getItem(CLAVE_TEMA) as Tema) ?? 'claro');
  readonly fuente = signal<Fuente>((localStorage.getItem(CLAVE_FUENTE) as Fuente) ?? 'sans');
  readonly tamanoFuente = signal<TamanoFuente>(
    (localStorage.getItem(CLAVE_TAMANO_FUENTE) as TamanoFuente) ?? 'normal',
  );

  constructor() {
    this.aplicar();
  }

  establecerTema(tema: Tema): void {
    this.tema.set(tema);
    localStorage.setItem(CLAVE_TEMA, tema);
    this.aplicar();
  }

  alternarTema(): void {
    this.establecerTema(this.tema() === 'claro' ? 'oscuro' : 'claro');
  }

  establecerFuente(fuente: Fuente): void {
    this.fuente.set(fuente);
    localStorage.setItem(CLAVE_FUENTE, fuente);
    this.aplicar();
  }

  establecerTamanoFuente(tamano: TamanoFuente): void {
    this.tamanoFuente.set(tamano);
    localStorage.setItem(CLAVE_TAMANO_FUENTE, tamano);
    this.aplicar();
  }

  private aplicar(): void {
    const raiz = document.documentElement;
    raiz.setAttribute('data-theme', this.tema() === 'oscuro' ? 'oscuro' : 'claro');
    raiz.classList.remove('fuente-sans', 'fuente-serif', 'fuente-mono');
    raiz.classList.add(`fuente-${this.fuente()}`);
    raiz.classList.remove('tamano-normal', 'tamano-grande', 'tamano-extra-grande');
    raiz.classList.add(`tamano-${this.tamanoFuente()}`);
  }
}
