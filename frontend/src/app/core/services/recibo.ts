import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Recibo } from '../models/recibo.model';

@Injectable({
  providedIn: 'root'
})
export class ReciboService {

  private http = inject(HttpClient);
  private apiUrl = '/api/recibos';

  getRecibos(comunidadId: number): Observable<Recibo[]> {
    return this.http.get<Recibo[]>(
      `${this.apiUrl}?comunidadId=${comunidadId}`
    );
  }
}
