import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  CategoriaOpcion, EntrenadorRequest, EntrenadorResponse, EspecialidadOpcion, EstudianteRequest, EstudianteResponse,
  HabilitarAccesoRequest, PersonaRequest, PersonaResponse, PosicionOpcion, RepresentanteRequest,
  RepresentanteResponse, UsuarioRequest, UsuarioResponse, VinculoRequest,
} from './personas.models';

interface Pagina<T> {
  content: T[];
}

export const ESTADO_GENERAL_ACTIVO = 1;

@Injectable({ providedIn: 'root' })
export class PersonasService {
  private readonly http = inject(HttpClient);

  listarPersonas() {
    return this.http.get<Pagina<PersonaResponse>>('/api/personas?size=200&sort=apellido');
  }

  crearPersona(request: PersonaRequest) {
    return this.http.post<PersonaResponse>('/api/personas', request);
  }

  editarPersona(id: number, request: PersonaRequest) {
    return this.http.put<PersonaResponse>(`/api/personas/${id}`, request);
  }

  listarUsuarios() {
    return this.http.get<Pagina<UsuarioResponse>>('/api/usuarios?size=500');
  }

  listarEntrenadores() {
    return this.http.get<Pagina<EntrenadorResponse>>('/api/entrenadores?size=500');
  }

  listarRepresentantes() {
    return this.http.get<Pagina<RepresentanteResponse>>('/api/representantes?size=500');
  }

  crearUsuario(request: UsuarioRequest) {
    return this.http.post<UsuarioResponse>('/api/usuarios', request);
  }

  editarUsuario(id: number, request: UsuarioRequest) {
    return this.http.put<UsuarioResponse>(`/api/usuarios/${id}`, request);
  }

  desactivarUsuario(id: number) {
    return this.http.delete<void>(`/api/usuarios/${id}`);
  }

  activarUsuario(id: number) {
    return this.http.post<UsuarioResponse>(`/api/usuarios/${id}/activacion`, null);
  }

  categoriasActivas() {
    return this.http.get<CategoriaOpcion[]>('/api/categorias/activas');
  }

  posicionesActivas() {
    return this.http.get<PosicionOpcion[]>('/api/posiciones/activas');
  }

  siguienteCodigoEstudiante(anio: number) {
    return this.http.get('/api/estudiantes/operaciones/siguiente-codigo', {
      params: { anio }, responseType: 'text' as const,
    });
  }

  listarEstudiantes() {
    return this.http.get<Pagina<EstudianteResponse>>('/api/estudiantes?size=200');
  }

  crearEstudiante(request: EstudianteRequest) {
    return this.http.post<EstudianteResponse>('/api/estudiantes', request);
  }

  editarEstudiante(id: number, request: EstudianteRequest) {
    return this.http.put<EstudianteResponse>(`/api/estudiantes/${id}`, request);
  }

  habilitarAccesoEstudiante(idEstudiante: number, request: HabilitarAccesoRequest) {
    return this.http.post<EstudianteResponse>(`/api/estudiantes/${idEstudiante}/acceso`, request);
  }

  especialidadesActivas() {
    return this.http.get<EspecialidadOpcion[]>('/api/especialidades/activas');
  }

  crearEntrenador(request: EntrenadorRequest) {
    return this.http.post<EntrenadorResponse>('/api/entrenadores', request);
  }

  editarEntrenador(id: number, request: EntrenadorRequest) {
    return this.http.put<EntrenadorResponse>(`/api/entrenadores/${id}`, request);
  }

  crearRepresentante(request: RepresentanteRequest) {
    return this.http.post<RepresentanteResponse>('/api/representantes', request);
  }

  vincularEstudianteARepresentante(idRepresentante: number, idEstudiante: number, request: VinculoRequest) {
    return this.http.post<RepresentanteResponse>(
      `/api/representantes/${idRepresentante}/estudiantes/${idEstudiante}`, request);
  }

  desvincularEstudianteDeRepresentante(idRepresentante: number, idEstudiante: number) {
    return this.http.delete<void>(`/api/representantes/${idRepresentante}/estudiantes/${idEstudiante}`);
  }
}
