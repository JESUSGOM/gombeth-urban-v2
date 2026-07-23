import {
  Injectable,
  inject
} from '@angular/core';

import {
  HttpClient,
  HttpParams
} from '@angular/common/http';

import { Observable } from 'rxjs';

import {
  VecinoDocumento
} from '../models/vecino-documento.model';

@Injectable({
  providedIn: 'root'
})
export class VecinoDocumentoService {

  private http = inject(HttpClient);

  private readonly apiUrl =
    '/api/vecino-documentos';

  listarPorVecino(
    vecinoId: number
  ): Observable<VecinoDocumento[]> {
    return this.http.get<VecinoDocumento[]>(
      `${this.apiUrl}/vecino/${vecinoId}`
    );
  }

  subirMandatoFirmado(
    vecinoId: number,
    archivo: File
  ): Observable<VecinoDocumento> {
    const formData = new FormData();

    formData.append(
      'file',
      archivo
    );

    const parametros = new HttpParams()
      .set(
        'tipoDocumento',
        'MANDATO_SEPA_FIRMADO'
      );

    return this.http.post<VecinoDocumento>(
      `${this.apiUrl}/vecino/${vecinoId}`,
      formData,
      {
        params: parametros
      }
    );
  }

  obtenerUrlVisualizacion(
    documentoId: number
  ): string {
    return `${this.apiUrl}/${documentoId}`;
  }

  obtenerUrlDescarga(
    documentoId: number
  ): string {
    return `${this.apiUrl}/${documentoId}/descarga`;
  }

  eliminar(
    documentoId: number
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/${documentoId}`
    );
  }
}
