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
