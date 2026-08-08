import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { EstudianteOpcionPago, PagoResponse, RegistrarDiarioRequest, RegistrarMembresiaRequest } from './pagos.models';

interface EstudiantePagina {
  content: { idEstudiante: number; nombrePersona: string; apellidoPersona: string; nombreCategoria: string }[];
}

@Injectable({ providedIn: 'root' })
export class PagosService {
  private readonly http = inject(HttpClient);

  listarEstudiantes() {
    return this.http.get<EstudiantePagina>('/api/estudiantes?size=200').pipe(
      map((pagina) =>
        pagina.content.map(
          (e): EstudianteOpcionPago => ({
            idEstudiante: e.idEstudiante,
            nombreCompleto: `${e.nombrePersona} ${e.apellidoPersona}`,
            categoria: e.nombreCategoria,
          }),
        ),
      ),
    );
  }

  registrarMembresia(request: RegistrarMembresiaRequest) {
    return this.http.post<PagoResponse[]>('/api/pagos/membresia', request);
  }

  registrarDiario(request: RegistrarDiarioRequest) {
    return this.http.post<PagoResponse>('/api/pagos/diario', request);
  }

  historialDe(idEstudiante: number) {
    return this.http.get<PagoResponse[]>(`/api/pagos/estudiante/${idEstudiante}`);
  }
}
