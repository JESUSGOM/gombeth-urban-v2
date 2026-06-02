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
    size: number = 10
  ): Observable<PageResponse<Comunidad>> {
    return this.http.get<PageResponse<Comunidad>>(
      `${this.apiUrl}?page=${page}&size=${size}`
    );
  }
}
