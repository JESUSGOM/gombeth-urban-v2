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
  FormsModule
} from '@angular/forms';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  forkJoin,
  Subject
} from 'rxjs';

import {
  takeUntil
} from 'rxjs/operators';

import {
  ComunidadAdministracion,
  RolAdministracion,
  UsuarioAdministracion,
  UsuariosAdministracionService
} from '../../services/usuarios-administracion.service';

@Component({
  selector: 'app-usuarios-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  template: `
    <div class="usuarios-edit-page">

      <div class="page-header">

        <div>

          <h2>
            {{
              modoEdicion
                ? 'Editar usuario'
                : 'Nuevo usuario'
            }}
          </h2>

          <p>
            Administración de roles y comunidades compartidas.
          </p>

        </div>

        <button
          type="button"
          (click)="volver()">
          Volver
        </button>

      </div>

      <div
        *ngIf="cargando"
        class="estado">

        Cargando información...

      </div>

      <div
        *ngIf="error"
        class="estado error">

        {{ error }}

      </div>

      <div
        *ngIf="errorFormulario"
        class="estado error">

        {{ errorFormulario }}

      </div>

      <div
        *ngIf="
          !cargando
          && !error
        "
        class="contenido-grid">

        <!-- DATOS DEL USUARIO -->
        <section class="panel">

          <h3>
            Datos del usuario
          </h3>

          <label class="campo">

            <span>
              Usuario
            </span>

            <input
              type="text"
              [(ngModel)]="usernameFormulario"
              autocomplete="off">

          </label>

          <label
            *ngIf="!modoEdicion"
            class="campo">

            <span>
              Contraseña inicial
            </span>

            <input
              type="password"
              [(ngModel)]="passwordInicial"
              autocomplete="new-password">

          </label>

          <p
            *ngIf="
              modoEdicion
              && usuario
            "
            class="administrador-info">

            Administrador:

            <strong>
              {{ usuario.administrador.nombre }}
            </strong>

          </p>

        </section>

        <!-- ROLES -->
        <section class="panel">

          <h3>
            Roles
          </h3>

          <p class="contador">

            Roles disponibles:

            <strong>
              {{ roles.length }}
            </strong>

          </p>

          <div
            *ngIf="roles.length === 0"
            class="ayuda">

            No existen roles configurados.

          </div>

          <div
            *ngIf="roles.length > 0"
            class="opciones-lista">

            <label
              *ngFor="let rol of roles"
              class="opcion">

              <input
                type="checkbox"
                [checked]="
                  rolSeleccionado(
                    rol.rolId
                  )
                "
                (change)="
                  cambiarRol(
                    rol.rolId,
                    $event
                  )
                ">

              <span>
                {{ rol.nombre }}
              </span>

            </label>

          </div>

        </section>

        <!-- COMUNIDADES COMPARTIDAS -->
        <section
          class="
            panel
            panel-comunidades
          ">

          <h3>
            Comunidades compartidas
          </h3>

          <p class="ayuda">

            Estas son las comunidades que pueden asignarse
            adicionalmente al usuario mediante
            <strong>usuario_comunidades</strong>.

          </p>

          <p class="contador">

            Comunidades disponibles:

            <strong>
              {{ comunidades.length }}
            </strong>

          </p>

          <div
            *ngIf="comunidades.length === 0"
            class="ayuda">

            No existen comunidades disponibles.

          </div>

          <div
            *ngIf="comunidades.length > 0"
            class="opciones-lista">

            <label
              *ngFor="
                let comunidad
                of comunidades
              "
              class="opcion">

              <input
                type="checkbox"
                [checked]="
                  comunidadCompartidaSeleccionada(
                    comunidad.comunidadId
                  )
                "
                (change)="
                  cambiarComunidadCompartida(
                    comunidad.comunidadId,
                    $event
                  )
                ">

              <span>
                {{ comunidad.nombreComunidad }}
              </span>

            </label>

          </div>

        </section>

        <!-- COMUNIDADES DIRECTAS -->
        <section
          *ngIf="
            modoEdicion
            && usuario
          "
          class="
            panel
            panel-comunidades
          ">

          <h3>
            Comunidades directas
          </h3>

          <p class="ayuda">

            Estas comunidades proceden de la asignación
            directa del usuario en la comunidad.

            No se modificarán desde la gestión de
            comunidades compartidas.

          </p>

          <p class="contador">

            Comunidades directas:

            <strong>
              {{
                usuario.comunidadesDirectas.length
              }}
            </strong>

          </p>

          <div
            *ngIf="
              usuario.comunidadesDirectas.length === 0
            "
            class="ayuda">

            Ninguna.

          </div>

          <ul
            *ngIf="
              usuario.comunidadesDirectas.length > 0
            "
            class="comunidades-directas">

            <li
              *ngFor="
                let comunidad
                of usuario.comunidadesDirectas
              ">

              {{ comunidad.nombreComunidad }}

            </li>

          </ul>

        </section>

        <!-- COMPROBACIÓN LOCAL -->
        <div class="modo-comprobacion">

          <p>

            <strong>
              Edición local de comprobación:
            </strong>

            todavía no se enviará ningún cambio
            al servidor.

          </p>

          <button
            type="button"
            (click)="validarFormulario()">

            Comprobar datos

          </button>

        </div>

      </div>

    </div>
  `,
  styleUrl: './usuarios-edit.scss'
})
export class UsuariosEdit
  implements OnInit, OnDestroy {

  private readonly usuariosService =
    inject(UsuariosAdministracionService);

  private readonly route =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly changeDetectorRef =
    inject(ChangeDetectorRef);

  private readonly destruir$ =
    new Subject<void>();

  private componenteDestruido = false;

  modoEdicion = false;

  usuarioId: number | null = null;

  usuario: UsuarioAdministracion | null =
    null;

  roles: RolAdministracion[] = [];

  comunidades: ComunidadAdministracion[] =
    [];

  usernameFormulario = '';

  passwordInicial = '';

  rolIdsSeleccionados =
    new Set<number>();

  comunidadCompartidaIdsSeleccionados =
    new Set<number>();

  cargando = false;

  error = '';

  errorFormulario = '';

  ngOnInit(): void {

    const idParametro =
      this.route.snapshot.paramMap.get('id');

    if (idParametro !== null) {

      const id =
        Number(idParametro);

      if (
        !Number.isInteger(id)
        || id <= 0
      ) {

        this.error =
          'El identificador del usuario no es válido.';

        return;
      }

      this.modoEdicion = true;
      this.usuarioId = id;
    }

    this.cargarDatos();
  }

  ngOnDestroy(): void {

    this.componenteDestruido = true;

    this.destruir$.next();
    this.destruir$.complete();
  }

  cargarDatos(): void {

    this.cargando = true;
    this.error = '';
    this.errorFormulario = '';

    if (
      this.modoEdicion
      && this.usuarioId !== null
    ) {

      forkJoin({

        usuario:
          this.usuariosService
            .obtenerUsuario(
              this.usuarioId
            ),

        roles:
          this.usuariosService
            .listarRoles(),

        comunidades:
          this.usuariosService
            .listarComunidades()

      })
        .pipe(
          takeUntil(this.destruir$)
        )
        .subscribe({

          next: resultado => {

            this.usuario =
              resultado.usuario;

            this.roles = [
              ...(resultado.roles ?? [])
            ];

            this.comunidades = [
              ...(resultado.comunidades ?? [])
            ];

            this.inicializarFormularioEdicion(
              resultado.usuario
            );

            this.cargando = false;

            this.actualizarVista();
          },

          error: error => {
            this.procesarError(error);
          }
        });

      return;
    }

    forkJoin({

      roles:
        this.usuariosService
          .listarRoles(),

      comunidades:
        this.usuariosService
          .listarComunidades()

    })
      .pipe(
        takeUntil(this.destruir$)
      )
      .subscribe({

        next: resultado => {

          this.usuario = null;

          this.roles = [
            ...(resultado.roles ?? [])
          ];

          this.comunidades = [
            ...(resultado.comunidades ?? [])
          ];

          this.inicializarFormularioAlta();

          this.cargando = false;

          this.actualizarVista();
        },

        error: error => {
          this.procesarError(error);
        }
      });
  }

  rolSeleccionado(
    rolId: number
  ): boolean {

    return this.rolIdsSeleccionados
      .has(rolId);
  }

  cambiarRol(
    rolId: number,
    event: Event
  ): void {

    const input =
      event.target as HTMLInputElement;

    this.errorFormulario = '';

    if (input.checked) {

      this.rolIdsSeleccionados
        .add(rolId);

    } else {

      this.rolIdsSeleccionados
        .delete(rolId);
    }
  }

  comunidadCompartidaSeleccionada(
    comunidadId: number
  ): boolean {

    return this
      .comunidadCompartidaIdsSeleccionados
      .has(comunidadId);
  }

  cambiarComunidadCompartida(
    comunidadId: number,
    event: Event
  ): void {

    const input =
      event.target as HTMLInputElement;

    this.errorFormulario = '';

    if (input.checked) {

      this
        .comunidadCompartidaIdsSeleccionados
        .add(comunidadId);

    } else {

      this
        .comunidadCompartidaIdsSeleccionados
        .delete(comunidadId);
    }
  }

  validarFormulario(): boolean {

    this.errorFormulario = '';

    const username =
      this.usernameFormulario.trim();

    if (!username) {

      this.errorFormulario =
        'Debe indicar el nombre de usuario.';

      return false;
    }

    if (!this.modoEdicion) {

      const password =
        this.passwordInicial;

      if (!password) {

        this.errorFormulario =
          'Debe indicar la contraseña inicial.';

        return false;
      }

      if (password.length < 8) {

        this.errorFormulario =
          'La contraseña debe tener al menos 8 caracteres.';

        return false;
      }

      if (password.length > 72) {

        this.errorFormulario =
          'La contraseña no puede superar los 72 caracteres.';

        return false;
      }

      if (!/[A-Z]/.test(password)) {

        this.errorFormulario =
          'La contraseña debe contener al menos una letra mayúscula.';

        return false;
      }

      if (!/[a-z]/.test(password)) {

        this.errorFormulario =
          'La contraseña debe contener al menos una letra minúscula.';

        return false;
      }

      if (!/[0-9]/.test(password)) {

        this.errorFormulario =
          'La contraseña debe contener al menos un número.';

        return false;
      }
    }

    return true;
  }

  volver(): void {

    this.router.navigate([
      '/administracion/usuarios'
    ]);
  }

  private inicializarFormularioEdicion(
    usuario: UsuarioAdministracion
  ): void {

    this.usernameFormulario =
      usuario.username;

    this.passwordInicial = '';

    this.errorFormulario = '';

    this.rolIdsSeleccionados =
      new Set<number>(
        usuario.roles.map(
          rol => rol.rolId
        )
      );

    this.comunidadCompartidaIdsSeleccionados =
      new Set<number>(
        usuario.comunidadesCompartidas.map(
          comunidad =>
            comunidad.comunidadId
        )
      );
  }

  private inicializarFormularioAlta(): void {

    this.usernameFormulario = '';

    this.passwordInicial = '';

    this.errorFormulario = '';

    this.rolIdsSeleccionados =
      new Set<number>();

    this.comunidadCompartidaIdsSeleccionados =
      new Set<number>();
  }

  private procesarError(
    error: any
  ): void {

    console.error(
      'Error cargando administración de usuario:',
      error
    );

    this.errorFormulario = '';

    if (error.status === 401) {

      this.error =
        'La sesión ha caducado.';

    } else if (error.status === 403) {

      this.error =
        'No tiene permisos para administrar usuarios.';

    } else if (error.status === 404) {

      this.error =
        'El usuario solicitado no existe o no pertenece a este administrador.';

    } else {

      this.error =
        'No se pudo cargar la información del usuario.';
    }

    this.usuario = null;
    this.roles = [];
    this.comunidades = [];

    this.usernameFormulario = '';
    this.passwordInicial = '';

    this.rolIdsSeleccionados.clear();

    this
      .comunidadCompartidaIdsSeleccionados
      .clear();

    this.cargando = false;

    this.actualizarVista();
  }

  private actualizarVista(): void {

    if (!this.componenteDestruido) {
      this.changeDetectorRef.detectChanges();
    }
  }
}
