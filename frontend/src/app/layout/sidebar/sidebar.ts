import {
  Component
} from '@angular/core';

import {
  RouterLink,
  RouterLinkActive
} from '@angular/router';

import {
  AuthService
} from '../../core/services/auth.service';

import {
  UsuarioContextoService
} from '../../core/services/usuario-contexto.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    RouterLink,
    RouterLinkActive
  ],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar {

  cerrandoSesion = false;

  constructor(

    private readonly authService:
    AuthService,

    private readonly usuarioContextoService:
    UsuarioContextoService

  ) {}

  cerrarSesion(): void {

    if (this.cerrandoSesion) {
      return;
    }

    this.cerrandoSesion = true;

    this.usuarioContextoService
      .limpiarCache();

    /*
     * Ahora el logout se realiza primero
     * en Spring Security.
     */
    this.authService
      .logout()
      .subscribe({

        next: () => {
          this.cerrandoSesion = false;
        },

        error: () => {
          this.cerrandoSesion = false;
        }
      });
  }
}
