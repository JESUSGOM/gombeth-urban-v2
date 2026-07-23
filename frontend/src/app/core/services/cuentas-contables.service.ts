import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CuentaContable {
  id: number;
  codigo: string;
  nombre: string;
  tipo: string;
}

@Injectable({
  providedIn: 'root'
})
export class CuentasContablesService {
  private http = inject(HttpClient);
  private readonly api =
    '/api/cuentas-contables';

  listarPorComunidad(
    comunidadId: number
  ): Observable<CuentaContable[]> {
    return this.http.get<CuentaContable[]>(
      `${this.api}/comunidad/${comunidadId}`
    );
  }

  getByComunidad(
    comunidadId: number
  ): Observable<CuentaContable[]> {
    return this.listarPorComunidad(comunidadId);
  }
}
