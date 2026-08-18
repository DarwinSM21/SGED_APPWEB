import { mensajeDeError } from './mensaje-error';

describe('mensajeDeError', () => {
  it('prefiere el detalle por campo sobre el mensaje generico de validacion', () => {
    const respuesta = {
      error: {
        detail: 'Errores de validacion',
        errores: ['cedula: must match "\\d{10}"', 'correo: must be a well-formed email address'],
      },
    };

    expect(mensajeDeError(respuesta)).toBe(
      'cedula: must match "\\d{10}" · correo: must be a well-formed email address',
    );
  });

  it('usa detail cuando el backend no manda errores por campo', () => {
    const respuesta = { error: { detail: 'El código de estudiante ya se encuentra en uso.' } };

    expect(mensajeDeError(respuesta)).toBe('El código de estudiante ya se encuentra en uso.');
  });

  it('cae al mensaje por defecto si la respuesta no tiene nada aprovechable', () => {
    expect(mensajeDeError({ error: {} })).toBe('Error del servidor');
    expect(mensajeDeError(null)).toBe('Error del servidor');
    expect(mensajeDeError(undefined)).toBe('Error del servidor');
  });

  it('respeta el mensaje por defecto propio de cada pantalla', () => {
    expect(mensajeDeError(null, 'No se pudo crear la sesión.')).toBe('No se pudo crear la sesión.');
  });

  it('ignora un arreglo de errores vacio y sigue con detail', () => {
    const respuesta = { error: { detail: 'Conflicto', errores: [] } };

    expect(mensajeDeError(respuesta)).toBe('Conflicto');
  });
});
