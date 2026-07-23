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
    comunidadId: number
  ): Observable<MovimientoBancario[]> {

    return this.http.get<MovimientoBancario[]>(
      `${this.apiUrl}?comunidadId=${comunidadId}`
    );
  }

  getRecibosPendientes(
    movimientoId: number
  ): Observable<any[]> {

    return this.http.get<any[]>(
      `${this.apiUrl}/${movimientoId}/recibos-pendientes`
    );
  }

  conciliarMovimiento(
    movimientoId: number,
    reciboIds: number[]
  ): Observable<any> {

    return this.http.post<any>(
      `${this.apiUrl}/${movimientoId}/conciliar`,
      {
        reciboIds
      }
    );
  }

  getContextoMovimiento(
    movimientoId: number
  ): Observable<any> {

    return this.http.get<any>(
      `${this.apiUrl}/${movimientoId}/contexto`
    );
  }

  getNombreComunidad(
    comunidadId: number
  ): Observable<any> {

    return this.http.get<any>(
      `${this.apiUrl}/comunidad/${comunidadId}/nombre`
    );
  }

  getResumenTesoreria(
    comunidadId: number
  ): Observable<any> {

    return this.http.get<any>(
      `${this.apiUrl}/resumen?comunidadId=${comunidadId}`
    );
  }

  getCandidatosConciliacion(
    movimientoId: number
  ): Observable<any[]> {

    return this.http.get<any[]>(
      `${this.apiUrl}/${movimientoId}/candidatos`
    );
  }
}
