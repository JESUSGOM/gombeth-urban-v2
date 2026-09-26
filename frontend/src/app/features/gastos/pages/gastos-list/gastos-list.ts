import {
  Component,
  DestroyRef,
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
  Router
} from '@angular/router';

import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';

import {
  EMPTY,
  catchError,
  switchMap
} from 'rxjs';

import {
  Gasto
} from '../../../../core/models/gasto.model';

import {
  GastoService
} from '../../../../core/services/gasto.service';

import {
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

import {
  GombethDialogService
} from '../../../../shared/gombeth-dialog/gombeth-dialog.service';

type FiltroEstado =
  'TODOS'
  | 'PENDIENTE'
  | 'CONTABILIZADO'
  | 'PAGADO';

@Component({
  selector: 'app-gastos-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './gastos-list.html',
  styleUrl: './gastos-list.scss'
})
export class GastosList implements OnInit {

  private readonly gastoService =
    inject(GastoService);

  private readonly comunidadState =
    inject(ComunidadStateService);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly router =
    inject(Router);

  private readonly gombethDialog =
    inject(GombethDialogService);

  gastos: Gasto[] = [];
  gastosFiltrados: Gasto[] = [];

  comunidadId: number | null = null;
  nombreComunidad = '';

  cargando = false;
  error = '';

  mensajeOperacion = '';
  errorOperacion = '';

  procesandoGastoId: number | null =
    null;

  procesandoPdfGastoId: number | null =
    null;

  readonly tamanioMaximoPdfBytes =
    10 * 1024 * 1024;

  /*
   * IMPORTANTE:
   *
   * Mientras Gombeth Urban V2 comparta la base de datos
   * con la aplicación antigua, las operaciones de pagar
   * y deshacer pago permanecen deshabilitadas también
   * en la interfaz.
   *
   * El backend dispone además de su propio bloqueo.
   */
  readonly pagosHabilitadosEnInterfaz =
    false;

  readonly avisoPagos =
    'El pago y la anulación de gastos desde '
    + 'Gombeth Urban V2 están temporalmente '
    + 'deshabilitados mientras convive con '
    + 'la aplicación anterior.';

  textoBusqueda = '';

  filtroEstado: FiltroEstado =
    'TODOS';

  paginaActual = 1;
  registrosPorPagina = 10;

  ngOnInit(): void {

    this.comunidadState.init();

    this.comunidadState.comunidad$
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        ),

        switchMap(comunidad => {

          if (
            !comunidad
            || !Number.isInteger(
              Number(comunidad.id)
            )
            || Number(comunidad.id) <= 0
          ) {
            this.limpiarPantalla();
            return EMPTY;
          }

          const comunidadId =
            Number(comunidad.id);

          const cambiaComunidad =
            this.comunidadId
            !== comunidadId;

          this.comunidadId =
            comunidadId;

          this.nombreComunidad =
            comunidad.nombre;

          if (cambiaComunidad) {
            this.textoBusqueda = '';
            this.filtroEstado = 'TODOS';
            this.paginaActual = 1;

            this.mensajeOperacion = '';
            this.errorOperacion = '';
            this.procesandoGastoId = null;
            this.procesandoPdfGastoId = null;
          }

          this.cargando = true;
          this.error = '';

          return this.gastoService
            .listarPorComunidad(
              comunidadId
            )
            .pipe(
              catchError(error => {

                console.error(
                  'Error cargando gastos:',
                  error
                );

                if (error?.status === 403) {
                  this.error =
                    'No tiene acceso a los gastos de esta comunidad.';
                } else if (
                  error?.status === 401
                ) {
                  this.error =
                    'La sesión ha caducado. Vuelva a iniciar sesión.';
                } else {
                  this.error =
                    error?.error?.message
                    || error?.error?.detail
                    || 'No se pudieron cargar los gastos.';
                }

                this.gastos = [];
                this.gastosFiltrados = [];
                this.paginaActual = 1;
                this.cargando = false;

                return EMPTY;
              })
            );
        })
      )
      .subscribe(gastos => {

        this.gastos = [
          ...(gastos ?? [])
        ];

        this.paginaActual = 1;

        this.aplicarFiltros();

        this.cargando = false;
      });
  }

  nuevoGasto(): void {

    if (
      this.comunidadId === null
      || this.comunidadId <= 0
    ) {
      this.error =
        'Seleccione una comunidad antes de crear un gasto.';

      return;
    }

    void this.router.navigate([
      '/gastos/nuevo'
    ]);
  }

  editarGasto(
    gasto: Gasto
  ): void {

    if (!this.puedeEditar(gasto)) {
      return;
    }

    void this.router.navigate([
      '/gastos/editar',
      gasto.id
    ]);
  }

  puedeEditar(
    gasto: Gasto
  ): boolean {

    return (
      !this.estaPagado(gasto)
      && !this.estaContabilizado(gasto)
    );
  }

  motivoNoEditable(
    gasto: Gasto
  ): string {

    if (this.estaPagado(gasto)) {
      return (
        'El gasto está pagado. '
        + 'La edición requiere deshacer el pago.'
      );
    }

    if (
      this.estaContabilizado(gasto)
    ) {
      return (
        'El gasto está contabilizado. '
        + 'La edición requiere una reversión contable segura.'
      );
    }

    return '';
  }

  puedeEliminar(
    gasto: Gasto
  ): boolean {

    return (
      !this.estaPagado(gasto)
      && !this.estaContabilizado(gasto)
      && this.procesandoGastoId === null
    );
  }

  motivoNoEliminar(
    gasto: Gasto
  ): string {

    if (this.estaPagado(gasto)) {
      return (
        'El gasto está pagado. '
        + 'Primero debe deshacerse el pago.'
      );
    }

    if (this.estaContabilizado(gasto)) {
      return (
        'El gasto está contabilizado. '
        + 'Primero debe deshacerse su contabilización.'
      );
    }

    if (this.procesandoGastoId !== null) {
      return (
        'Hay otra operación de gasto en curso.'
      );
    }

    return '';
  }

  async eliminarGasto(
    gasto: Gasto
  ): Promise<void> {

    if (!this.puedeEliminar(gasto)) {

      this.errorOperacion =
        this.motivoNoEliminar(
          gasto
        );

      this.mensajeOperacion = '';

      return;
    }

    const gastoId =
      Number(
        gasto.id
      );

    if (
      !Number.isInteger(gastoId)
      || gastoId <= 0
    ) {
      this.errorOperacion =
        'El identificador del gasto no es válido.';

      this.mensajeOperacion = '';

      return;
    }

    const confirmado =
      await this.gombethDialog.confirm(
        '¿Desea eliminar este gasto pendiente? '
        + 'Esta operación eliminará el registro del gasto, '
        + 'pero no eliminará físicamente el PDF de la factura.',
        'Eliminar gasto',
        'Cancelar'
      );

    if (!confirmado) {
      return;
    }

    this.procesandoGastoId =
      gastoId;

    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.gastoService
      .eliminar(
        gastoId
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({

        next: () => {

          this.procesandoGastoId =
            null;

          this.gastos =
            this.gastos.filter(
              gastoActual =>
                Number(
                  gastoActual.id
                ) !== gastoId
            );

          this.aplicarFiltros();

          this.mensajeOperacion =
            'Gasto eliminado correctamente.';

          this.errorOperacion = '';
        },

        error: error => {

          console.error(
            'Error eliminando gasto:',
            error
          );

          this.procesandoGastoId =
            null;

          this.mensajeOperacion = '';

          this.errorOperacion =
            this.obtenerMensajeErrorOperacion(
              error,
              'No se pudo eliminar el gasto.'
            );
        }
      });
  }

  puedePagar(
    gasto: Gasto
  ): boolean {

    return (
      this.pagosHabilitadosEnInterfaz
      && this.estaContabilizado(gasto)
      && !this.estaPagado(gasto)
      && this.procesandoGastoId === null
    );
  }

  motivoNoPagar(
    gasto: Gasto
  ): string {

    if (!this.pagosHabilitadosEnInterfaz) {
      return this.avisoPagos;
    }

    if (this.estaPagado(gasto)) {
      return 'El gasto ya está pagado.';
    }

    if (!this.estaContabilizado(gasto)) {
      return (
        'El gasto debe estar contabilizado '
        + 'antes de registrar el pago.'
      );
    }

    if (this.procesandoGastoId !== null) {
      return 'Hay otra operación de gasto en curso.';
    }

    return '';
  }

  puedeDeshacerPago(
    gasto: Gasto
  ): boolean {

    return (
      this.pagosHabilitadosEnInterfaz
      && this.estaPagado(gasto)
      && this.procesandoGastoId === null
    );
  }

  motivoNoDeshacerPago(
    gasto: Gasto
  ): string {

    if (!this.pagosHabilitadosEnInterfaz) {
      return this.avisoPagos;
    }

    if (!this.estaPagado(gasto)) {
      return 'El gasto no está pagado.';
    }

    if (this.procesandoGastoId !== null) {
      return 'Hay otra operación de gasto en curso.';
    }

    return '';
  }

  pagarGasto(
    gasto: Gasto
  ): void {

    if (!this.puedePagar(gasto)) {
      this.errorOperacion =
        this.motivoNoPagar(gasto);

      this.mensajeOperacion = '';

      return;
    }

    const gastoId =
      Number(gasto.id);

    if (
      !Number.isInteger(gastoId)
      || gastoId <= 0
    ) {
      this.errorOperacion =
        'El identificador del gasto no es válido.';

      this.mensajeOperacion = '';

      return;
    }

    this.procesandoGastoId =
      gastoId;

    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.gastoService
      .pagar(
        gastoId
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({

        next: gastoActualizado => {

          this.procesandoGastoId =
            null;

          this.reemplazarGasto(
            gastoActualizado
          );

          this.mensajeOperacion =
            'Pago registrado correctamente.';

          this.errorOperacion = '';
        },

        error: error => {

          console.error(
            'Error pagando gasto:',
            error
          );

          this.procesandoGastoId =
            null;

          this.mensajeOperacion = '';

          this.errorOperacion =
            this.obtenerMensajeErrorOperacion(
              error,
              'No se pudo registrar el pago.'
            );
        }
      });
  }

  deshacerPagoGasto(
    gasto: Gasto
  ): void {

    if (!this.puedeDeshacerPago(gasto)) {
      this.errorOperacion =
        this.motivoNoDeshacerPago(
          gasto
        );

      this.mensajeOperacion = '';

      return;
    }

    const gastoId =
      Number(gasto.id);

    if (
      !Number.isInteger(gastoId)
      || gastoId <= 0
    ) {
      this.errorOperacion =
        'El identificador del gasto no es válido.';

      this.mensajeOperacion = '';

      return;
    }

    this.procesandoGastoId =
      gastoId;

    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.gastoService
      .deshacerPago(
        gastoId
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({

        next: gastoActualizado => {

          this.procesandoGastoId =
            null;

          this.reemplazarGasto(
            gastoActualizado
          );

          this.mensajeOperacion =
            'Pago anulado correctamente.';

          this.errorOperacion = '';
        },

        error: error => {

          console.error(
            'Error deshaciendo pago:',
            error
          );

          this.procesandoGastoId =
            null;

          this.mensajeOperacion = '';

          this.errorOperacion =
            this.obtenerMensajeErrorOperacion(
              error,
              'No se pudo deshacer el pago.'
            );
        }
      });
  }

  tienePdf(
    gasto: Gasto
  ): boolean {

    return Boolean(
      gasto.rutaPdf?.trim()
    );
  }

  puedeAdjuntarPdf(
    gasto: Gasto
  ): boolean {

    return (
      !this.tienePdf(gasto)
      && this.procesandoPdfGastoId === null
    );
  }

  motivoNoAdjuntarPdf(
    gasto: Gasto
  ): string {

    if (this.tienePdf(gasto)) {
      return (
        'El gasto ya tiene un PDF asociado. '
        + 'Durante la convivencia no se permite sustituirlo.'
      );
    }

    if (
      this.procesandoPdfGastoId !== null
    ) {
      return (
        'Hay otra operación de PDF en curso.'
      );
    }

    return '';
  }

  seleccionarPdf(
    gasto: Gasto,
    event: Event
  ): void {

    const input =
      event.target as HTMLInputElement;

    const archivo =
      input.files?.item(0)
      ?? null;

    /*
     * Limpiamos el input para permitir volver a seleccionar
     * el mismo fichero si posteriormente hubiera un error.
     */
    input.value = '';

    if (!archivo) {
      return;
    }

    this.adjuntarPdf(
      gasto,
      archivo
    );
  }

  adjuntarPdf(
    gasto: Gasto,
    archivo: File
  ): void {

    if (!this.puedeAdjuntarPdf(gasto)) {

      this.errorOperacion =
        this.motivoNoAdjuntarPdf(
          gasto
        );

      this.mensajeOperacion = '';

      return;
    }

    const gastoId =
      Number(
        gasto.id
      );

    if (
      !Number.isInteger(gastoId)
      || gastoId <= 0
    ) {
      this.errorOperacion =
        'El identificador del gasto no es válido.';

      this.mensajeOperacion = '';

      return;
    }

    if (
      !archivo.name
        .toLocaleLowerCase('es-ES')
        .endsWith('.pdf')
    ) {
      this.errorOperacion =
        'Solo se pueden adjuntar archivos PDF.';

      this.mensajeOperacion = '';

      return;
    }

    if (
      archivo.size <= 0
      || archivo.size
      > this.tamanioMaximoPdfBytes
    ) {
      this.errorOperacion =
        'El PDF no puede superar los 10 MB.';

      this.mensajeOperacion = '';

      return;
    }

    this.procesandoPdfGastoId =
      gastoId;

    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.gastoService
      .subirPdf(
        gastoId,
        archivo
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({

        next: gastoActualizado => {

          this.procesandoPdfGastoId =
            null;

          this.reemplazarGasto(
            gastoActualizado
          );

          this.mensajeOperacion =
            'PDF de la factura adjuntado correctamente.';

          this.errorOperacion = '';
        },

        error: error => {

          console.error(
            'Error adjuntando PDF al gasto:',
            error
          );

          this.procesandoPdfGastoId =
            null;

          this.mensajeOperacion = '';

          this.errorOperacion =
            this.obtenerMensajeErrorPdf(
              error,
              'No se pudo adjuntar el PDF de la factura.'
            );
        }
      });
  }

  verPdf(
    gasto: Gasto
  ): void {

    if (!this.tienePdf(gasto)) {
      this.errorOperacion =
        'El gasto no tiene un PDF asociado.';

      this.mensajeOperacion = '';

      return;
    }

    const gastoId =
      Number(
        gasto.id
      );

    if (
      !Number.isInteger(gastoId)
      || gastoId <= 0
    ) {
      this.errorOperacion =
        'El identificador del gasto no es válido.';

      this.mensajeOperacion = '';

      return;
    }

    if (
      this.procesandoPdfGastoId !== null
    ) {
      this.errorOperacion =
        'Hay otra operación de PDF en curso.';

      this.mensajeOperacion = '';

      return;
    }

    /*
     * Abrimos la pestaña mientras todavía estamos dentro
     * del clic del usuario para evitar que el navegador
     * la considere un popup no solicitado.
     */
    const ventanaPdf =
      window.open(
        '',
        '_blank'
      );

    if (!ventanaPdf) {
      this.errorOperacion =
        'El navegador ha bloqueado la apertura del PDF.';

      this.mensajeOperacion = '';

      return;
    }

    ventanaPdf.opener = null;

    this.procesandoPdfGastoId =
      gastoId;

    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.gastoService
      .obtenerPdf(
        gastoId
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({

        next: pdf => {

          this.procesandoPdfGastoId =
            null;

          if (
            !pdf
            || pdf.size <= 0
          ) {
            ventanaPdf.close();

            this.errorOperacion =
              'El servidor devolvió un PDF vacío.';

            return;
          }

          const url =
            URL.createObjectURL(
              pdf
            );

          ventanaPdf.location.href =
            url;

          /*
           * Damos tiempo al navegador para cargar el PDF
           * antes de liberar la URL temporal.
           */
          window.setTimeout(
            () => {
              URL.revokeObjectURL(
                url
              );
            },
            60000
          );

          this.mensajeOperacion =
            'PDF abierto correctamente.';

          this.errorOperacion = '';
        },

        error: error => {

          console.error(
            'Error abriendo PDF del gasto:',
            error
          );

          this.procesandoPdfGastoId =
            null;

          ventanaPdf.close();

          this.mensajeOperacion = '';

          this.errorOperacion =
            this.obtenerMensajeErrorPdf(
              error,
              'No se pudo abrir el PDF de la factura.'
            );
        }
      });
  }

  cargarGastos(): void {

    if (
      this.comunidadId === null
      || this.comunidadId <= 0
    ) {
      this.limpiarPantalla();
      return;
    }

    this.cargando = true;
    this.error = '';

    this.gastoService
      .listarPorComunidad(
        this.comunidadId
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({

        next: gastos => {

          this.gastos = [
            ...(gastos ?? [])
          ];

          this.paginaActual = 1;

          this.aplicarFiltros();

          this.cargando = false;
        },

        error: error => {

          console.error(
            'Error cargando gastos:',
            error
          );

          if (error?.status === 403) {
            this.error =
              'No tiene acceso a los gastos de esta comunidad.';
          } else if (
            error?.status === 401
          ) {
            this.error =
              'La sesión ha caducado. Vuelva a iniciar sesión.';
          } else {
            this.error =
              error?.error?.message
              || error?.error?.detail
              || 'No se pudieron cargar los gastos.';
          }

          this.gastos = [];
          this.gastosFiltrados = [];
          this.paginaActual = 1;
          this.cargando = false;
        }
      });
  }

  actualizarListado(): void {

    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.cargarGastos();
  }

  aplicarFiltros(): void {

    const busqueda =
      this.normalizar(
        this.textoBusqueda
      );

    this.gastosFiltrados =
      this.gastos.filter(gasto => {

        const coincideEstado =
          this.coincideEstado(
            gasto
          );

        if (!coincideEstado) {
          return false;
        }

        if (!busqueda) {
          return true;
        }

        const contenido =
          this.normalizar(
            [
              gasto.id,
              gasto.fechaFactura,
              gasto.proveedor,
              gasto.numeroFactura,
              gasto.concepto,
              gasto.importeTotal,
              gasto.numeroAsiento
            ]
              .filter(valor =>
                valor !== null
                && valor !== undefined
              )
              .join(' ')
          );

        return contenido.includes(
          busqueda
        );
      });

    this.paginaActual = 1;

    this.ajustarPagina();
  }

  limpiarFiltros(): void {

    this.textoBusqueda = '';
    this.filtroEstado = 'TODOS';

    this.aplicarFiltros();
  }

  get gastosPagina(): Gasto[] {

    const inicio =
      (
        this.paginaActual - 1
      )
      * this.registrosPorPagina;

    return this.gastosFiltrados.slice(
      inicio,
      inicio + this.registrosPorPagina
    );
  }

  get totalPaginas(): number {

    return Math.max(
      1,
      Math.ceil(
        this.gastosFiltrados.length
        / this.registrosPorPagina
      )
    );
  }

  get totalImporte(): number {

    return this.gastos.reduce(
      (total, gasto) =>
        total
        + Number(
          gasto.importeTotal ?? 0
        ),
      0
    );
  }

  get numeroPendientes(): number {

    return this.gastos.filter(
      gasto =>
        !this.estaPagado(gasto)
        && !this.estaContabilizado(gasto)
    ).length;
  }

  get numeroContabilizados(): number {

    return this.gastos.filter(
      gasto =>
        this.estaContabilizado(gasto)
        && !this.estaPagado(gasto)
    ).length;
  }

  get numeroPagados(): number {

    return this.gastos.filter(
      gasto =>
        this.estaPagado(gasto)
    ).length;
  }

  paginaAnterior(): void {

    if (this.paginaActual <= 1) {
      return;
    }

    this.paginaActual--;
  }

  paginaSiguiente(): void {

    if (
      this.paginaActual
      >= this.totalPaginas
    ) {
      return;
    }

    this.paginaActual++;
  }

  estadoGasto(
    gasto: Gasto
  ): string {

    if (this.estaPagado(gasto)) {
      return 'Pagado';
    }

    if (
      this.estaContabilizado(gasto)
    ) {
      return 'Contabilizado';
    }

    return 'Pendiente';
  }

  claseEstado(
    gasto: Gasto
  ): string {

    if (this.estaPagado(gasto)) {
      return 'estado pagado';
    }

    if (
      this.estaContabilizado(gasto)
    ) {
      return 'estado contabilizado';
    }

    return 'estado pendiente';
  }

  private coincideEstado(
    gasto: Gasto
  ): boolean {

    switch (this.filtroEstado) {

      case 'PENDIENTE':
        return (
          !this.estaPagado(gasto)
          && !this.estaContabilizado(
            gasto
          )
        );

      case 'CONTABILIZADO':
        return (
          this.estaContabilizado(gasto)
          && !this.estaPagado(gasto)
        );

      case 'PAGADO':
        return this.estaPagado(gasto);

      case 'TODOS':
        return true;
    }
  }

  private estaPagado(
    gasto: Gasto
  ): boolean {

    return gasto.pagado === true;
  }

  private estaContabilizado(
    gasto: Gasto
  ): boolean {

    return Boolean(
      gasto.numeroAsiento?.trim()
    );
  }

  private reemplazarGasto(
    gastoActualizado: Gasto
  ): void {

    const gastoId =
      Number(
        gastoActualizado.id
      );

    this.gastos =
      this.gastos.map(gasto =>
        Number(gasto.id) === gastoId
          ? gastoActualizado
          : gasto
      );

    this.aplicarFiltros();
  }

  private obtenerMensajeErrorOperacion(
    error: any,
    mensajeDefecto: string
  ): string {

    if (error?.status === 409) {
      return (
        error?.error?.detail
        || error?.error?.message
        || this.avisoPagos
      );
    }

    if (error?.status === 403) {
      return (
        'No tiene permiso para realizar '
        + 'esta operación.'
      );
    }

    if (error?.status === 401) {
      return (
        'La sesión ha caducado. '
        + 'Vuelva a iniciar sesión.'
      );
    }

    return (
      error?.error?.detail
      || error?.error?.message
      || mensajeDefecto
    );
  }

  private obtenerMensajeErrorPdf(
    error: any,
    mensajeDefecto: string
  ): string {

    if (error?.status === 409) {
      return (
        error?.error?.detail
        || error?.error?.message
        || 'El gasto ya tiene un PDF asociado.'
      );
    }

    if (error?.status === 404) {
      return (
        'No se ha encontrado el PDF '
        + 'asociado a este gasto.'
      );
    }

    if (error?.status === 403) {
      return (
        'No tiene permiso para acceder '
        + 'al PDF de este gasto.'
      );
    }

    if (error?.status === 401) {
      return (
        'La sesión ha caducado. '
        + 'Vuelva a iniciar sesión.'
      );
    }

    if (error?.status === 400) {
      return (
        error?.error?.detail
        || error?.error?.message
        || mensajeDefecto
      );
    }

    return mensajeDefecto;
  }

  private normalizar(
    valor: unknown
  ): string {

    return String(
      valor ?? ''
    )
      .trim()
      .toLocaleLowerCase(
        'es-ES'
      );
  }

  private ajustarPagina(): void {

    if (
      this.paginaActual
      > this.totalPaginas
    ) {
      this.paginaActual =
        this.totalPaginas;
    }
  }

  private limpiarPantalla(): void {

    this.comunidadId = null;
    this.nombreComunidad = '';

    this.gastos = [];
    this.gastosFiltrados = [];

    this.paginaActual = 1;
    this.cargando = false;

    this.procesandoGastoId = null;
    this.procesandoPdfGastoId = null;

    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.error =
      'Seleccione una comunidad en la parte superior.';
  }
}
