import {
  Component,
  OnInit,
  inject,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Presupuesto } from '../../../../core/models/presupuesto.model';
import { RepartoPresupuesto } from '../../../../core/models/reparto-presupuesto.model';
import { CuotaPresupuesto } from '../../../../core/models/cuota-presupuesto.model';
import { Comunidad } from '../../../../core/models/comunidad.model';
import { PresupuestoRevision } from '../../../../core/models/presupuesto-revision.model';
import { CoeficienteVecinoDetalle } from '../../../../core/models/coeficiente-vecino-detalle.model';

import { PresupuestoService } from '../../../../core/services/presupuesto';
import {
  CuentaContable,
  CuentasContablesService
} from '../../../../core/services/cuentas-contables.service';
import { ComunidadService } from '../../../../core/services/comunidad';
import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';

@Component({
  selector: 'app-presupuestos-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './presupuestos-list.html',
  styleUrl: './presupuestos-list.scss'
})
export class PresupuestosList implements OnInit {

  private presupuestoService = inject(PresupuestoService);
  private cuentasContablesService = inject(CuentasContablesService);
  private comunidadService = inject(ComunidadService);
  private comunidadState = inject(ComunidadStateService);
  private cdr = inject(ChangeDetectorRef);

  comunidadId = 0;
  comunidad?: Comunidad;

  anio = 2026;
  mesRecibos = new Date().getMonth() + 1;
  generandoRecibos = false;

  metodoRepartoPredeterminado: 'COEFICIENTE' | 'PARTES_IGUALES' =
    'PARTES_IGUALES';

  presupuestos: Presupuesto[] = [];
  cuentasGasto: CuentaContable[] = [];
  propietarios: CoeficienteVecinoDetalle[] = [];

  partidaEditandoId: number | null = null;
  nuevaCuentaId: number | null = null;
  nuevoImporte: number | null = null;
  nuevoMetodoReparto: 'COEFICIENTE' | 'PARTES_IGUALES' =
    'PARTES_IGUALES';
  nuevoAplicaTodos = true;
  vecinosSeleccionados: number[] = [];
  guardandoPartida = false;
  eliminandoPartidaId: number | null = null;

  reparto: RepartoPresupuesto[] = [];
  cuotasBorrador: CuotaPresupuesto[] = [];
  revisiones: PresupuestoRevision[] = [];

  total = 0;
  error = '';
  mensaje = '';
  cargando = false;
  generandoCuotas = false;

  ngOnInit(): void {
    this.comunidadState.init();

    this.comunidadState.comunidad$
      .subscribe(comunidad => {

        if (!comunidad || !comunidad.id) {
          return;
        }

        if (this.comunidadId === comunidad.id) {
          return;
        }

        this.comunidadId = comunidad.id;

        this.limpiarPantalla();
        this.cargarTodo();
      });
  }

  private limpiarPantalla(): void {
    this.comunidad = undefined;

    this.presupuestos = [];
    this.cuentasGasto = [];

    this.propietarios = [];

    this.partidaEditandoId = null;
    this.nuevaCuentaId = null;
    this.nuevoImporte = null;
    this.nuevoMetodoReparto =
      this.metodoRepartoPredeterminado;
    this.nuevoAplicaTodos = true;
    this.vecinosSeleccionados = [];
    this.guardandoPartida = false;
    this.eliminandoPartidaId = null;

    this.reparto = [];
    this.cuotasBorrador = [];
    this.revisiones = [];

    this.total = 0;
    this.error = '';
    this.mensaje = '';
    this.cargando = false;
  }

  cargarTodo(): void {
    if (this.comunidadId <= 0) {
      return;
    }

    this.cargarComunidad();
    this.cargarPropietarios();
    this.cargarCuentasGasto();
    this.cargarPresupuestos();
  }

