import {
  Injectable,
  inject
} from '@angular/core';

import {
  HttpClient
} from '@angular/common/http';

import {
  Router
} from '@angular/router';

import {
  BehaviorSubject,
  Observable,
  catchError,
  finalize,
  map,
  of,
  switchMap,
  tap
} from 'rxjs';

export interface UsuarioLogin {

  usuarioId: number;

  username: string;

  administradorId: number | null;

  administradorNombre: string | null;
}

export interface LoginResponse {

  ok: boolean;

  usuarioId: number | null;

  username: string | null;

  administradorId: number | null;

  administradorNombre: string | null;

  mensaje: string;
}

export interface CambioPasswordRequest {

  username: string;

  passwordActual: string;

  nuevaPassword: string;

  confirmarPassword: string;
}

export interface CambioPasswordResponse {

  ok: boolean;

  mensaje: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private readonly http =
    inject(HttpClient);

  private readonly router =
    inject(Router);

  private readonly apiUrl =
    '/api/auth';

  /*
   * localStorage se mantiene temporalmente porque otras
   * pantallas todavía leen el usuario desde allí.
   *
   * No se utiliza como prueba real de autenticación.
   */
  private readonly usuarioSubject =
    new BehaviorSubject<UsuarioLogin | null>(
      this.leerUsuarioLocal()
    );

  readonly usuario$ =
    this.usuarioSubject.asObservable();

  login(
    username: string,
    password: string
  ): Observable<LoginResponse> {

    /*
     * Primero se solicita la cookie XSRF-TOKEN.
     * Después se realiza el POST de autenticación.
     */
    return this.inicializarCsrf().pipe(

      switchMap(() =>
        this.http.post<LoginResponse>(
          `${this.apiUrl}/login`,
          {
            username,
            password
          }
        )
      ),

      tap(response => {

        if (response.ok) {
          this.guardarUsuario(response);
        }
      })
    );
  }

  cambiarPassword(
    request: CambioPasswordRequest
  ): Observable<CambioPasswordResponse> {

    /*
     * El cambio de contraseña también está protegido
     * mediante CSRF, aunque se utilice antes del login.
     */
    return this.inicializarCsrf().pipe(

      switchMap(() =>
        this.http.post<CambioPasswordResponse>(
          `${this.apiUrl}/cambiar-password`,
          request
        )
      )
    );
  }

  comprobarSesion(): Observable<boolean> {

    return this.http
      .get<LoginResponse>(
        `${this.apiUrl}/me`
      )
      .pipe(

        tap(response => {

          if (response.ok) {
            this.guardarUsuario(response);
          }
        }),

        map(response => response.ok),

        catchError(() => {

          this.limpiarContextoLocal();

          return of(false);
        })
      );
  }

  logout(): Observable<void> {

    return this.http
      .post<void>(
        `${this.apiUrl}/logout`,
        {}
      )
      .pipe(

        /*
         * Aunque el servidor no responda, se elimina
         * el contexto local para no dejar la interfaz
         * aparentemente conectada.
         */
        catchError(() =>
          of(void 0)
        ),

        finalize(() => {

          this.limpiarContextoLocal();

          void this.router.navigate([
            '/login'
          ]);
        })
      );
  }

  getUsuario(): UsuarioLogin | null {

    return this.usuarioSubject.value;
  }

  estaLogueadoLocalmente(): boolean {

    return this.usuarioSubject.value !== null;
  }

  private inicializarCsrf():
    Observable<unknown> {

    return this.http.get(
      `${this.apiUrl}/csrf`
    );
  }

  private guardarUsuario(
    response: LoginResponse
  ): void {

    if (
      response.usuarioId === null ||
      response.username === null
    ) {
      return;
    }

    const usuario: UsuarioLogin = {

      usuarioId:
      response.usuarioId,

      username:
      response.username,

      administradorId:
      response.administradorId,

      administradorNombre:
      response.administradorNombre
    };

    localStorage.setItem(
      'usuario',
      JSON.stringify(usuario)
    );

    this.usuarioSubject.next(
      usuario
    );
  }

  private leerUsuarioLocal():
    UsuarioLogin | null {

    const contenido =
      localStorage.getItem(
        'usuario'
      );

    if (!contenido) {
      return null;
    }

    try {

      return JSON.parse(
        contenido
      ) as UsuarioLogin;

    } catch {

      localStorage.removeItem(
        'usuario'
      );

      return null;
    }
  }

  private limpiarContextoLocal(): void {

    localStorage.removeItem(
      'usuario'
    );

    localStorage.removeItem(
      'comunidadActiva'
    );

    Object.keys(localStorage)
      .filter(clave =>
        clave.startsWith(
          'comunidades_usuario_'
        )
      )
      .forEach(clave =>
        localStorage.removeItem(
          clave
        )
      );

    sessionStorage.clear();

    this.usuarioSubject.next(
      null
    );
  }
}
