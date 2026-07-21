import { inject } from '@angular/core';
import {
  HttpErrorResponse,
  HttpInterceptorFn
} from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

const API_LOCAL = 'http://localhost:8080/api/';

export const sessionInterceptor: HttpInterceptorFn = (
  request,
  next
) => {

  const router = inject(Router);

  /*
   * No se modifican las peticiones que no pertenecen
   * a la API de Gombeth Urban.
   */
  if (!esPeticionApi(request.url)) {
    return next(request);
  }

  let headers = request.headers;
  const metodo = request.method.toUpperCase();

  /*
   * Para operaciones que modifican datos se envía el
   * token leído de la cookie XSRF-TOKEN.
   */
  if (!['GET', 'HEAD', 'OPTIONS'].includes(metodo)) {

    const tokenXsrf = leerCookie('XSRF-TOKEN');

    if (
      tokenXsrf &&
      !headers.has('X-XSRF-TOKEN')
    ) {
      headers = headers.set(
        'X-XSRF-TOKEN',
        tokenXsrf
      );
    }
  }

  /*
   * withCredentials permite enviar y recibir
   * la cookie de sesión JSESSIONID.
   */
  const peticionConSesion = request.clone({
    withCredentials: true,
    headers
  });

  return next(peticionConSesion).pipe(

    catchError((error: HttpErrorResponse) => {

      /*
       * Si la sesión ha caducado, se limpia el contexto
       * local y se vuelve a la pantalla de login.
       *
       * No se fuerza una nueva navegación cuando el 401
       * procede del propio intento de login.
       */
      if (
        error.status === 401 &&
        !esPeticionDeLogin(request.url)
      ) {
        limpiarContextoLocal();

        if (router.url !== '/login') {
          void router.navigate(['/login']);
        }
      }

      return throwError(() => error);
    })
  );
};

function esPeticionApi(
  url: string
): boolean {

  return (
    url.startsWith('/api/') ||
    url.startsWith(API_LOCAL)
  );
}

function esPeticionDeLogin(
  url: string
): boolean {

  return url.endsWith('/api/auth/login');
}

function leerCookie(
  nombre: string
): string | null {

  const prefijo = `${nombre}=`;

  const cookie = document.cookie
    .split(';')
    .map(valor => valor.trim())
    .find(valor => valor.startsWith(prefijo));

  if (!cookie) {
    return null;
  }

  return decodeURIComponent(
    cookie.substring(prefijo.length)
  );
}

function limpiarContextoLocal(): void {

  localStorage.removeItem('usuario');
  localStorage.removeItem('comunidadActiva');

  Object.keys(localStorage)
    .filter(clave =>
      clave.startsWith('comunidades_usuario_')
    )
    .forEach(clave =>
      localStorage.removeItem(clave)
    );

  sessionStorage.clear();
}