  cargarComunidad(): void {
    this.comunidadService
      .getComunidad(this.comunidadId)
      .subscribe({
        next: data => {
          this.comunidad = data;

          switch (data.tipoReparto) {
            case 'COEFICIENTE':
              this.metodoRepartoPredeterminado = 'COEFICIENTE';
              break;

            case 'PARTES_IGUALES':
              this.metodoRepartoPredeterminado = 'PARTES_IGUALES';
              break;

            default:
              this.metodoRepartoPredeterminado = 'PARTES_IGUALES';
              break;
          }

          if (this.partidaEditandoId === null) {
            this.nuevoMetodoReparto =
              this.metodoRepartoPredeterminado;
          }

          this.cdr.detectChanges();
        },

        error: err => {
          console.error(
            'Error cargando comunidad:',
            err
          );
        }
      });
  }

  cargarPropietarios(): void {
    this.comunidadService
      .getDetalleCoeficientes(this.comunidadId)
      .subscribe({
        next: data => {
          this.propietarios = (data ?? [])
            .filter(propietario => propietario.activo)
            .sort((a, b) =>
              (a.vivienda || '').localeCompare(
                b.vivienda || '',
                'es',
                { numeric: true }
              ) ||
              (a.nombre || '').localeCompare(
                b.nombre || '',
                'es'
              )
            );

          this.cdr.detectChanges();
        },

        error: err => {
          console.error(
            'Error cargando propietarios del presupuesto:',
            err
          );

          this.propietarios = [];
          this.error = this.obtenerMensajeError(
            err,
            'No se pudieron cargar los propietarios de la comunidad.'
          );

          this.cdr.detectChanges();
        }
      });
  }

  cargarCuentasGasto(): void {
    this.cuentasContablesService
      .listarPorComunidad(this.comunidadId)
      .subscribe({
        next: data => {
          this.cuentasGasto = data
            .filter(cuenta =>
              cuenta.tipo === 'GASTO' ||
              cuenta.codigo === '10200000'
            )
            .sort((cuentaA, cuentaB) =>
              cuentaA.codigo.localeCompare(
                cuentaB.codigo,
                'es',
                {
                  numeric: true
                }
              )
            );

          const cuentaSeleccionadaExiste =
            this.cuentasGasto.some(
              cuenta =>
                cuenta.id === this.nuevaCuentaId
            );

          if (!cuentaSeleccionadaExiste) {
            this.nuevaCuentaId =
              this.cuentasGasto.length > 0
                ? this.cuentasGasto[0].id
                : null;
          }

          this.cdr.detectChanges();
        },

        error: err => {
          console.error(
            'Error cargando cuentas de gasto:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudieron cargar las cuentas presupuestarias.'
          );

          this.cdr.detectChanges();
        }
      });
  }

  guardarPartida(): void {
    if (this.comunidadId <= 0) {
      return;
    }

    if (!this.nuevaCuentaId) {
      this.error =
        'Debe seleccionar una cuenta presupuestaria.';

      this.mensaje = '';
      return;
    }

    if (
      this.nuevoImporte === null ||
      Number(this.nuevoImporte) <= 0
    ) {
      this.error =
        'El importe debe ser mayor que cero.';

      this.mensaje = '';
      return;
    }

    if (
      !this.nuevoAplicaTodos &&
      this.vecinosSeleccionados.length === 0
    ) {
      this.error =
        'Debe seleccionar al menos un propietario afectado.';

      this.mensaje = '';
      return;
    }

    if (this.partidaEditandoId === null) {
      const partidaDuplicada =
        this.presupuestos.find(
          presupuesto =>
            presupuesto.cuentaId === this.nuevaCuentaId &&
            presupuesto.anio === this.anio
        );

      if (partidaDuplicada) {
        const cuenta =
          this.cuentasGasto.find(
            elemento =>
              elemento.id === this.nuevaCuentaId
          );

        const descripcionCuenta =
          cuenta?.nombre ||
          partidaDuplicada.cuentaDescripcion ||
          'esa cuenta';

        this.error =
          `Ya existe una partida para ${descripcionCuenta} ` +
          `en ${this.anio}. Utilice Editar para modificarla.`;

        this.mensaje = '';
        return;
      }
    }

    const request = {
      cuentaId: this.nuevaCuentaId,
      anio: this.anio,
      importe: Number(this.nuevoImporte),
      metodoReparto: this.nuevoMetodoReparto,
      aplicaTodos: this.nuevoAplicaTodos,
      vecinoIds: this.nuevoAplicaTodos
        ? []
        : [...this.vecinosSeleccionados]
    };

    this.guardandoPartida = true;
    this.error = '';
    this.mensaje = '';

    const operacion =
      this.partidaEditandoId === null
        ? this.presupuestoService.crearPartida(
          this.comunidadId,
          request
        )
        : this.presupuestoService.modificarPartida(
          this.partidaEditandoId,
          request
        );

    operacion.subscribe({
      next: () => {
        const editando =
          this.partidaEditandoId !== null;

        this.guardandoPartida = false;
        this.resetFormularioPartida();

        this.mensaje = editando
          ? 'Partida presupuestaria modificada correctamente.'
          : 'Partida presupuestaria creada correctamente.';

        this.cargarPresupuestos(false);
      },

      error: err => {
        console.error(
          'Error guardando partida presupuestaria:',
          err
        );

        this.error = this.obtenerMensajeError(
          err,
          'No se pudo guardar la partida presupuestaria.'
        );

        this.guardandoPartida = false;
        this.cdr.detectChanges();
      }
    });
  }

