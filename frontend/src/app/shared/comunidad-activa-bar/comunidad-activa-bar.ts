import {
  Component,
  DestroyRef,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  ComunidadUsuario,
  UsuarioContextoService
} from '../../core/services/usuario-contexto.service';

import {
  ComunidadSeleccionada,
  ComunidadStateService
} from '../../core/state/comunidad-state.service';

@Component({
  selector: 'app-comunidad-activa-bar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './comunidad-activa-bar.html',
  styleUrl: './comunidad-activa-bar.scss'
})
export class ComunidadActivaBar implements OnInit {

  private usuarioService =
    inject(UsuarioContextoService);

  private comunidadState =
    inject(ComunidadStateService);

  private destroyRef =
    inject(DestroyRef);

  comunidades: ComunidadUsuario[] = [];
  comunidadId: number | null = null;

  ngOnInit(): void {
    /*
     * Recupera la comunidad guardada en localStorage.
     */
    this.comunidadState.init();

    /*
     * Mantiene el combo sincronizado cuando otra pantalla,
     * como Comunidades o Recibos, cambia la comunidad activa.
     */
    this.comunidadState.comunidad$
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(comunidad => {
        this.comunidadId =
          comunidad?.id ?? null;
      });

    const usuario = JSON.parse(
      localStorage.getItem('usuario') || 'null'
    );

    if (!usuario?.usuarioId) {
      this.comunidades = [];
      this.comunidadId = null;
      return;
    }

    this.usuarioService
      .obtenerComunidades(usuario.usuarioId)
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: data => {
          this.comunidades = data ?? [];

          const actual =
            this.comunidadState.getComunidad();

          /*
           * Comprueba que la comunidad almacenada todavía
           * pertenece al usuario.
           */
          const comunidadActualExiste =
            actual
              ? this.comunidades.some(
                comunidad =>
                  comunidad.id === actual.id
              )
              : false;

          if (
            actual &&
            comunidadActualExiste
          ) {
            this.comunidadId = actual.id;
            return;
          }

          /*
           * Si no hay comunidad válida almacenada,
           * selecciona automáticamente la primera.
           */
          if (this.comunidades.length > 0) {
            this.comunidadId =
              this.comunidades[0].id;

            this.seleccionar();
          } else {
            this.comunidadId = null;
          }
        },

        error: error => {
          console.error(
            'Error cargando las comunidades del usuario:',
            error
          );

          this.comunidades = [];
          this.comunidadId = null;
        }
      });
  }

  seleccionar(): void {
    if (!this.comunidadId) {
      return;
    }

    const comunidad =
      this.comunidades.find(
        elemento =>
          elemento.id === this.comunidadId
      );

    if (!comunidad) {
      return;
    }

    const seleccionada: ComunidadSeleccionada = {
      id: comunidad.id,
      nombre: comunidad.nombre
    };

    this.comunidadState.setComunidad(
      seleccionada
    );
  }
}
