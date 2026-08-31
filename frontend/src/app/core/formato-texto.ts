
export function inicialesDe(nombreCompleto: string): string {
  const partes = nombreCompleto.trim().split(/\s+/);
  const ultimo = partes[partes.length - 1] ?? '';
  return ultimo.slice(0, 2).toUpperCase();
}

export function apellidoDe(nombreCompleto: string): string {
  const partes = nombreCompleto.trim().split(/\s+/);
  return partes[partes.length - 1] ?? nombreCompleto;
}

export function horaCorta(hora: string | null): string | null {
  if (!hora) return hora;
  const [h, m] = hora.slice(0, 5).split(':').map(Number);
  const periodo = h < 12 ? 'AM' : 'PM';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${h12}:${String(m).padStart(2, '0')} ${periodo}`;
}