  editarPartida(
    presupuesto: Presupuesto
  ): void {
    this.partidaEditandoId =
      presupuesto.id;

    this.nuevaCuentaId =
      presupuesto.cuentaId;

    this.nuevoImporte =
      Number(presupuesto.importe);

    this.nuevoMetodoReparto =
      presupuesto.metodoReparto === 'COEFICIENTE'
        ? 'COEFICIENTE'
        : 'PARTES_IGUALES';

    this.nuevoAplicaTodos =
      presupuesto.aplicaTodos !== false;

    this.vecinosSeleccionados =
      this.nuevoAplicaTodos
        ? []
        : [...(presupuesto.vecinoIds ?? [])];

    this.error = '';
    this.mensaje = '';
  }

  cancelarEdicionPartida(): void {
    this.resetFormularioPartida();
    this.error = '';
    this.mensaje = '';
  }

  eliminarPartida(
    presupuesto: Presupuesto
  ): void {
    const descripcion =
      `${presupuesto.cuentaCodigo} — ` +
      `${presupuesto.cuentaDescripcion}`;

    if (
      !confirm(
        `¿Desea eliminar la partida ${descripcion}?\n\n` +
        'Si existen cuotas en BORRADOR para este año, ' +
        'se eliminarán porque deben recalcularse con el ' +
        'nuevo presupuesto.'
      )
    ) {
      return;
    }

    this.eliminandoPartidaId =
      presupuesto.id;

    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .eliminarPartida(
        presupuesto.id
      )
      .subscribe({
        next: () => {
          this.eliminandoPartidaId = null;

          if (
            this.partidaEditandoId ===
            presupuesto.id
          ) {
            this.resetFormularioPartida();
          }

          this.mensaje =
            `Partida ${descripcion} eliminada correctamente. ` +
            'Si había cuotas en BORRADOR, se han eliminado. ' +
            'Genere un nuevo borrador de cuotas cuando el ' +
            'presupuesto esté definitivo.';

          this.cargarPresupuestos(false);
        },

        error: err => {
          console.error(
            'Error eliminando partida presupuestaria:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudo eliminar la partida presupuestaria.'
          );

          this.eliminandoPartidaId = null;
          this.cdr.detectChanges();
        }
      });
  }

  cambiarAlcancePartida(): void {
    if (this.nuevoAplicaTodos) {
      this.vecinosSeleccionados = [];
    }
  }

  estaPropietarioSeleccionado(
    vecinoId: number
  ): boolean {
    return this.vecinosSeleccionados
      .includes(vecinoId);
  }

