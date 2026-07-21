import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ProcesoRemesaRequest {
  comunidadId: number;
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

  private readonly apiUrl = 'http://localhost:8080/api/remesas/proceso';

  constructor(private http: HttpClient) {}

  generarProceso(request: ProcesoRemesaRequest): Observable<ProcesoRemesaResponse> {
    return this.http.post<ProcesoRemesaResponse>(this.apiUrl, request);
  }
}
