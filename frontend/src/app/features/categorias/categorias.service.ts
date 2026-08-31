import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Categoria, CategoriaRequest, PaginaCategorias } from './categorias.models';

@Injectable({ providedIn: 'root' })
export class CategoriasService {
  private readonly http = inject(HttpClient);

  listar() {
    return this.http.get<PaginaCategorias>('/api/categorias?size=200&sort=edadMin');
  }

  crear(request: CategoriaRequest) {
    return this.http.post<Categoria>('/api/categorias', request);
  }

  editar(id: number, request: CategoriaRequest) {
    return this.http.put<Categoria>(`/api/categorias/${id}`, request);
  }

  desactivar(id: number) {
    return this.http.delete<void>(`/api/categorias/${id}`);
  }

  reactivar(id: number) {
    return this.http.post<Categoria>(`/api/categorias/${id}/reactivar`, null);
  }
}