  cambiarPropietarioSeleccionado(
    vecinoId: number,
    event: Event
  ): void {
    const marcado =
      (event.target as HTMLInputElement)
        .checked;

    if (marcado) {
      if (
        !this.vecinosSeleccionados
          .includes(vecinoId)
      ) {
        this.vecinosSeleccionados = [
          ...this.vecinosSeleccionados,
          vecinoId
        ];
      }
      return;
    }

    this.vecinosSeleccionados =
      this.vecinosSeleccionados
        .filter(id => id !== vecinoId);
  }

  seleccionarTodosPropietarios(): void {
    this.vecinosSeleccionados =
      this.propietarios.map(
        propietario => propietario.vecinoId
      );
  }

  limpiarPropietariosSeleccionados(): void {
    this.vecinosSeleccionados = [];
  }

  get totalCoeficienteSeleccionado(): number {
    if (this.nuevoAplicaTodos) {
      return this.propietarios.reduce(
        (total, propietario) =>
          total + Number(propietario.coeficiente || 0),
        0
      );
    }

    return this.propietarios
      .filter(propietario =>
        this.vecinosSeleccionados
          .includes(propietario.vecinoId)
      )
      .reduce(
        (total, propietario) =>
          total + Number(propietario.coeficiente || 0),
        0
      );
  }

  private resetFormularioPartida(): void {
    this.partidaEditandoId = null;
    this.nuevoImporte = null;
    this.nuevoMetodoReparto =
      this.metodoRepartoPredeterminado;
    this.nuevoAplicaTodos = true;
    this.vecinosSeleccionados = [];

    if (this.cuentasGasto.length > 0) {
      this.nuevaCuentaId =
        this.cuentasGasto[0].id;
    }
  }

