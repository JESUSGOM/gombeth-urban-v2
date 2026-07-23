import { Injectable, inject } from '@angular/core';
import {
  HttpClient,
  HttpParams
} from '@angular/common/http';
import { Observable } from 'rxjs';

import { Comunidad } from '../models/comunidad.model';
import { PageResponse } from '../models/page-response.model';
import { CoeficientesResumen } from '../models/coeficientes-resumen.model';
import { ConfiguracionReparto } from '../models/configuracion-reparto.model';

@Injectable({
  providedIn: 'root'
})
export class ComunidadService {

  private readonly http = inject(HttpClient);

  private readonly apiUrl =
    '/api/comunidades';

  getComunidades(
    page: number = 0,
    size: number = 10
  ): Observable<PageResponse<Comunidad>> {

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<Comunidad>>(
      this.apiUrl,
      {
        params
      }
    );
  }

  getComunidad(
    id: number
  ): Observable<Comunidad> {

    const params = new HttpParams()
      .set(
        'nocache',
        Date.now().toString()
      );

    return this.http.get<Comunidad>(
      `${this.apiUrl}/${id}`,
      {
        params
      }
    );
  }

  crearComunidad(
    comunidad: Comunidad
  ): Observable<Comunidad> {

    return this.http.post<Comunidad>(
      this.apiUrl,
      comunidad
    );
  }

  actualizarComunidad(
    id: number,
    comunidad: Comunidad
  ): Observable<Comunidad> {

    return this.http.put<Comunidad>(
      `${this.apiUrl}/${id}`,
      comunidad
    );
  }

  obtenerQrComunidad(
    comunidadId: number
  ): Observable<Blob> {

    const params = new HttpParams()
      .set(
        'nocache',
        Date.now().toString()
      );

    return this.http.get(
      `${this.apiUrl}/${comunidadId}/qr`,
      {
        params,
        responseType: 'blob'
      }
    );
  }

  getResumenCoeficientes(
    comunidadId: number
  ): Observable<CoeficientesResumen> {

    return this.http.get<CoeficientesResumen>(
      `${this.apiUrl}/${comunidadId}/coeficientes/resumen`,
      {
        params: {
          nocache: Date.now().toString()
        }
      }
    );
  }

  getConfiguracionReparto(
    comunidadId: number
  ): Observable<ConfiguracionReparto> {

    return this.http.get<ConfiguracionReparto>(
      `${this.apiUrl}/${comunidadId}/configuracion-reparto`,
      {
        params: {
          nocache: Date.now().toString()
        }
      }
    );
  }

  guardarConfiguracionReparto(
    comunidadId: number,
    metodoReparto: string
  ): Observable<ConfiguracionReparto> {

    return this.http.put<ConfiguracionReparto>(
      `${this.apiUrl}/${comunidadId}/configuracion-reparto`,
      {
        metodoReparto
      }
    );
  }
}
