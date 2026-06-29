import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import {
  Diario,
  DiarioDetalle
} from '../models/diario.model';

@Injectable({
  providedIn: 'root'
})
export class DiarioService {

  private http = inject(HttpClient);

  private readonly api =
    'http://localhost:8080/api/diario';
  listar(
    comunidadId: number,
    ejercicio: number
  ): Observable<Diario[]> {
    return this.http.get<Diario[]>(
      `${this.api}?comunidadId=${comunidadId}&ejercicio=${ejercicio}`
    );
  }

  detalle(
    id: number
  ): Observable<DiarioDetalle> {
    return this.http.get<DiarioDetalle>(
      `${this.api}/${id}`
    );
  }
}
