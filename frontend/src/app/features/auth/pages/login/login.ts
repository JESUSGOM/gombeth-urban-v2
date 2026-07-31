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

  mostrarPassword = false;

  error = '';

  mensajeExito = '';

  cargando = false;

  mostrarCambioPassword = false;

  cambioUsername = '';

  passwordActual = '';

  nuevaPassword = '';

  confirmarPassword = '';

  mostrarPasswordActual = false;

  mostrarNuevaPassword = false;

  mostrarConfirmacionPassword = false;

  errorCambio = '';

  cambiandoPassword = false;

  alternarVisibilidadPassword(): void {

    this.mostrarPassword =
      !this.mostrarPassword;
  }

  alternarVisibilidadPasswordActual(): void {

    this.mostrarPasswordActual =
      !this.mostrarPasswordActual;
  }

  alternarVisibilidadNuevaPassword(): void {

    this.mostrarNuevaPassword =
      !this.mostrarNuevaPassword;
  }

  alternarVisibilidadConfirmacion(): void {

    this.mostrarConfirmacionPassword =
      !this.mostrarConfirmacionPassword;
  }

  alternarPanelCambioPassword(): void {

    this.mostrarCambioPassword =
      !this.mostrarCambioPassword;

    this.errorCambio = '';
    this.mensajeExito = '';

    if (
      this.mostrarCambioPassword &&
      !this.cambioUsername.trim()
    ) {
      this.cambioUsername =
        this.username.trim();
    }

    if (!this.mostrarCambioPassword) {
      this.limpiarPasswordsCambio();
    }
  }

  cancelarCambioPassword(): void {

    this.mostrarCambioPassword = false;
    this.errorCambio = '';

    this.limpiarPasswordsCambio();
  }

  login(): void {

    this.error = '';
    this.mensajeExito = '';

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

  cambiarPassword(): void {

    this.errorCambio = '';
    this.mensajeExito = '';

    const username =
      this.cambioUsername.trim();

    if (
      !username ||
      !this.passwordActual ||
      !this.nuevaPassword ||
      !this.confirmarPassword
    ) {
      this.errorCambio =
        'Debe completar todos los campos.';

      return;
    }

    if (
      this.nuevaPassword !==
      this.confirmarPassword
    ) {
      this.errorCambio =
        'La nueva contraseña y su confirmación no coinciden.';

      return;
    }

    if (
      this.nuevaPassword.length < 8
    ) {
      this.errorCambio =
        'La nueva contraseña debe tener al menos 8 caracteres.';

      return;
    }

    const contieneMayuscula =
      /[A-ZÁÉÍÓÚÑ]/.test(
        this.nuevaPassword
      );

    const contieneMinuscula =
      /[a-záéíóúñ]/.test(
        this.nuevaPassword
      );

    const contieneNumero =
      /\d/.test(
        this.nuevaPassword
      );

    if (
      !contieneMayuscula ||
      !contieneMinuscula ||
      !contieneNumero
    ) {
      this.errorCambio =
        'La nueva contraseña debe contener mayúsculas, minúsculas y números.';

      return;
    }

    this.cambiandoPassword = true;

    this.authService
      .cambiarPassword({
        username,
        passwordActual:
        this.passwordActual,
        nuevaPassword:
        this.nuevaPassword,
        confirmarPassword:
        this.confirmarPassword
      })
      .pipe(

        finalize(() => {
          this.cambiandoPassword = false;
        })
      )
      .subscribe({

        next: response => {

          if (!response.ok) {

            this.errorCambio =
              response.mensaje ||
              'No se ha podido cambiar la contraseña.';

            return;
          }

          this.username = username;
          this.password = '';

          this.mensajeExito =
            response.mensaje;

          this.mostrarCambioPassword =
            false;

          this.limpiarPasswordsCambio();
        },

        error: error => {

          console.error(
            'Error al cambiar la contraseña:',
            error
          );

          this.errorCambio =
            error?.error?.mensaje ||
            'No se ha podido cambiar la contraseña.';
        }
      });
  }

  private limpiarPasswordsCambio(): void {

    this.passwordActual = '';
    this.nuevaPassword = '';
    this.confirmarPassword = '';

    this.mostrarPasswordActual = false;
    this.mostrarNuevaPassword = false;
    this.mostrarConfirmacionPassword = false;
  }
}
