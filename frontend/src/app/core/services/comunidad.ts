import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Comunidad } from '../models/comunidad.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({
  providedIn: 'root'
})
export class ComunidadService {

  private http = inject(HttpClient);

  private apiUrl = 'http://localhost:8080/api/comunidades';

  getComunidades(
    page: number = 0,
    size: number = 10,
    usuarioId?: number
  ): Observable<PageResponse<Comunidad>> {

    let url = `${this.apiUrl}?page=${page}&size=${size}`;

    if (usuarioId) {
      url += `&usuarioId=${usuarioId}`;
    }

    return this.http.get<PageResponse<Comunidad>>(url);
  }

  getComunidad(id: number): Observable<Comunidad> {
    return this.http.get<Comunidad>(
      `${this.apiUrl}/${id}`
    );
  }

  actualizarComunidad(id: number, comunidad: Comunidad): Observable<Comunidad> {
    return this.http.put<Comunidad>(
      `${this.apiUrl}/${id}`,
      comunidad
    );
  }
}
