import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Remesa } from '../models/remesa.model';
import { ValidacionRemesa } from '../models/validacion-remesa.model';

@Injectable({
  providedIn: 'root'
})
export class RemesaService {

  private http = inject(HttpClient);

  private apiUrl =
    'http://localhost:8080/api/remesas';

  getRemesas(
    comunidadId: number
  ): Observable<Remesa[]> {

    return this.http.get<Remesa[]>(
      `${this.apiUrl}?comunidadId=${comunidadId}`
    );
  }

  generarRemesa(
    comunidadId: number,
    fechaCobro: string,
    fechaDesde: string,
    fechaHasta: string
  ): Observable<any> {

    return this.http.post<any>(
      `${this.apiUrl}/generar?comunidadId=${comunidadId}&fechaCobro=${fechaCobro}&fechaDesde=${fechaDesde}&fechaHasta=${fechaHasta}`,
      {}
    );
  }

  validarRemesa(
    remesaId: number
  ) {

    return this.http.get<ValidacionRemesa>(
      `${this.apiUrl}/${remesaId}/validar`
    );

  }
}
