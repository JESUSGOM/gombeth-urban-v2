import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Remesa } from '../models/remesa.model';
import { RemesaDetalle } from '../models/remesa-detalle.model';
import { ValidacionRemesa } from '../models/validacion-remesa.model';

@Injectable({
  providedIn: 'root'
})
export class RemesaService {

  private http = inject(HttpClient);

  private readonly apiUrl =
      'http://localhost:8080/api/remesas';

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

  generarRemesa(
      comunidadId: number,
      fechaCobro: string,
      fechaDesde: string,
      fechaHasta: string
  ): Observable<any> {

    return this.http.post<any>(
        `${this.apiUrl}/generar` +
        `?comunidadId=${comunidadId}` +
        `&fechaCobro=${fechaCobro}` +
        `&fechaDesde=${fechaDesde}` +
        `&fechaHasta=${fechaHasta}`,
        {}
    );
  }

  generarRemesaSeleccion(
      comunidadId: number,
      fechaCobro: string,
      reciboIds: number[]
  ): Observable<any> {

    return this.http.post<any>(
        `${this.apiUrl}/generar-seleccion`,
        {
          comunidadId,
          fechaCobro,
          reciboIds
        }
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
