import {
  CommonModule
} from '@angular/common';

import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';

import {
  Router
} from '@angular/router';

import {
  Subject
} from 'rxjs';

import {
  takeUntil
} from 'rxjs/operators';

import {
  UsuarioAdministracion,
  UsuariosAdministracionService
} from '../../services/usuarios-administracion.service';

@Component({
  selector: 'app-usuarios-list',
  standalone: true,
  imports: [
    CommonModule
  ],
  template: `
    <div class="usuarios-page">

      <div class="page-header">

        <div>
          <h2>
            Administración de usuarios
          </h2>

          <p>
            Gestión de usuarios, roles y comunidades asignadas.
          </p>
        </div>

        <div class="header-actions">

          <button
            type="button"
            [disabled]="cargando"
            (click)="actualizarListado()">

            {{
              cargando
                ? 'Actualizando...'
                : 'Actualizar'
            }}

          </button>

          <button
            type="button"
            class="primary-button"
            (click)="nuevoUsuario()">

            + Nuevo usuario

          </button>

        </div>

      </div>

      <div
        *ngIf="cargando"
        class="estado-card">

        Cargando usuarios...

      </div>

      <div
        *ngIf="error"
        class="estado-card error">

        {{ error }}

      </div>

      <div
        *ngIf="
          !cargando
          && !error
          && usuarios.length === 0
        "
        class="estado-card">

        No existen usuarios para este administrador.

      </div>

      <div
        *ngIf="
          !cargando
          && !error
          && usuarios.length > 0
        ">

        <div class="usuarios-resumen">

          Usuarios encontrados:
          <strong>
            {{ usuarios.length }}
          </strong>

        </div>

        <div class="table-card">

          <table class="usuarios-table">

            <thead>

              <tr>
                <th>ID</th>
                <th>Usuario</th>
                <th>Administrador</th>
                <th>Roles</th>
                <th>Comunidades directas</th>
                <th>Comunidades compartidas</th>
                <th>Acciones</th>
              </tr>

            </thead>

            <tbody>

              <tr
                *ngFor="
                  let usuario of usuarios;
                  trackBy: trackByUsuario
                ">

                <td class="id-col">
                  {{ usuario.usuarioId }}
                </td>

                <td class="usuario-col">

                  <strong>
                    {{ usuario.username }}
                  </strong>

                </td>

                <td class="administrador-col">

                  {{
                    usuario.administrador.nombre
                    || '-'
                  }}

                </td>

                <td class="roles-col">

                  <ng-container
                    *ngIf="
                      usuario.roles
                      && usuario.roles.length > 0;
                      else sinRol
                    ">

                    <span
                      *ngFor="let rol of usuario.roles"
                      class="rol-badge">

                      {{ rol.nombre }}

                    </span>

                  </ng-container>

                  <ng-template #sinRol>

                    <span class="sin-datos">
                      Sin rol explícito
                    </span>

                  </ng-template>

                </td>

                <td class="comunidades-col">

                  <ul
                    *ngIf="
                      usuario.comunidadesDirectas
                      && usuario.comunidadesDirectas.length > 0;
                      else sinComunidadesDirectas
                    "
                    class="comunidades-lista">

                    <li
                      *ngFor="
                        let comunidad
                        of usuario.comunidadesDirectas
                      ">

                      {{ comunidad.nombreComunidad }}

                    </li>

                  </ul>

                  <ng-template
                    #sinComunidadesDirectas>

                    <span class="sin-datos">
                      Ninguna
                    </span>

                  </ng-template>

                </td>

                <td class="comunidades-col">

                  <ul
                    *ngIf="
                      usuario.comunidadesCompartidas
                      && usuario.comunidadesCompartidas.length > 0;
                      else sinComunidadesCompartidas
                    "
                    class="comunidades-lista">

                    <li
                      *ngFor="
                        let comunidad
                        of usuario.comunidadesCompartidas
                      ">

                      {{ comunidad.nombreComunidad }}

                    </li>

                  </ul>

                  <ng-template
                    #sinComunidadesCompartidas>

                    <span class="sin-datos">
                      Ninguna
                    </span>

                  </ng-template>

                </td>

                <td class="acciones-col">

                  <button
                    type="button"
                    (click)="editarUsuario(
                      usuario.usuarioId
                    )">

                    Ver / Editar

                  </button>

                </td>

              </tr>

            </tbody>

          </table>

        </div>

      </div>

    </div>
  `,
  styleUrl: './usuarios-list.scss'
})
export class UsuariosList
  implements OnInit, OnDestroy {

  private readonly usuariosService =
    inject(UsuariosAdministracionService);

  private readonly router =
    inject(Router);

  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  private readonly destruir$ =
    new Subject<void>();

  private componenteDestruido = false;

  usuarios: UsuarioAdministracion[] = [];

  cargando = false;

  error = '';

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  ngOnDestroy(): void {
    this.componenteDestruido = true;

    this.destruir$.next();
    this.destruir$.complete();
  }

  cargarUsuarios(): void {

    this.cargando = true;
    this.error = '';

    this.usuariosService
      .listarUsuarios()
      .pipe(
        takeUntil(this.destruir$)
      )
      .subscribe({

        next: (
          data: UsuarioAdministracion[]
        ) => {

          this.usuarios = [
            ...(data ?? [])
          ];

          this.cargando = false;

          this.actualizarVista();
        },

        error: error => {

          console.error(
            'Error cargando usuarios:',
            error
          );

          if (error.status === 401) {

            this.error =
              'La sesión ha caducado.';

          } else if (error.status === 403) {

            this.error =
              'No tiene permisos para administrar usuarios.';

          } else {

            this.error =
              'No se pudieron cargar los usuarios.';
          }

          this.usuarios = [];

          this.cargando = false;

          this.actualizarVista();
        }
      });
  }

  actualizarListado(): void {
    this.cargarUsuarios();
  }

  nuevoUsuario(): void {

    this.router.navigate([
      '/administracion/usuarios/nuevo'
    ]);
  }

  editarUsuario(
    usuarioId: number
  ): void {

    if (
      !Number.isInteger(usuarioId)
      || usuarioId <= 0
    ) {
      return;
    }

    this.router.navigate([
      '/administracion/usuarios/editar',
      usuarioId
    ]);
  }

  trackByUsuario(
    _indice: number,
    usuario: UsuarioAdministracion
  ): number {

    return usuario.usuarioId;
  }

  private actualizarVista(): void {

    if (!this.componenteDestruido) {
      this.changeDetectorRef.detectChanges();
    }
  }
}
