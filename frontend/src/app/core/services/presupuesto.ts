import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Presupuesto } from '../models/presupuesto.model';
import { RepartoPresupuesto } from '../models/reparto-presupuesto.model';
import { CuotaPresupuesto } from '../models/cuota-presupuesto.model';
import { PresupuestoRevision } from '../models/presupuesto-revision.model';

export interface PresupuestoAltaRequest {
  cuentaId: number;
  anio: number;
  importe: number;
  metodoReparto: 'COEFICIENTE' | 'PARTES_IGUALES';
  aplicaTodos: boolean;
  vecinoIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class PresupuestoService {

  private http = inject(HttpClient);
  private apiUrl = '/api/presupuestos';

  getPresupuestosComunidad(
    comunidadId: number,
    anio: number
  ): Observable<Presupuesto[]> {
    return this.http.get<Presupuesto[]>(
      `${this.apiUrl}/comunidad/${comunidadId}?anio=${anio}`
    );
  }

  crearPartida(
    comunidadId: number,
    request: PresupuestoAltaRequest
  ): Observable<Presupuesto> {
    return this.http.post<Presupuesto>(
      `${this.apiUrl}/comunidad/${comunidadId}`,
      request
    );
  }

  modificarPartida(
    presupuestoId: number,
    request: PresupuestoAltaRequest
  ): Observable<Presupuesto> {
    return this.http.put<Presupuesto>(
      `${this.apiUrl}/partidas/${presupuestoId}`,
      request
    );
  }

  eliminarPartida(
    presupuestoId: number
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/partidas/${presupuestoId}`
    );
  }

  getResumenComunidad(
    comunidadId: number,
    anio: number
  ): Observable<number> {
    return this.http.get<number>(
      `${this.apiUrl}/comunidad/${comunidadId}/resumen?anio=${anio}`
    );
  }

  getRepartoComunidad(
    comunidadId: number,
    anio: number
  ): Observable<RepartoPresupuesto[]> {
    return this.http.get<RepartoPresupuesto[]>(
      `${this.apiUrl}/comunidad/${comunidadId}/reparto?anio=${anio}`
    );
  }

  generarBorradorCuotas(
    comunidadId: number,
    anio: number
  ): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/comunidad/${comunidadId}/generar-borrador-cuotas?anio=${anio}`,
      {}
    );
  }

  getCuotasBorrador(
    comunidadId: number,
    anio: number
  ): Observable<CuotaPresupuesto[]> {
    return this.http.get<CuotaPresupuesto[]>(
      `${this.apiUrl}/comunidad/${comunidadId}/cuotas-borrador?anio=${anio}`
    );
  }

  aprobarCuotas(
    comunidadId: number,
    anio: number
  ): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/comunidad/${comunidadId}/aprobar-cuotas?anio=${anio}`,
      {}
    );
  }

  getRevisiones(
    comunidadId: number,
    anio: number
  ): Observable<PresupuestoRevision[]> {
    return this.http.get<PresupuestoRevision[]>(
      `${this.apiUrl}/comunidad/${comunidadId}/revisiones?anio=${anio}`
    );
  }

  aprobarRevision(
    revisionId: number
  ): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/revisiones/${revisionId}/aprobar`,
      {}
    );
  }

  eliminarRevision(
    revisionId: number
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.apiUrl}/revisiones/${revisionId}`
    );
  }

  generarRecibos(
    comunidadId: number,
    anio: number,
    mes: number
  ): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/comunidad/${comunidadId}/generar-recibos?anio=${anio}&mes=${mes}`,
      {}
    );
  }
}
