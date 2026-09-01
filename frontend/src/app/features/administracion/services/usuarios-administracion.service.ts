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

export interface AdministradorResumen {
  administradorId: number;
  nombre: string;
}

export interface RolAdministracion {
  rolId: number;
  nombre: string;
}

export interface ComunidadAdministracion {
  comunidadId: number;
  nombreComunidad: string;
}

export interface UsuarioAdministracion {
  usuarioId: number;
  username: string;
  administrador: AdministradorResumen;
  roles: RolAdministracion[];
  comunidadesDirectas: ComunidadAdministracion[];
  comunidadesCompartidas: ComunidadAdministracion[];
}

export interface UsuarioAdministracionAlta {
  username: string;
  passwordInicial: string;
  administradorId: number | null;
  rolIds: number[];
  comunidadCompartidaIds: number[];
}

export interface UsuarioAdministracionEdicion {
  username: string;
  administradorId: number | null;
  rolIds: number[];
  comunidadCompartidaIds: number[];
}

@Injectable({
  providedIn: 'root'
})
export class UsuariosAdministracionService {

  private readonly http =
    inject(HttpClient);

  private readonly usuariosApiUrl =
    '/api/admin/usuarios';

  private readonly catalogosApiUrl =
    '/api/admin/catalogos';

  listarUsuarios():
    Observable<UsuarioAdministracion[]> {

    return this.http.get<UsuarioAdministracion[]>(
      this.usuariosApiUrl,
      {
        params: {
          nocache: Date.now().toString()
        }
      }
    );
  }

  obtenerUsuario(
    usuarioId: number
  ): Observable<UsuarioAdministracion> {

    return this.http.get<UsuarioAdministracion>(
      `${this.usuariosApiUrl}/${usuarioId}`,
      {
        params: {
          nocache: Date.now().toString()
        }
      }
    );
  }

  crearUsuario(
    request: UsuarioAdministracionAlta
  ): Observable<UsuarioAdministracion> {

    return this.http.post<UsuarioAdministracion>(
      this.usuariosApiUrl,
      request
    );
  }

  editarUsuario(
    usuarioId: number,
    request: UsuarioAdministracionEdicion
  ): Observable<UsuarioAdministracion> {

    return this.http.put<UsuarioAdministracion>(
      `${this.usuariosApiUrl}/${usuarioId}`,
      request
    );
  }

  listarRoles():
    Observable<RolAdministracion[]> {

    return this.http.get<RolAdministracion[]>(
      `${this.catalogosApiUrl}/roles`,
      {
        params: {
          nocache: Date.now().toString()
        }
      }
    );
  }

  listarComunidades():
    Observable<ComunidadAdministracion[]> {

    return this.http.get<ComunidadAdministracion[]>(
      `${this.catalogosApiUrl}/comunidades`,
      {
        params: {
          nocache: Date.now().toString()
        }
      }
    );
  }
}
