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

export interface FacturaOcrResultado {
  proveedor: string | null;
  fechaFactura: string | null;
  importeTotal: number | null;
  numeroFactura: string | null;
  ocrAplicado: boolean;
  advertencias: string[];
}

@Injectable({
  providedIn: 'root'
})
export class GastoService {

  private readonly http =
    inject(HttpClient);

  private readonly api =
    '/api/gastos';

  analizarFactura(
    comunidadId: number,
    file: File
  ): Observable<FacturaOcrResultado> {

    const formData =
      new FormData();

    formData.append(
      'file',
      file
    );

    const params =
      new HttpParams()
        .set(
          'comunidadId',
          comunidadId.toString()
        );

    return this.http.post<FacturaOcrResultado>(
      `${this.api}/ocr`,
      formData,
      {
        params
      }
    );
  }

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

  eliminar(
    id: number
  ): Observable<void> {

    return this.http.delete<void>(
      `${this.api}/${id}`
    );
  }

  pagar(
    id: number,
    fechaPago?: string | null
  ): Observable<Gasto> {

    let params =
      new HttpParams();

    if (
      fechaPago
      && fechaPago.trim() !== ''
    ) {
      params =
        params.set(
          'fechaPago',
          fechaPago
        );
    }

    return this.http.post<Gasto>(
      `${this.api}/${id}/pagar`,
      null,
      {
        params
      }
    );
  }

  deshacerPago(
    id: number,
    fechaAnulacion?: string | null
  ): Observable<Gasto> {

    let params =
      new HttpParams();

    if (
      fechaAnulacion
      && fechaAnulacion.trim() !== ''
    ) {
      params =
        params.set(
          'fechaAnulacion',
          fechaAnulacion
        );
    }

    return this.http.post<Gasto>(
      `${this.api}/${id}/deshacer-pago`,
      null,
      {
        params
      }
    );
  }

  subirPdf(
    id: number,
    file: File
  ): Observable<Gasto> {

    const formData =
      new FormData();

    formData.append(
      'file',
      file
    );

    return this.http.post<Gasto>(
      `${this.api}/${id}/pdf`,
      formData
    );
  }

  obtenerPdf(
    id: number
  ): Observable<Blob> {

    return this.http.get(
      `${this.api}/${id}/pdf`,
      {
        responseType: 'blob'
      }
    );
  }
}
