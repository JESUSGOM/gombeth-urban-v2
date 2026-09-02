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
  Gasto
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
}
