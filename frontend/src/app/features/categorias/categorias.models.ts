
export interface Categoria {
  categoryId: number;
  name: string;
  minAge: number;
  maxAge: number;
  description: string | null;
  active: boolean;
  createdAt: string;
}

export interface CategoriaRequest {
  name: string;
  minAge: number | null;
  maxAge: number | null;
  description: string | null;
}

export interface PaginaCategorias {
  content: Categoria[];
  totalElements: number;
}
