import {
  inject,
  Injectable
} from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Observable
} from 'rxjs';

import {
  Proveedor,
  ProveedorComunidad,
  ProveedorGuardarRequest
} from '../models/proveedor.model';

@Injectable({
  providedIn: 'root'
})
export class ProveedorService {

  private readonly http =
    inject(HttpClient);

  private readonly api =
    '/api/proveedores';

  listar(): Observable<Proveedor[]> {

    return this.http.get<Proveedor[]>(
      this.api
    );
  }

  obtener(
    proveedorId: number
  ): Observable<Proveedor> {

    return this.http.get<Proveedor>(
      `${this.api}/${proveedorId}`
    );
  }

  crear(
    request: ProveedorGuardarRequest
  ): Observable<Proveedor> {

    return this.http.post<Proveedor>(
      this.api,
      request
    );
  }

  actualizar(
    proveedorId: number,
    request: ProveedorGuardarRequest
  ): Observable<Proveedor> {

    return this.http.put<Proveedor>(
      `${this.api}/${proveedorId}`,
      request
    );
  }

  listarPorComunidad(
    comunidadId: number
  ): Observable<ProveedorComunidad[]> {

    return this.http.get<ProveedorComunidad[]>(
      `/api/comunidades/${comunidadId}/proveedores`
    );
  }

  asociarAComunidad(
    comunidadId: number,
    proveedorId: number
  ): Observable<ProveedorComunidad> {

    return this.http.post<ProveedorComunidad>(
      `/api/comunidades/${comunidadId}/proveedores/${proveedorId}`,
      null
    );
  }
}
