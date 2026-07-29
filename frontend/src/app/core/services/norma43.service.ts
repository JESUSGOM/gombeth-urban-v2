import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { MovimientoBancario } from '../models/movimiento-bancario.model';
import { Norma43Previsualizacion } from '../models/norma43.model';

@Injectable({
  providedIn: 'root'
})
export class Norma43Service {

  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/norma43';

  previsualizar(
    comunidadId: number,
    fichero: File
  ): Observable<Norma43Previsualizacion> {
    return this.http.post<Norma43Previsualizacion>(
      `${this.apiUrl}/previsualizar`,
      this.crearFormulario(comunidadId, fichero)
    );
  }

  importar(
    comunidadId: number,
    fichero: File
  ): Observable<MovimientoBancario[]> {
    return this.http.post<MovimientoBancario[]>(
      `${this.apiUrl}/importar`,
      this.crearFormulario(comunidadId, fichero)
    );
  }

  private crearFormulario(
    comunidadId: number,
    fichero: File
  ): FormData {
    const formulario = new FormData();

    formulario.append(
      'comunidadId',
      comunidadId.toString()
    );

    formulario.append(
      'fichero',
      fichero,
      fichero.name
    );

    return formulario;
  }
}
