import {
  inject,
  Injectable
} from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

import {
  CuentaPresentador,
  CuentaPresentadorRequest
} from '../models/cuenta-presentador.model';

@Injectable({
  providedIn: 'root'
})
export class CuentaPresentadorService {

  private readonly http =
    inject(HttpClient);

  private readonly apiUrl =
    '/api/cuentas-presentador';

  listar(): Observable<CuentaPresentador[]> {
    return this.http.get<CuentaPresentador[]>(
      this.apiUrl
    );
  }

  listarActivas(): Observable<CuentaPresentador[]> {
    return this.http.get<CuentaPresentador[]>(
      `${this.apiUrl}/activas`
    );
  }

  obtener(
    id: number
  ): Observable<CuentaPresentador> {
    return this.http.get<CuentaPresentador>(
      `${this.apiUrl}/${id}`
    );
  }

  crear(
    request: CuentaPresentadorRequest
  ): Observable<CuentaPresentador> {
    return this.http.post<CuentaPresentador>(
      this.apiUrl,
      request
    );
  }

  actualizar(
    id: number,
    request: CuentaPresentadorRequest
  ): Observable<CuentaPresentador> {
    return this.http.put<CuentaPresentador>(
      `${this.apiUrl}/${id}`,
      request
    );
  }

  eliminar(
    id: number
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${id}`
    );
  }
}