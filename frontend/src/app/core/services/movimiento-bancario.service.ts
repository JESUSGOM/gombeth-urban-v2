import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { MovimientoBancario } from '../models/movimiento-bancario.model';

@Injectable({
  providedIn: 'root'
})
export class MovimientoBancarioService {

  private http = inject(HttpClient);

  private apiUrl = '/api/movimientos';

  getMovimientos(
    comunidadId: number,
    usuarioId: number
  ): Observable<MovimientoBancario[]> {

    return this.http.get<MovimientoBancario[]>(
      `${this.apiUrl}?comunidadId=${comunidadId}&usuarioId=${usuarioId}`
    );
  }

  getRecibosPendientes(
    movimientoId: number,
    usuarioId: number
  ): Observable<any[]> {

    return this.http.get<any[]>(
      `${this.apiUrl}/${movimientoId}/recibos-pendientes?usuarioId=${usuarioId}`
    );
  }

  conciliarMovimiento(
    movimientoId: number,
    reciboIds: number[],
    usuarioId: number
  ): Observable<any> {

    return this.http.post<any>(
      `${this.apiUrl}/${movimientoId}/conciliar?usuarioId=${usuarioId}`,
      {
        reciboIds
      }
    );
  }

  getContextoMovimiento(
    movimientoId: number,
    usuarioId: number
  ): Observable<any> {

    return this.http.get<any>(
      `${this.apiUrl}/${movimientoId}/contexto?usuarioId=${usuarioId}`
    );
  }

  getNombreComunidad(
    comunidadId: number,
    usuarioId: number
  ): Observable<any> {

    return this.http.get<any>(
      `${this.apiUrl}/comunidad/${comunidadId}/nombre?usuarioId=${usuarioId}`
    );
  }

  getResumenTesoreria(
    comunidadId: number,
    usuarioId: number
  ): Observable<any> {

    return this.http.get<any>(
      `${this.apiUrl}/resumen?comunidadId=${comunidadId}&usuarioId=${usuarioId}`
    );
  }

  getCandidatosConciliacion(
    movimientoId: number,
    usuarioId: number
  ): Observable<any[]> {

    return this.http.get<any[]>(
      `${this.apiUrl}/${movimientoId}/candidatos?usuarioId=${usuarioId}`
    );
  }
}
