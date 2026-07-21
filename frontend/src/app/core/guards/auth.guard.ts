import {
  inject
} from '@angular/core';

import {
  CanActivateFn,
  Router
} from '@angular/router';

import {
  map
} from 'rxjs';

import {
  AuthService
} from '../services/auth.service';

export const authGuard: CanActivateFn = (
  _route,
  state
) => {

  const authService =
    inject(AuthService);

  const router =
    inject(Router);

  /*
   * La autorización ya no depende de que exista
   * un objeto en localStorage.
   *
   * Se pregunta al servidor si JSESSIONID
   * corresponde a una sesión válida.
   */
  return authService
    .comprobarSesion()
    .pipe(

      map(sesionValida => {

        if (sesionValida) {
          return true;
        }

        return router.createUrlTree(
          ['/login'],
          {
            queryParams: {
              returnUrl: state.url
            }
          }
        );
      })
    );
};
