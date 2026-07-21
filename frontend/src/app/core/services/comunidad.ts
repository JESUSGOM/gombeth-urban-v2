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
    'http://localhost:8080/api/comunidades';

  getComunidades(
    page: number = 0,
    size: number = 10,
    usuarioId?: number
  ): Observable<PageResponse<Comunidad>> {

    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (usuarioId !== undefined) {
      params = params.set(
        'usuarioId',
        usuarioId.toString()
      );
    }

    return this.http.get<PageResponse<Comunidad>>(
      this.apiUrl,
      {
        params
      }
    );
  }

  getComunidad(
    id: number,
    usuarioId?: number,
    administradorId?: number
  ): Observable<Comunidad> {

    let params = new HttpParams()
      .set(
        'nocache',
        Date.now().toString()
      );

    if (usuarioId !== undefined) {
      params = params.set(
        'usuarioId',
        usuarioId.toString()
      );
    }

    if (administradorId !== undefined) {
      params = params.set(
        'administradorId',
        administradorId.toString()
      );
    }

    return this.http.get<Comunidad>(
      `${this.apiUrl}/${id}`,
      {
        params
      }
    );
  }

  actualizarComunidad(
    id: number,
    comunidad: Comunidad,
    usuarioId?: number,
    administradorId?: number
  ): Observable<Comunidad> {

    let params = new HttpParams();

    if (usuarioId !== undefined) {
      params = params.set(
        'usuarioId',
        usuarioId.toString()
      );
    }

    if (administradorId !== undefined) {
      params = params.set(
        'administradorId',
        administradorId.toString()
      );
    }

    return this.http.put<Comunidad>(
      `${this.apiUrl}/${id}`,
      comunidad,
      {
        params
      }
    );
  }

  obtenerQrComunidad(
    comunidadId: number,
    usuarioId?: number,
    administradorId?: number
  ): Observable<Blob> {

    let params = new HttpParams()
      .set(
        'nocache',
        Date.now().toString()
      );

    if (usuarioId !== undefined) {
      params = params.set(
        'usuarioId',
        usuarioId.toString()
      );
    }

    if (administradorId !== undefined) {
      params = params.set(
        'administradorId',
        administradorId.toString()
      );
    }

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
