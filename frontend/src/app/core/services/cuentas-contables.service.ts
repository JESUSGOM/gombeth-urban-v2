import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class CuentasContablesService {

  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/cuentas-contables';

  getByComunidad(comunidadId: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.baseUrl}/comunidad/${comunidadId}`
    );
  }
}
