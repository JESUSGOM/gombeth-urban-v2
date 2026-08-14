import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Vecino } from '../models/vecino.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({
  providedIn: 'root'
})
export class VecinoService {

  private http = inject(HttpClient);
  private apiUrl = '/api/vecinos';

  getVecinos(
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<Vecino>> {

    const url =
      `${this.apiUrl}?page=${page}&size=${size}`;

    return this.http.get<PageResponse<Vecino>>(
      url
    );
  }

  getVecinosPorComunidad(
    comunidadId: number,
    page: number = 0,
    size: number = 10,
    estado: string = 'activos'
  ): Observable<PageResponse<Vecino>> {

    return this.http.get<PageResponse<Vecino>>(
      `${this.apiUrl}/comunidad/${comunidadId}`
      + `?page=${page}`
      + `&size=${size}`
      + `&estado=${estado}`
    );
  }

  getVecino(
    id: number
  ): Observable<Vecino> {

    return this.http.get<Vecino>(
      `${this.apiUrl}/${id}`
    );
  }

  actualizarVecino(
    id: number,
    vecino: Vecino
  ): Observable<Vecino> {

    return this.http.put<Vecino>(
      `${this.apiUrl}/${id}`,
      vecino
    );
  }

  crearVecino(
    vecino: Vecino
  ): Observable<Vecino> {

    return this.http.post<Vecino>(
      this.apiUrl,
      vecino
    );
  }


  descargarMandatoPdf(
    id: number
  ): Observable<Blob> {

    return this.http.get(
      `${this.apiUrl}/${id}/mandato-pdf`,
      {
        responseType: 'blob'
      }
    );
  }

  eliminarVecino(
    id: number
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }
}
