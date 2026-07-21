import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConceptoCobroListado {
  id: number;
  descripcion: string;
  importe: number;
  periodicidad: string;
  cuentaContableId: number | null;
  cuentaContableCodigo: string | null;
  cuentaContableNombre: string | null;
  comunidadId: number | null;
  activo: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ConceptosService {

  private http = inject(HttpClient);

  private readonly baseUrl =
    'http://localhost:8080/api/conceptos-cobro';

  getByComunidad(
    comunidadId: number
  ): Observable<ConceptoCobroListado[]> {

    return this.http.get<ConceptoCobroListado[]>(
      `${this.baseUrl}/comunidad/${comunidadId}`
    );
  }

  getById(
    id: number
  ): Observable<ConceptoCobroListado> {

    return this.http.get<ConceptoCobroListado>(
      `${this.baseUrl}/${id}`
    );
  }

  save(
    concepto: any
  ): Observable<ConceptoCobroListado> {

    if (concepto.id) {
      return this.http.put<ConceptoCobroListado>(
        `${this.baseUrl}/${concepto.id}`,
        concepto
      );
    }

    return this.http.post<ConceptoCobroListado>(
      this.baseUrl,
      concepto
    );
  }
}
