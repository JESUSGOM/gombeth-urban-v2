import {
  inject,
  Injectable
} from '@angular/core';

import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

import {
  Gasto,
  GastoGuardarRequest
} from '../models/gasto.model';

@Injectable({
  providedIn: 'root'
})
export class GastoService {

  private readonly http =
    inject(HttpClient);

  private readonly api =
    '/api/gastos';

  listarPorComunidad(
    comunidadId: number
  ): Observable<Gasto[]> {

    const params =
      new HttpParams()
        .set(
          'comunidadId',
          comunidadId.toString()
        );

    return this.http.get<Gasto[]>(
      this.api,
      {
        params
      }
    );
  }

  obtener(
    id: number
  ): Observable<Gasto> {

    return this.http.get<Gasto>(
      `${this.api}/${id}`
    );
  }

  crear(
    request: GastoGuardarRequest
  ): Observable<Gasto> {

    return this.http.post<Gasto>(
      this.api,
      request
    );
  }

  actualizar(
    id: number,
    request: GastoGuardarRequest
  ): Observable<Gasto> {

    return this.http.put<Gasto>(
      `${this.api}/${id}`,
      request
    );
  }
}
