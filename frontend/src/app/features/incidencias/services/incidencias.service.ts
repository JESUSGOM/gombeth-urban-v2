import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ComunidadIncidencia {
  id: number;
  nombre: string;
}

export interface GestionIncidencia {
  id: number;
  costeEstimado: number | null;
  descripcion: string | null;
  observacionesInternas: string | null;
  estado: string;
  fechaRegistro: string | null;
  fechaActualizacion: string | null;
  fechaFinalizacion: string | null;
  fechaCierre: string | null;
  prioridad: string | null;
  titulo: string;
  comunidad?: ComunidadIncidencia | null;
}

export interface IncidenciaAdjunto {
  id: number;
  incidenciaId: number;
  nombreOriginal: string;
  contentType: string;
  tamanio: number;
  fechaSubida: string;
}

@Injectable({
  providedIn: 'root'
})
export class IncidenciasService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl =
      'http://localhost:8080/api/incidencias';

  listarPorComunidad(
      comunidadId: number
  ): Observable<GestionIncidencia[]> {

    return this.http.get<GestionIncidencia[]>(
        `${this.apiUrl}/comunidad/${comunidadId}`,
        {
          params: {
            nocache: Date.now().toString()
          }
        }
    );
  }

  obtener(
      id: number
  ): Observable<GestionIncidencia> {

    return this.http.get<GestionIncidencia>(
        `${this.apiUrl}/${id}`,
        {
          params: {
            nocache: Date.now().toString()
          }
        }
    );
  }

  guardar(
      incidencia: Partial<GestionIncidencia>
  ): Observable<GestionIncidencia> {

    return this.http.post<GestionIncidencia>(
        this.apiUrl,
        incidencia
    );
  }

  actualizar(
      id: number,
      incidencia: Partial<GestionIncidencia>
  ): Observable<GestionIncidencia> {

    return this.http.put<GestionIncidencia>(
        `${this.apiUrl}/${id}`,
        incidencia
    );
  }

  listarAdjuntos(
      incidenciaId: number
  ): Observable<IncidenciaAdjunto[]> {

    return this.http.get<IncidenciaAdjunto[]>(
        `${this.apiUrl}/${incidenciaId}/adjuntos`,
        {
          params: {
            nocache: Date.now().toString()
          }
        }
    );
  }

  obtenerContenidoAdjunto(
      incidenciaId: number,
      adjuntoId: number
  ): Observable<Blob> {

    return this.http.get(
        `${this.apiUrl}/${incidenciaId}/adjuntos/${adjuntoId}/contenido`,
        {
          params: {
            nocache: Date.now().toString()
          },
          responseType: 'blob'
        }
    );
  }
}
