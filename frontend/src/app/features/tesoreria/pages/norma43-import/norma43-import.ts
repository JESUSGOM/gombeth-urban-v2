import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription, finalize } from 'rxjs';

import {
  Norma43MovimientoPreview,
  Norma43Previsualizacion
} from '../../../../core/models/norma43.model';
import { Norma43Service } from '../../../../core/services/norma43.service';
import {
  ComunidadSeleccionada,
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

@Component({
  selector: 'app-norma43-import',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './norma43-import.html',
  styleUrl: './norma43-import.scss'
})
export class Norma43Import implements OnInit, OnDestroy {

  private readonly norma43Service = inject(Norma43Service);
  private readonly comunidadState = inject(ComunidadStateService);
  private readonly router = inject(Router);

  private readonly subscriptions = new Subscription();

  comunidad: ComunidadSeleccionada | null = null;
  ficheroSeleccionado: File | null = null;
  previsualizacion: Norma43Previsualizacion | null = null;

  cargandoPrevisualizacion = false;
  importando = false;
  importacionConfirmada = false;

  mensajeError = '';
  mensajeExito = '';

  paginaActual = 1;
  elementosPorPagina = 10;

  ngOnInit(): void {
    this.comunidadState.init();

    this.subscriptions.add(
      this.comunidadState.comunidad$.subscribe(comunidad => {
        const cambioComunidad =
          this.comunidad?.id !== comunidad?.id;

        this.comunidad = comunidad;

        if (cambioComunidad) {
          this.reiniciarFlujo();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  seleccionarFichero(evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const fichero = input.files?.item(0) ?? null;

    this.ficheroSeleccionado = fichero;
    this.previsualizacion = null;
    this.importacionConfirmada = false;
    this.mensajeError = '';
    this.mensajeExito = '';
    this.paginaActual = 1;

    if (fichero && fichero.size > 10 * 1024 * 1024) {
      this.mensajeError =
        'El fichero supera el tamaño máximo de 10 MB.';

      this.ficheroSeleccionado = null;
      input.value = '';
    }
  }

  previsualizar(): void {
    if (!this.comunidad) {
      this.mensajeError =
        'Debe seleccionar una comunidad antes de analizar el extracto.';
      return;
    }

    if (!this.ficheroSeleccionado) {
      this.mensajeError =
        'Debe seleccionar un fichero Norma 43.';
      return;
    }

    this.cargandoPrevisualizacion = true;
    this.previsualizacion = null;
    this.importacionConfirmada = false;
    this.mensajeError = '';
    this.mensajeExito = '';

    this.norma43Service
      .previsualizar(
        this.comunidad.id,
        this.ficheroSeleccionado
      )
      .pipe(
        finalize(() => {
          this.cargandoPrevisualizacion = false;
        })
      )
      .subscribe({
        next: resultado => {
          this.previsualizacion = resultado;
          this.paginaActual = 1;

          if (resultado.numeroMovimientos === 0) {
            this.mensajeError =
              'El fichero no contiene movimientos Norma 43 reconocibles.';
          }
        },
        error: error => {
          this.mensajeError =
            this.obtenerMensajeError(error);
        }
      });
  }

  confirmarImportacion(): void {
    if (
      !this.comunidad ||
      !this.ficheroSeleccionado ||
      !this.previsualizacion ||
      this.previsualizacion.numeroMovimientos === 0 ||
      this.importacionConfirmada
    ) {
      return;
    }

    const confirmado = window.confirm(
      `Se importarán ${this.previsualizacion.numeroMovimientos} ` +
      `movimientos en la comunidad "${this.comunidad.nombre}". ` +
      'La conciliación no se ejecutará automáticamente. ¿Continuar?'
    );

    if (!confirmado) {
      return;
    }

    this.importando = true;
    this.mensajeError = '';
    this.mensajeExito = '';

    this.norma43Service
      .importar(
        this.comunidad.id,
        this.ficheroSeleccionado
      )
      .pipe(
        finalize(() => {
          this.importando = false;
        })
      )
      .subscribe({
        next: movimientos => {
          this.importacionConfirmada = true;

          this.mensajeExito =
            movimientos.length === 0
              ? 'No se añadió ningún movimiento porque todos ya existían.'
              : `Importación terminada: ${movimientos.length} movimientos nuevos.`;
        },
        error: error => {
          this.mensajeError =
            this.obtenerMensajeError(error);
        }
      });
  }

  limpiar(): void {
    this.reiniciarFlujo();
  }

  verMovimientos(): void {
    void this.router.navigate([
      '/tesoreria/movimientos'
    ]);
  }

  cambiarPagina(pagina: number): void {
    if (
      pagina < 1 ||
      pagina > this.totalPaginas
    ) {
      return;
    }

    this.paginaActual = pagina;
  }

  cambiarElementosPorPagina(): void {
    this.paginaActual = 1;
  }

  get movimientosPaginados(): Norma43MovimientoPreview[] {
    const movimientos =
      this.previsualizacion?.movimientos ?? [];

    const inicio =
      (this.paginaActual - 1) *
      this.elementosPorPagina;

    return movimientos.slice(
      inicio,
      inicio + this.elementosPorPagina
    );
  }

  get totalPaginas(): number {
    const total =
      this.previsualizacion?.numeroMovimientos ?? 0;

    return Math.max(
      1,
      Math.ceil(
        total / this.elementosPorPagina
      )
    );
  }

  get puedePrevisualizar(): boolean {
    return Boolean(
      this.comunidad &&
      this.ficheroSeleccionado &&
      !this.cargandoPrevisualizacion &&
      !this.importando
    );
  }

  get puedeImportar(): boolean {
    return Boolean(
      this.previsualizacion &&
      this.previsualizacion.numeroMovimientos > 0 &&
      !this.importando &&
      !this.cargandoPrevisualizacion &&
      !this.importacionConfirmada
    );
  }

  private reiniciarFlujo(): void {
    this.ficheroSeleccionado = null;
    this.previsualizacion = null;
    this.cargandoPrevisualizacion = false;
    this.importando = false;
    this.importacionConfirmada = false;
    this.mensajeError = '';
    this.mensajeExito = '';
    this.paginaActual = 1;
  }

  private obtenerMensajeError(
    error: unknown
  ): string {
    if (!(error instanceof HttpErrorResponse)) {
      return 'No se pudo completar la operación Norma 43.';
    }

    const cuerpo = error.error;

    if (
      typeof cuerpo === 'string' &&
      cuerpo.trim()
    ) {
      return cuerpo.trim();
    }

    if (
      cuerpo &&
      typeof cuerpo === 'object'
    ) {
      const detalle =
        cuerpo.detail ??
        cuerpo.message ??
        cuerpo.error;

      if (
        typeof detalle === 'string' &&
        detalle.trim()
      ) {
        return detalle.trim();
      }
    }

    if (error.status === 403) {
      return 'No tiene acceso a la comunidad seleccionada.';
    }

    return error.message ||
      'No se pudo completar la operación Norma 43.';
  }
}
