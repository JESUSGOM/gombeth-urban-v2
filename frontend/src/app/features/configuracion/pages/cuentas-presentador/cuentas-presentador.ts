import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  FormsModule
} from '@angular/forms';

import {
  finalize,
  timeout
} from 'rxjs';

import {
  CuentaPresentador,
  CuentaPresentadorRequest
} from '../../../../core/models/cuenta-presentador.model';

import {
  CuentaPresentadorService
} from '../../../../core/services/cuenta-presentador.service';

@Component({
  selector: 'app-cuentas-presentador',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './cuentas-presentador.html',
  styleUrl: './cuentas-presentador.scss'
})
export class CuentasPresentador
  implements OnInit {

  private readonly service =
    inject(CuentaPresentadorService);

  private readonly changeDetector =
    inject(ChangeDetectorRef);

  cuentas: CuentaPresentador[] = [];

  cargando = false;
  guardando = false;

  eliminandoId: number | null = null;
  actualizandoEstadoId: number | null = null;

  error = '';
  mensaje = '';

  formularioAbierto = false;
  cuentaEditandoId: number | null = null;

  formulario: CuentaPresentadorRequest =
    this.crearFormularioVacio();

  ngOnInit(): void {
    this.cargarCuentas();
  }

  cargarCuentas(): void {
    this.cargando = true;
    this.error = '';

    this.actualizarVista();

    this.service
      .listar()
      .pipe(
        timeout(15000),

        finalize(() => {
          this.cargando = false;
          this.actualizarVista();
        })
      )
      .subscribe({
        next: cuentas => {
          this.cuentas = [
            ...(cuentas ?? [])
          ].sort((a, b) =>
            String(a.alias ?? '')
              .localeCompare(
                String(b.alias ?? ''),
                'es',
                {
                  sensitivity: 'base'
                }
              )
          );

          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error cargando cuentas presentadoras:',
            error
          );

          this.cuentas = [];

          this.error = this.obtenerMensajeError(
            error,
            'No se pudieron cargar las cuentas presentadoras.'
          );

          this.actualizarVista();
        }
      });
  }

  nuevaCuenta(): void {
    this.error = '';
    this.mensaje = '';

    this.cuentaEditandoId = null;
    this.formulario =
      this.crearFormularioVacio();

    this.formularioAbierto = true;
    this.actualizarVista();
  }

  editarCuenta(
    cuenta: CuentaPresentador
  ): void {
    this.error = '';
    this.mensaje = '';

    this.cuentaEditandoId = cuenta.id;

    this.formulario = {
      alias: cuenta.alias ?? '',
      banco: cuenta.banco,
      identificadorPresentador:
        cuenta.identificadorPresentador ?? '',
      nifCif: cuenta.nifCif,
      sufijo: cuenta.sufijo,
      iban: cuenta.iban,
      bic: cuenta.bic,
      activa: cuenta.activa,
      observaciones: cuenta.observaciones
    };

    this.formularioAbierto = true;
    this.actualizarVista();
  }

  cerrarFormulario(): void {
    if (this.guardando) {
      return;
    }

    this.formularioAbierto = false;
    this.cuentaEditandoId = null;

    this.formulario =
      this.crearFormularioVacio();

    this.actualizarVista();
  }

  guardar(): void {
    this.error = '';
    this.mensaje = '';

    const alias =
      this.normalizarTexto(
        this.formulario.alias
      );

    if (!alias) {
      this.error =
        'El alias de la cuenta presentadora es obligatorio.';

      this.actualizarVista();
      return;
    }

    const identificadorPresentador =
      this.normalizarCodigo(
        this.formulario
          .identificadorPresentador
      );

    if (!identificadorPresentador) {
      this.error =
        'El identificador SEPA del presentador es obligatorio.';

      this.actualizarVista();
      return;
    }

    const sufijo =
      this.normalizarCodigo(
        this.formulario.sufijo
      );

    if (
      sufijo !== null
      && sufijo.length !== 3
    ) {
      this.error =
        'El sufijo debe contener exactamente 3 caracteres.';

      this.actualizarVista();
      return;
    }

    const bic =
      this.normalizarCodigo(
        this.formulario.bic
      );

    if (
      bic !== null
      && bic.length !== 8
      && bic.length !== 11
    ) {
      this.error =
        'El BIC debe contener 8 u 11 caracteres.';

      this.actualizarVista();
      return;
    }

    const iban =
      this.normalizarCodigo(
        this.formulario.iban
      );

    if (
      iban !== null
      && iban.length > 34
    ) {
      this.error =
        'El IBAN no puede superar 34 caracteres.';

      this.actualizarVista();
      return;
    }

    const request: CuentaPresentadorRequest = {
      alias,
      banco:
        this.normalizarTexto(
          this.formulario.banco
        ),
      identificadorPresentador,
      nifCif:
        this.normalizarCodigo(
          this.formulario.nifCif
        ),
      sufijo,
      iban,
      bic,
      activa:
        Boolean(this.formulario.activa),
      observaciones:
        this.normalizarObservaciones(
          this.formulario.observaciones
        )
    };

    this.guardando = true;
    this.actualizarVista();

    const operacion =
      this.cuentaEditandoId === null
        ? this.service.crear(request)
        : this.service.actualizar(
            this.cuentaEditandoId,
            request
          );

    operacion
      .pipe(
        timeout(15000),

        finalize(() => {
          this.guardando = false;
          this.actualizarVista();
        })
      )
      .subscribe({
        next: cuentaGuardada => {
          const estabaEditando =
            this.cuentaEditandoId !== null;

          this.formularioAbierto = false;
          this.cuentaEditandoId = null;

          this.formulario =
            this.crearFormularioVacio();

          this.mensaje = estabaEditando
            ? `Cuenta "${cuentaGuardada.alias}" actualizada correctamente.`
            : `Cuenta "${cuentaGuardada.alias}" creada correctamente.`;

          this.cargarCuentas();
          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error guardando cuenta presentadora:',
            error
          );

          this.error = this.obtenerMensajeError(
            error,
            'No se pudo guardar la cuenta presentadora.'
          );

          this.actualizarVista();
        }
      });
  }

  cambiarEstado(
    cuenta: CuentaPresentador
  ): void {
    if (
      this.actualizandoEstadoId !== null
      || this.eliminandoId !== null
    ) {
      return;
    }

    this.error = '';
    this.mensaje = '';

    this.actualizandoEstadoId = cuenta.id;
    this.actualizarVista();

    const request: CuentaPresentadorRequest = {
      alias: cuenta.alias,
      banco: cuenta.banco,
      identificadorPresentador:
        cuenta.identificadorPresentador,
      nifCif: cuenta.nifCif,
      sufijo: cuenta.sufijo,
      iban: cuenta.iban,
      bic: cuenta.bic,
      activa: !cuenta.activa,
      observaciones: cuenta.observaciones
    };

    this.service
      .actualizar(
        cuenta.id,
        request
      )
      .pipe(
        timeout(15000),

        finalize(() => {
          this.actualizandoEstadoId = null;
          this.actualizarVista();
        })
      )
      .subscribe({
        next: cuentaActualizada => {
          this.mensaje =
            cuentaActualizada.activa
              ? `Cuenta "${cuentaActualizada.alias}" activada correctamente.`
              : `Cuenta "${cuentaActualizada.alias}" desactivada correctamente.`;

          this.cargarCuentas();
          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error cambiando estado de la cuenta:',
            error
          );

          this.error = this.obtenerMensajeError(
            error,
            'No se pudo cambiar el estado de la cuenta presentadora.'
          );

          this.actualizarVista();
        }
      });
  }

  eliminarCuenta(
    cuenta: CuentaPresentador
  ): void {
    if (
      this.eliminandoId !== null
      || this.actualizandoEstadoId !== null
    ) {
      return;
    }

    const confirmado = confirm(
      `¿Desea eliminar la cuenta presentadora "${cuenta.alias}"?`
    );

    if (!confirmado) {
      return;
    }

    this.error = '';
    this.mensaje = '';

    this.eliminandoId = cuenta.id;
    this.actualizarVista();

    this.service
      .eliminar(cuenta.id)
      .pipe(
        timeout(15000),

        finalize(() => {
          this.eliminandoId = null;
          this.actualizarVista();
        })
      )
      .subscribe({
        next: () => {
          this.mensaje =
            `Cuenta "${cuenta.alias}" eliminada correctamente.`;

          this.cargarCuentas();
          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error eliminando cuenta presentadora:',
            error
          );

          this.error = this.obtenerMensajeError(
            error,
            'No se pudo eliminar la cuenta presentadora.'
          );

          this.actualizarVista();
        }
      });
  }

  trackById(
    _indice: number,
    cuenta: CuentaPresentador
  ): number {
    return cuenta.id;
  }

  get tituloFormulario(): string {
    return this.cuentaEditandoId === null
      ? 'Nueva cuenta presentadora'
      : 'Editar cuenta presentadora';
  }

  private crearFormularioVacio():
    CuentaPresentadorRequest {

    return {
      alias: '',
      banco: null,
      identificadorPresentador: '',
      nifCif: null,
      sufijo: '000',
      iban: null,
      bic: null,
      activa: true,
      observaciones: null
    };
  }

  private normalizarTexto(
    valor: string | null | undefined
  ): string | null {

    const limpio =
      String(valor ?? '')
        .trim()
        .replace(/\s+/g, ' ');

    return limpio
      ? limpio
      : null;
  }

  private normalizarCodigo(
    valor: string | null | undefined
  ): string | null {

    const limpio =
      String(valor ?? '')
        .replace(/\s+/g, '')
        .trim()
        .toUpperCase();

    return limpio
      ? limpio
      : null;
  }

  private normalizarObservaciones(
    valor: string | null | undefined
  ): string | null {

    const limpio =
      String(valor ?? '').trim();

    return limpio
      ? limpio
      : null;
  }

  private obtenerMensajeError(
    error: any,
    mensajePredeterminado: string
  ): string {

    if (error?.name === 'TimeoutError') {
      return 'El backend no respondió en 15 segundos.';
    }

    const mensajeBackend =
      error?.error?.message
      || error?.error?.detail
      || error?.error?.error;

    return mensajeBackend
      ? String(mensajeBackend)
      : mensajePredeterminado;
  }

  private actualizarVista(): void {
    this.changeDetector.markForCheck();
  }
}