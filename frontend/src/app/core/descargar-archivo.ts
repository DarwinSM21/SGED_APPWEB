/**
 * Dispara la descarga de un blob (ej. PDF recibido de la API) en el
 * navegador. No hay ningun otro patron de descarga de archivos en el
 * frontend todavia -- este es el primero, usado por Reportes y por el
 * boton "mis datos" de Configuración.
 */
export function descargarBlob(blob: Blob, nombreArchivo: string): void {
  const url = URL.createObjectURL(blob);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  enlace.click();
  URL.revokeObjectURL(url);
}
