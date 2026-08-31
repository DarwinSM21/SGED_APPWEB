
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

export interface PaginaCategorias {
  content: Categoria[];
  totalElements: number;
}
