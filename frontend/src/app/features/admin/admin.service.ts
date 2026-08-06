import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { CrearRepresentanteRequest, EstudianteOpcion, HabilitarAccesoRequest } from './admin.models';

/** Forma minima de EstudiantePageResponse<EstudianteResponse> que interesa aqui. */
interface EstudiantePagina {
  content: {
    idEstudiante: number;
    nombrePersona: string;
    apellidoPersona: string;
    nombreCategoria: string;
  }[];
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);

  /** Tamaño de pagina grande a proposito: es para un multi-select, no para una lista paginada. */
  listarEstudiantes() {
    return this.http.get<EstudiantePagina>('/api/estudiantes?size=200').pipe(
      map((pagina) =>
        pagina.content.map(
          (e): EstudianteOpcion => ({
            idEstudiante: e.idEstudiante,
            nombreCompleto: `${e.nombrePersona} ${e.apellidoPersona}`,
            categoria: e.nombreCategoria,
          }),
        ),
      ),
    );
  }

  crearRepresentante(request: CrearRepresentanteRequest) {
    return this.http.post('/api/representantes', request);
  }

  /** A diferencia de register(), no crea una Persona nueva: usa la que el estudiante ya tiene. */
  habilitarAccesoEstudiante(idEstudiante: number, request: HabilitarAccesoRequest) {
    return this.http.post(`/api/estudiantes/${idEstudiante}/acceso`, request);
  }
}
