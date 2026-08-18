/**
 * Formato de hora unico para toda la app: 12 horas con AM/PM en mayusculas.
 *
 * No se usa el pipe `date` de Angular para la parte horaria porque el locale
 * es es-EC y su patron `a` produce "p. m." -minusculas y con puntos-, que no
 * coincide con el "PM" que ya emite horaCorta() para los LocalTime que manda
 * el backend. Dos formatos distintos en la misma pantalla se notan, asi que
 * el criterio se centraliza aqui.
 */

function partes12(f: Date): { hora: number; periodo: string } {
  const h = f.getHours();
  return { hora: h % 12 === 0 ? 12 : h % 12, periodo: h < 12 ? 'AM' : 'PM' };
}

const dosDigitos = (n: number) => String(n).padStart(2, '0');

/** Reloj en vivo del topbar: "11:59:53 PM". */
export function relojEn12(f: Date): string {
  const { hora, periodo } = partes12(f);
  return `${hora}:${dosDigitos(f.getMinutes())}:${dosDigitos(f.getSeconds())} ${periodo}`;
}

/** Marca de tiempo de un registro: "18/08/2026 3:05 PM". */
export function fechaHoraCorta(valor: string | Date | null | undefined): string {
  if (!valor) return '';
  const f = valor instanceof Date ? valor : new Date(valor);
  if (Number.isNaN(f.getTime())) return '';
  const { hora, periodo } = partes12(f);
  return `${dosDigitos(f.getDate())}/${dosDigitos(f.getMonth() + 1)}/${f.getFullYear()}`
    + ` ${hora}:${dosDigitos(f.getMinutes())} ${periodo}`;
}
