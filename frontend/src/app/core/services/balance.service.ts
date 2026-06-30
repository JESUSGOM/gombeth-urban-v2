import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { BalanceLinea } from '../models/balance.model';

@Injectable({
  providedIn: 'root'
})
export class BalanceService {

  private http = inject(HttpClient);

  private readonly api =
    'http://localhost:8080/api/balance';

  obtenerBalance(
    comunidadId: number
  ): Observable<BalanceLinea[]> {

    return this.http.get<BalanceLinea[]>(
      `${this.api}?comunidadId=${comunidadId}`
    );

  }

}
