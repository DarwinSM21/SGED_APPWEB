import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  CategoriaOpcion, EstudianteAltaRequest, EstudiantePagina,
  EstudianteResponse, PersonaAltaRequest, PersonaCreada,
} from './estudiantes.models';

/** ID de seguridad.estados_general para "Activo" (ver db/seed.sql). */
export const ESTADO_GENERAL_ACTIVO = 1;

@Injectable({ providedIn: 'root' })
export class EstudiantesService {
  private readonly http = inject(HttpClient);

  categoriasActivas() {
    return this.http.get<CategoriaOpcion[]>('/api/categorias/activas');
  }

  siguienteCodigo(anio: number) {
    return this.http.get('/api/estudiantes/operaciones/siguiente-codigo', {
      params: { anio }, responseType: 'text' as const,
    });
  }

  crearPersona(request: PersonaAltaRequest) {
    return this.http.post<PersonaCreada>('/api/personas', request);
  }

  crearEstudiante(request: EstudianteAltaRequest) {
    return this.http.post<EstudianteResponse>('/api/estudiantes', request);
  }

  editarEstudiante(id: number, request: EstudianteAltaRequest) {
    return this.http.put<EstudianteResponse>(`/api/estudiantes/${id}`, request);
  }

  listar() {
    return this.http.get<EstudiantePagina>('/api/estudiantes?size=200');
  }
}
