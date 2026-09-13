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
    studentId: number;
    personName: string;
    personLastName: string;
    categoryName: string | null;
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
        studentId: e.studentId,
        fullName: `${e.personName} ${e.personLastName}`,
        category: e.categoryName,
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
