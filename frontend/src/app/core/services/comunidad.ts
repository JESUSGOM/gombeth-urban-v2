import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Comunidad } from '../models/comunidad.model';
import { PageResponse } from '../models/page-response.model';

import { CoeficientesResumen } from '../models/coeficientes-resumen.model';
import { ConfiguracionReparto } from '../models/configuracion-reparto.model';

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

  getComunidad(
    id: number,
    usuarioId?: number,
    administradorId?: number
  ): Observable<Comunidad> {

    let url = `${this.apiUrl}/${id}`;

    const params: string[] = [];

    if (usuarioId) {
      params.push(`usuarioId=${usuarioId}`);
    }

    if (administradorId) {
      params.push(`administradorId=${administradorId}`);
    }

    if (params.length > 0) {
      url += `?${params.join('&')}`;
    }

    return this.http.get<Comunidad>(url);
  }

  actualizarComunidad(
    id: number,
    comunidad: Comunidad,
    usuarioId?: number,
    administradorId?: number
  ): Observable<Comunidad> {

    let url = `${this.apiUrl}/${id}`;

    const params: string[] = [];

    if (usuarioId) {
      params.push(`usuarioId=${usuarioId}`);
    }

    if (administradorId) {
      params.push(`administradorId=${administradorId}`);
    }

    if (params.length > 0) {
      url += `?${params.join('&')}`;
    }

    return this.http.put<Comunidad>(
      url,
      comunidad
    );
  }

  getResumenCoeficientes(
    comunidadId: number
  ): Observable<CoeficientesResumen> {
    return this.http.get<CoeficientesResumen>(
      `${this.apiUrl}/${comunidadId}/coeficientes/resumen`
    );
  }

  getConfiguracionReparto(
    comunidadId: number
  ): Observable<ConfiguracionReparto> {
    return this.http.get<ConfiguracionReparto>(
      `${this.apiUrl}/${comunidadId}/configuracion-reparto`
    );
  }

  guardarConfiguracionReparto(
    comunidadId: number,
    metodoReparto: string
  ): Observable<ConfiguracionReparto> {
    return this.http.put<ConfiguracionReparto>(
      `${this.apiUrl}/${comunidadId}/configuracion-reparto`,
      { metodoReparto }
    );
  }
}
