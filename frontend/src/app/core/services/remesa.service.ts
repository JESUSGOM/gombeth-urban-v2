import {
  Injectable,
  inject
} from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

import {
  Remesa
} from '../models/remesa.model';

import {
  RemesaDetalle
} from '../models/remesa-detalle.model';

import {
  RemesaEvento
} from '../models/remesa-evento.model';

import {
  ValidacionRemesa
} from '../models/validacion-remesa.model';

@Injectable({
  providedIn: 'root'
})
export class RemesaService {

  private http =
    inject(HttpClient);

  private readonly apiUrl =
    '/api/remesas';

  getRemesas(
    comunidadId: number
  ): Observable<Remesa[]> {

    return this.http.get<Remesa[]>(
      `${this.apiUrl}?comunidadId=${comunidadId}`
    );
  }

  /**
   * Recupera el detalle completo de una remesa.
   *
   * Para remesas modernas utiliza remesa_lineas.
   * Para remesas históricas reconstruye automáticamente
   * las líneas desde el contenido del fichero C19.
   */
  getDetalle(
    remesaId: number
  ): Observable<RemesaDetalle> {

    return this.http.get<RemesaDetalle>(
      `${this.apiUrl}/${remesaId}/detalle-completo`
    );
  }

  getEventos(
    remesaId: number
  ): Observable<RemesaEvento[]> {

    return this.http.get<RemesaEvento[]>(
      `${this.apiUrl}/${remesaId}/eventos`
    );
  }

  generarRemesa(
    comunidadId: number,
    cuentaPresentadorId: number,
    fechaCobro: string,
    fechaDesde: string,
    fechaHasta: string
  ): Observable<any> {

    return this.http.post<any>(
      `${this.apiUrl}/generar` +
      `?comunidadId=${comunidadId}` +
      `&cuentaPresentadorId=${cuentaPresentadorId}` +
      `&fechaCobro=${fechaCobro}` +
      `&fechaDesde=${fechaDesde}` +
      `&fechaHasta=${fechaHasta}`,
      {}
    );
  }

  generarRemesaSeleccion(
    comunidadId: number,
    cuentaPresentadorId: number,
    fechaCobro: string,
    reciboIds: number[]
  ): Observable<any> {

    return this.http.post<any>(
      `${this.apiUrl}/generar-seleccion`,
      {
        comunidadId,
        cuentaPresentadorId,
        fechaCobro,
        reciboIds
      }
    );
  }

  anularRemesa(
    remesaId: number
  ): Observable<{
    remesaId: number;
    estado: string;
    mensaje: string;
  }> {

    return this.http.post<{
      remesaId: number;
      estado: string;
      mensaje: string;
    }>(
      `${this.apiUrl}/${remesaId}/anular`,
      {}
    );
  }

  presentarRemesa(
    remesaId: number
  ): Observable<{
    remesaId: number;
    estado: string;
    mensaje: string;
  }> {

    return this.http.post<{
      remesaId: number;
      estado: string;
      mensaje: string;
    }>(
      `${this.apiUrl}/${remesaId}/presentar`,
      {}
    );
  }

  validarRemesa(
    remesaId: number
  ): Observable<ValidacionRemesa> {

    return this.http.get<ValidacionRemesa>(
      `${this.apiUrl}/${remesaId}/validar`
    );
  }

  descargarXml(
    remesaId: number
  ): void {

    window.open(
      `${this.apiUrl}/${remesaId}/xml`,
      '_blank'
    );
  }

  descargarC19(
    remesaId: number
  ): void {

    window.open(
      `${this.apiUrl}/${remesaId}/c19`,
      '_blank'
    );
  }
}
