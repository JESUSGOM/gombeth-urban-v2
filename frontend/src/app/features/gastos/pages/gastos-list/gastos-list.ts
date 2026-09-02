import {
  ChangeDetectorRef,
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
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';

import {
  Gasto
} from '../../../../core/models/gasto.model';

import {
  GastoService
} from '../../../../core/services/gasto.service';

import {
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

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

  private readonly cdr =
    inject(ChangeDetectorRef);

  gastos: Gasto[] = [];
  gastosFiltrados: Gasto[] = [];

  comunidadId: number | null = null;
  nombreComunidad = '';

  cargando = false;
  error = '';

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
        )
      )
      .subscribe(comunidad => {

        if (
          !comunidad
          || !Number.isInteger(
            Number(comunidad.id)
          )
          || Number(comunidad.id) <= 0
        ) {
          this.limpiarPantalla();
          return;
        }

        const cambiaComunidad =
          this.comunidadId
          !== Number(comunidad.id);

        this.comunidadId =
          Number(comunidad.id);

        this.nombreComunidad =
          comunidad.nombre;

        if (cambiaComunidad) {
          this.textoBusqueda = '';
          this.filtroEstado = 'TODOS';
          this.paginaActual = 1;
        }

        this.cargarGastos();
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

          this.actualizarVista();
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

          this.actualizarVista();
        }
      });
  }

  actualizarListado(): void {
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

    this.actualizarVista();
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

    this.actualizarVista();
  }

  paginaSiguiente(): void {

    if (
      this.paginaActual
      >= this.totalPaginas
    ) {
      return;
    }

    this.paginaActual++;

    this.actualizarVista();
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

    this.error =
      'Seleccione una comunidad en la parte superior.';

    this.actualizarVista();
  }

  private actualizarVista(): void {

    try {
      this.cdr.detectChanges();
    } catch {
      // El componente puede haberse destruido
      // mientras finalizaba una petición HTTP.
    }
  }
}
