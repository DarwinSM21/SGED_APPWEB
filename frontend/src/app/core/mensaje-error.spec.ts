import { HttpErrorResponse } from '@angular/common/http';
import { mensajeDeError } from './mensaje-error';

describe('mensajeDeError', () => {
  it('prefiere el detalle por campo sobre el mensaje generico de validacion', () => {
    const respuesta = new HttpErrorResponse({
      status: 400,
      error: {
        detail: 'Errores de validacion',
        errores: ['cedula: must match "\\d{10}"', 'correo: must be a well-formed email address'],
      },
    });

    expect(mensajeDeError(respuesta)).toBe(
      'cedula: must match "\\d{10}" · correo: must be a well-formed email address',
    );
  });

  it('usa detail cuando el backend no manda errores por campo', () => {
    const respuesta = new HttpErrorResponse({
      status: 400, error: { detail: 'El código de estudiante ya se encuentra en uso.' },
    });

    expect(mensajeDeError(respuesta)).toBe('El código de estudiante ya se encuentra en uso.');
  });

  it('cae al mensaje por defecto si la respuesta no tiene nada aprovechable', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 400, error: {} })))
      .toBe('Revisa los datos del formulario');
    expect(mensajeDeError(null)).toBe('Ocurrió un error inesperado. Vuelve a intentarlo; si persiste, avisa a quien administra el sistema.');
    expect(mensajeDeError(undefined)).toBe('Ocurrió un error inesperado. Vuelve a intentarlo; si persiste, avisa a quien administra el sistema.');
  });

  it('respeta el mensaje por defecto propio de cada pantalla', () => {
    expect(mensajeDeError(new HttpErrorResponse({ status: 400 }), 'No se pudo crear la sesión.'))
      .toBe('No se pudo crear la sesión.');
  });

  it('ignora un arreglo de errores vacio y sigue con detail', () => {
    const respuesta = new HttpErrorResponse({ status: 409, error: { detail: 'Conflicto', errores: [] } });

    expect(mensajeDeError(respuesta)).toBe('Conflicto');
  });
});
