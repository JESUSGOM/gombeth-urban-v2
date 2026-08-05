import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Recibo } from '../models/recibo.model';

export interface OperacionReciboResponse {
  correcto: boolean;
  reciboId: number;
  estado: string;
  fechaCobro?: string;
  mensaje: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReciboService {

  private http = inject(HttpClient);

  private apiUrl = '/api/recibos';

  getRecibos(
    comunidadId: number
  ): Observable<Recibo[]> {

    return this.http.get<Recibo[]>(
      `${this.apiUrl}?comunidadId=${comunidadId}`
    );
  }

  descargarPdf(
    reciboId: number
  ): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/${reciboId}/pdf`,
      {
        responseType: 'blob'
      }
    );
  }

  cobrarRecibo(
    reciboId: number,
    fechaCobro?: string
  ): Observable<OperacionReciboResponse> {

    let params = new HttpParams();

    if (fechaCobro) {
      params = params.set(
        'fechaCobro',
        fechaCobro
      );
    }

    return this.http.post<OperacionReciboResponse>(
      `${this.apiUrl}/${reciboId}/cobrar`,
      null,
      {
        params
      }
    );
  }

  anularCobro(
    reciboId: number,
    fechaAnulacion?: string
  ): Observable<OperacionReciboResponse> {

    let params = new HttpParams();

    if (fechaAnulacion) {
      params = params.set(
        'fechaAnulacion',
        fechaAnulacion
      );
    }

    return this.http.post<OperacionReciboResponse>(
      `${this.apiUrl}/${reciboId}/anular-cobro`,
      null,
      {
        params
      }
    );
  }
}
