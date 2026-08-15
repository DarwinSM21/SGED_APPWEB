import { descargarBlob } from './descargar-archivo';

describe('descargarBlob', () => {
  it('crea un object URL, dispara la descarga con el nombre dado y lo revoca', () => {
    const urlFalsa = 'blob:falso-123';
    const createObjectURLSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue(urlFalsa);
    const revokeObjectURLSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    const clickSpy = vi.fn();
    const crearElementoOriginal = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      const elemento = crearElementoOriginal(tagName);
      if (tagName === 'a') elemento.click = clickSpy;
      return elemento;
    });

    const blob = new Blob(['contenido'], { type: 'application/pdf' });
    descargarBlob(blob, 'reporte.pdf');

    expect(createObjectURLSpy).toHaveBeenCalledWith(blob);
    expect(clickSpy).toHaveBeenCalled();
    expect(revokeObjectURLSpy).toHaveBeenCalledWith(urlFalsa);

    createObjectURLSpy.mockRestore();
    revokeObjectURLSpy.mockRestore();
    (document.createElement as any).mockRestore();
  });

  it('asigna el nombre de archivo pedido al enlace de descarga', () => {
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:otro');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    let nombreAsignado = '';
    const crearElementoOriginal = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tagName: string) => {
      const elemento = crearElementoOriginal(tagName);
      if (tagName === 'a') {
        elemento.click = () => { nombreAsignado = (elemento as HTMLAnchorElement).download; };
      }
      return elemento;
    });

    descargarBlob(new Blob(['x']), 'mis-datos.pdf');

    expect(nombreAsignado).toBe('mis-datos.pdf');

    (URL.createObjectURL as any).mockRestore();
    (URL.revokeObjectURL as any).mockRestore();
    (document.createElement as any).mockRestore();
  });
});
