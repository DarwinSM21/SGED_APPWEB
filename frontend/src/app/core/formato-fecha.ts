
function partes12(f: Date): { hora: number; periodo: string } {
  const h = f.getHours();
  return { hora: h % 12 === 0 ? 12 : h % 12, periodo: h < 12 ? 'AM' : 'PM' };
}

const dosDigitos = (n: number) => String(n).padStart(2, '0');

export function relojEn12(f: Date): string {
  const { hora, periodo } = partes12(f);
  return `${hora}:${dosDigitos(f.getMinutes())}:${dosDigitos(f.getSeconds())} ${periodo}`;
}

export function fechaHoraCorta(valor: string | Date | null | undefined): string {
  if (!valor) return '';
  const f = valor instanceof Date ? valor : new Date(valor);
  if (Number.isNaN(f.getTime())) return '';
  const { hora, periodo } = partes12(f);
  return `${dosDigitos(f.getDate())}/${dosDigitos(f.getMonth() + 1)}/${f.getFullYear()}`
    + ` ${hora}:${dosDigitos(f.getMinutes())} ${periodo}`;
}
