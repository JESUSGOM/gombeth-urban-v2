import {
  Injectable
} from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

export interface ProcesoRemesaRequest {
  comunidadId: number;
  cuentaPresentadorId: number;
  mes: number;
  anio: number;
  fechaCobro: string;
}

export interface ProcesoRemesaResponse {
  correcto: boolean;
  mensaje: string;
  remesaId: number | null;
  recibos: number;
  ficheroC19: string | null;
  ficheroXml: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class ProcesoRemesaService {

  private readonly apiUrl =
    '/api/remesas/proceso';

  constructor(
    private http: HttpClient
  ) {
  }

  generarProceso(
    request: ProcesoRemesaRequest
  ): Observable<ProcesoRemesaResponse> {

    return this.http.post<ProcesoRemesaResponse>(
      this.apiUrl,
      request
    );
  }
}