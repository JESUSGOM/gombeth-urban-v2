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
  private apiUrl = 'http://localhost:8080/api/vecinos';

  getVecinos(
    page: number = 0,
    size: number = 10,
    usuarioId?: number
  ): Observable<PageResponse<Vecino>> {

    let url = `${this.apiUrl}?page=${page}&size=${size}`;

    if (usuarioId) {
      url += `&usuarioId=${usuarioId}`;
    }

    return this.http.get<PageResponse<Vecino>>(url);
  }

  getVecinosPorComunidad(
    comunidadId: number,
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<Vecino>> {
    return this.http.get<PageResponse<Vecino>>(
      `${this.apiUrl}/comunidad/${comunidadId}?page=${page}&size=${size}`
    );
  }

  getVecino(id: number): Observable<Vecino> {
    return this.http.get<Vecino>(
      `${this.apiUrl}/${id}`
    );
  }

  actualizarVecino(id: number, vecino: Vecino): Observable<Vecino> {
    return this.http.put<Vecino>(
      `${this.apiUrl}/${id}`,
      vecino
    );
  }
}
