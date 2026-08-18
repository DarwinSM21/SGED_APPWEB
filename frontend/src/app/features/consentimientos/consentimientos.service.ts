import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import {
  Consentimiento,
  EstudianteOpcion,
  OtorgarConsentimientoRequest,
  RepresentanteConVinculos,
} from './consentimientos.models';

interface PaginaEstudiantes {
  content: {
    idEstudiante: number;
    nombrePersona: string;
    apellidoPersona: string;
    nombreCategoria: string | null;
  }[];
}

interface PaginaRepresentantes {
  content: RepresentanteConVinculos[];
}

@Injectable({ providedIn: 'root' })
export class ConsentimientosService {

  private readonly http = inject(HttpClient);

  estudiantes() {
    return this.http.get<PaginaEstudiantes>('/api/estudiantes?size=200').pipe(
      map((p): EstudianteOpcion[] => p.content.map((e) => ({
        idEstudiante: e.idEstudiante,
        nombreCompleto: `${e.nombrePersona} ${e.apellidoPersona}`,
        categoria: e.nombreCategoria,
      }))),
    );
  }

  /**
   * Se traen todos los representantes con sus representados y el vínculo se
   * cruza en el cliente. Es el mismo criterio que ya usa la ficha del
   * estudiante: no existe un endpoint que devuelva "los representantes de
   * este estudiante", y agregarlo por una pantalla más sería duplicar un
   * dato que ya viaja completo.
   */
  representantes() {
    return this.http.get<PaginaRepresentantes>('/api/representantes?size=500').pipe(
      map((p) => p.content),
    );
  }

  porEstudiante(idEstudiante: number) {
    return this.http.get<Consentimiento[]>(`/api/consentimientos/estudiante/${idEstudiante}`);
  }

  otorgar(request: OtorgarConsentimientoRequest) {
    return this.http.post<Consentimiento>('/api/consentimientos', request);
  }

  revocar(idConsentimiento: number) {
    return this.http.post<Consentimiento>(`/api/consentimientos/${idConsentimiento}/revocar`, null);
  }
}
