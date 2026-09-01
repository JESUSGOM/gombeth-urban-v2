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

export const adminGuard: CanActivateFn = (
  _route,
  state
) => {

  const authService =
    inject(AuthService);

  const router =
    inject(Router);

  /*
   * Igual que authGuard, no se confía únicamente
   * en el contenido existente en localStorage.
   *
   * Primero se valida la sesión contra el backend.
   * /api/auth/me actualiza además los roles actuales.
   */
  return authService
    .comprobarSesion()
    .pipe(

      map(sesionValida => {

        if (!sesionValida) {

          return router.createUrlTree(
            ['/login'],
            {
              queryParams: {
                returnUrl: state.url
              }
            }
          );
        }

        const usuario =
          authService.getUsuario();

        const esAdministrador =
          usuario?.roles?.includes(
            'ROLE_ADMIN'
          ) ?? false;

        if (esAdministrador) {
          return true;
        }

        /*
         * El usuario tiene una sesión válida,
         * pero carece del rol administrativo.
         */
        return router.createUrlTree(
          ['/dashboard']
        );
      })
    );
};
