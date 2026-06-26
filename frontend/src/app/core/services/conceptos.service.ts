import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ConceptosService {

  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/conceptos-cobro';

  getByComunidad(comunidadId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.baseUrl}/comunidad/${comunidadId}`
    );
  }

  getById(id: number): Observable<any> {
    return this.http.get<any>(
      `${this.baseUrl}/${id}`
    );
  }

  save(concepto: any): Observable<any> {

    if (concepto.id) {
      return this.http.put<any>(
        `${this.baseUrl}/${concepto.id}`,
        concepto
      );
    }

    return this.http.post<any>(
      `${this.baseUrl}`,
      concepto
    );
  }
}
