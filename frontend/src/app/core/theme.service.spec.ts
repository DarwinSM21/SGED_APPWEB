import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.documentElement.className = '';
  });

  it('arranca en tema claro y aplica el atributo por defecto', () => {
    const servicio = new ThemeService();

    expect(servicio.tema()).toBe('claro');
    expect(document.documentElement.getAttribute('data-theme')).toBe('claro');
    expect(document.documentElement.classList.contains('fuente-sans')).toBe(true);
    expect(document.documentElement.classList.contains('tamano-normal')).toBe(true);
  });

  it('establecerTema cambia el signal, el atributo data-theme y persiste en localStorage', () => {
    const servicio = new ThemeService();

    servicio.establecerTema('oscuro');

    expect(servicio.tema()).toBe('oscuro');
    expect(document.documentElement.getAttribute('data-theme')).toBe('oscuro');
    expect(localStorage.getItem('sged.apariencia.tema')).toBe('oscuro');
  });

  it('alternarTema invierte el tema actual', () => {
    const servicio = new ThemeService();

    servicio.alternarTema();
    expect(servicio.tema()).toBe('oscuro');

    servicio.alternarTema();
    expect(servicio.tema()).toBe('claro');
  });

  it('establecerFuente cambia la clase de fuente en <html> y persiste', () => {
    const servicio = new ThemeService();

    servicio.establecerFuente('serif');

    expect(servicio.fuente()).toBe('serif');
    expect(document.documentElement.classList.contains('fuente-serif')).toBe(true);
    expect(document.documentElement.classList.contains('fuente-sans')).toBe(false);
    expect(localStorage.getItem('sged.apariencia.fuente')).toBe('serif');
  });

  it('establecerTamanoFuente cambia la clase de tamaño en <html> y persiste', () => {
    const servicio = new ThemeService();

    servicio.establecerTamanoFuente('extra-grande');

    expect(servicio.tamanoFuente()).toBe('extra-grande');
    expect(document.documentElement.classList.contains('tamano-extra-grande')).toBe(true);
    expect(document.documentElement.classList.contains('tamano-normal')).toBe(false);
    expect(localStorage.getItem('sged.apariencia.tamanoFuente')).toBe('extra-grande');
  });

  it('una nueva instancia lee la preferencia ya guardada en localStorage', () => {
    localStorage.setItem('sged.apariencia.tema', 'oscuro');
    localStorage.setItem('sged.apariencia.fuente', 'mono');
    localStorage.setItem('sged.apariencia.tamanoFuente', 'grande');

    const servicio = new ThemeService();

    expect(servicio.tema()).toBe('oscuro');
    expect(servicio.fuente()).toBe('mono');
    expect(servicio.tamanoFuente()).toBe('grande');
  });
});
