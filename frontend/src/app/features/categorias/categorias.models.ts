/** Espejan CategoriaRequest/CategoriaResponse del backend. */

export interface Categoria {
  idCategoria: number;
  nombre: string;
  edadMin: number;
  edadMax: number;
  descripcion: string | null;
  activo: boolean;
  createdAt: string;
}

export interface CategoriaRequest {
  nombre: string;
  edadMin: number | null;
  edadMax: number | null;
  descripcion: string | null;
}

/** Forma de una página de Spring Data, en lo que interesa aquí. */
export interface PaginaCategorias {
  content: Categoria[];
  totalElements: number;
}
