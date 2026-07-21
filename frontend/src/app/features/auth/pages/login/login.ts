import {
  CommonModule
} from '@angular/common';

import {
  Component,
  inject
} from '@angular/core';

import {
  FormsModule
} from '@angular/forms';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  finalize
} from 'rxjs';

import {
  AuthService
} from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {

  private readonly authService =
    inject(AuthService);

  private readonly router =
    inject(Router);

  private readonly route =
    inject(ActivatedRoute);

  username = '';

  password = '';

  error = '';

  cargando = false;

  login(): void {

    this.error = '';

    const username =
      this.username.trim();

    if (
      !username ||
      !this.password
    ) {
      this.error =
        'Debe indicar usuario y contraseña.';

      return;
    }

    this.cargando = true;

    this.authService
      .login(
        username,
        this.password
      )
      .pipe(

        finalize(() => {
          this.cargando = false;
        })
      )
      .subscribe({

        next: response => {

          if (!response.ok) {

            this.error =
              response.mensaje ||
              'Login incorrecto.';

            return;
          }

          const returnUrl =
            this.route.snapshot
              .queryParamMap
              .get('returnUrl') ||
            '/dashboard';

          void this.router.navigateByUrl(
            returnUrl
          );
        },

        error: error => {

          console.error(
            'Error de login:',
            error
          );

          this.error =
            error?.error?.mensaje ||
            'Usuario o contraseña incorrectos.';
        }
      });
  }
}