  cargarPresupuestos(
    limpiarMensajes = true
  ): void {
    if (this.comunidadId <= 0) {
      return;
    }

    this.cargando = true;

    if (limpiarMensajes) {
      this.error = '';
      this.mensaje = '';
    }

    this.presupuestoService
      .getPresupuestosComunidad(
        this.comunidadId,
        this.anio
      )
      .subscribe({
        next: data => {
          this.presupuestos = data;

          this.total = data.reduce(
            (
              suma,
              presupuesto
            ) =>
              suma +
              Number(presupuesto.importe || 0),
            0
          );

          this.cargarRevisiones();
          this.cargarReparto();
        },

        error: err => {
          console.error(
            'Error cargando presupuesto:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudo cargar el presupuesto.'
          );

          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarRevisiones(): void {
    this.presupuestoService
      .getRevisiones(
        this.comunidadId,
        this.anio
      )
      .subscribe({
        next: data => {
          this.revisiones = data;
          this.cdr.detectChanges();
        },

        error: err => {
          console.error(
            'Error cargando revisiones:',
            err
          );
        }
      });
  }

  cargarReparto(): void {
    this.presupuestoService
      .getRepartoComunidad(
        this.comunidadId,
        this.anio
      )
      .subscribe({
        next: data => {
          this.reparto = data;
          this.cargarCuotasBorrador();
        },

        error: err => {
          console.error(
            'Error cargando reparto:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudo cargar el reparto.'
          );

          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarCuotasBorrador(): void {
    this.presupuestoService
      .getCuotasBorrador(
        this.comunidadId,
        this.anio
      )
      .subscribe({
        next: data => {
          this.cuotasBorrador = data;
          this.cargando = false;
          this.cdr.detectChanges();
        },

        error: err => {
          console.error(
            'Error cargando cuotas:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudieron cargar las cuotas.'
          );

          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  generarBorradorCuotas(): void {
    if (this.comunidadId <= 0) {
      return;
    }

    this.generandoCuotas = true;
    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .generarBorradorCuotas(
        this.comunidadId,
        this.anio
      )
      .subscribe({
        next: data => {
          this.mensaje =
            data?.mensaje ||
            'Borrador de cuotas generado correctamente.';

          this.generandoCuotas = false;

          this.cargarCuotasBorrador();
          this.cargarRevisiones();
        },

        error: err => {
          console.error(
            'Error generando borrador de cuotas:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudo generar el borrador de cuotas.'
          );

          this.generandoCuotas = false;
          this.cdr.detectChanges();
        }
      });
  }

  aprobarCuotas(): void {
    if (this.comunidadId <= 0) {
      return;
    }

    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .aprobarCuotas(
        this.comunidadId,
        this.anio
      )
      .subscribe({
        next: data => {
          this.mensaje =
            data?.mensaje ||
            'Cuotas aprobadas correctamente.';

          this.cargarCuotasBorrador();
          this.cargarRevisiones();
          this.cdr.detectChanges();
        },

        error: err => {
          console.error(
            'Error aprobando cuotas:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudieron aprobar las cuotas.'
          );

          this.cdr.detectChanges();
        }
      });
  }

  aprobarRevision(
    id: number
  ): void {
    if (
      !confirm(
        '¿Desea aprobar esta revisión presupuestaria?'
      )
    ) {
      return;
    }

    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .aprobarRevision(id)
      .subscribe({
        next: respuesta => {
          this.mensaje =
            respuesta?.mensaje ||
            'Revisión aprobada correctamente.';

          this.cargarPresupuestos(false);
        },

        error: err => {
          console.error(
            'Error aprobando revisión:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudo aprobar la revisión.'
          );

          this.cdr.detectChanges();
        }
      });
  }

  eliminarRevision(
    id: number
  ): void {
    if (
      !confirm(
        '¿Desea eliminar esta revisión presupuestaria?'
      )
    ) {
      return;
    }

    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .eliminarRevision(id)
      .subscribe({
        next: () => {
          this.mensaje =
            'Revisión eliminada correctamente.';

          this.cargarPresupuestos(false);
        },

        error: err => {
          console.error(
            'Error eliminando revisión:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudo eliminar la revisión.'
          );

          this.cdr.detectChanges();
        }
      });
  }

  private obtenerMensajeError(
    err: unknown,
    mensajePredeterminado: string
  ): string {
    const errorHttp = err as {
      error?: unknown;
      message?: unknown;
    };

    const cuerpo = errorHttp?.error;

    if (typeof cuerpo === 'string' && cuerpo.trim()) {
      return cuerpo.trim();
    }

    if (cuerpo && typeof cuerpo === 'object') {
      const objeto =
        cuerpo as Record<string, unknown>;

      const candidatos = [
        objeto['message'],
        objeto['mensaje'],
        objeto['detail']
      ];

      for (const candidato of candidatos) {
        if (
          typeof candidato === 'string' &&
          candidato.trim()
        ) {
          return candidato.trim();
        }
      }
    }

    if (
      typeof errorHttp?.message === 'string' &&
      errorHttp.message.trim() &&
      !errorHttp.message.startsWith(
        'Http failure response for'
      )
    ) {
      return errorHttp.message.trim();
    }

    return mensajePredeterminado;
  }

  tieneCuotasBorrador(): boolean {
    return this.cuotasBorrador.some(
      cuota => cuota.estado === 'BORRADOR'
    );
  }

  tieneCuotasAprobadas(): boolean {
    return this.cuotasBorrador.some(
      cuota => cuota.estado === 'APROBADA'
    );
  }

  generarRecibos(): void {
    if (this.comunidadId <= 0) {
      return;
    }

    if (
      !confirm(
        '¿Desea generar los recibos desde las cuotas aprobadas?'
      )
    ) {
      return;
    }

    this.generandoRecibos = true;
    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .generarRecibos(
        this.comunidadId,
        this.anio,
        this.mesRecibos
      )
      .subscribe({
        next: data => {
          this.mensaje =
            data?.mensaje ||
            'Recibos generados correctamente.';

          this.generandoRecibos = false;

          this.cargarCuotasBorrador();
          this.cdr.detectChanges();
        },

        error: err => {
          console.error(
            'Error generando recibos:',
            err
          );

          this.error = this.obtenerMensajeError(
            err,
            'No se pudieron generar los recibos.'
          );

          this.generandoRecibos = false;
          this.cdr.detectChanges();
        }
      });
  }
}
