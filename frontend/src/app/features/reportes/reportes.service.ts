import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map } from 'rxjs';
import { CategoriaOpcionReporte, EstudianteOpcionReporte, FiltrosReporte } from './reportes.models';

interface EstudiantePagina {
  content: { studentId: number; personName: string; personLastName: string; categoryName: string }[];
}

@Injectable({ providedIn: 'root' })
export class ReportesService {
  private readonly http = inject(HttpClient);

  listarEstudiantes() {
    return this.http.get<EstudiantePagina>('/api/estudiantes?size=200').pipe(
      map((pagina) =>
        pagina.content.map(
          (e): EstudianteOpcionReporte => ({
            studentId: e.studentId,
            fullName: `${e.personName} ${e.personLastName}`,
            category: e.categoryName,
          }),
        ),
      ),
    );
  }

  categoriasActivas() {
    return this.http.get<CategoriaOpcionReporte[]>('/api/categorias/activas');
  }

  estudiantesFichas(filtros: FiltrosReporte) {
    let params = new HttpParams();
    if (filtros.category) params = params.set('categoria', filtros.category);
    if (filtros.active !== null && filtros.active !== undefined) params = params.set('activo', filtros.active);
    return this.http.get('/api/reportes/estudiantes-fichas', { params, responseType: 'blob' });
  }

  pagos(filtros: FiltrosReporte) {
    return this.http.get('/api/reportes/pagos', { params: this.paramsComunes(filtros), responseType: 'blob' });
  }

  asistencias(filtros: FiltrosReporte) {
    return this.http.get('/api/reportes/asistencias', { params: this.paramsConCategoria(filtros), responseType: 'blob' });
  }

  evaluaciones(filtros: FiltrosReporte) {
    return this.http.get('/api/reportes/evaluaciones', { params: this.paramsConCategoria(filtros), responseType: 'blob' });
  }

  lesiones(filtros: FiltrosReporte) {
    return this.http.get('/api/reportes/lesiones', { params: this.paramsConCategoria(filtros), responseType: 'blob' });
  }

  private paramsComunes(filtros: FiltrosReporte): HttpParams {
    let params = new HttpParams();
    if (filtros.estudianteId) params = params.set('estudianteId', filtros.estudianteId);
    if (filtros.fechaDesde) params = params.set('fechaDesde', filtros.fechaDesde);
    if (filtros.fechaHasta) params = params.set('fechaHasta', filtros.fechaHasta);
    return params;
  }

  private paramsConCategoria(filtros: FiltrosReporte): HttpParams {
    let params = this.paramsComunes(filtros);
    if (filtros.category) params = params.set('categoria', filtros.category);
    return params;
  }
}
