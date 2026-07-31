import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { MovimientoBancario } from '../../../../core/models/movimiento-bancario.model';
import { MovimientoBancarioService } from '../../../../core/services/movimiento-bancario.service';
import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';

@Component({
  selector: 'app-movimientos-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './movimientos-list.html',
  styleUrl: './movimientos-list.scss'
})
export class MovimientosList implements OnInit, OnDestroy {

  private movimientoService = inject(MovimientoBancarioService);
  private comunidadState = inject(ComunidadStateService);
  private cdr = inject(ChangeDetectorRef);

  private comunidadSubscription?: Subscription;

  movimientos: MovimientoBancario[] = [];
  movimientosFiltrados: MovimientoBancario[] = [];

  nombreComunidadFiltro = '';
  resumenTesoreria: any = null;

  comunidadId: number | null = null;
  filtroTexto = '';
  filtroConciliado = 'TODOS';

  fechaDesde = '';
  fechaHasta = '';

  importeMinimo: number | null = null;
  importeMaximo: number | null = null;

  ordenCampo = 'fechaOperacion';
  ordenDireccion: 'asc' | 'desc' = 'asc';

  recibosPendientes: any[] = [];
  movimientoSeleccionado: any = null;
  movimientoDetalleConcepto: any = null;
  mostrarMovimientos = true;
  recibosSeleccionados: any[] = [];
  recibosSugeridos: number[] = [];
  contextoMovimiento: any = null;
  totalSeleccionado = 0;

  paginaActual = 1;
  registrosPorPagina = 10;

  paginaRecibos = 1;
  recibosPorPagina = 5;

  cargando = false;
  error = '';
  movimientoDesconciliandoId: number | null = null;

  ngOnInit(): void {
    this.comunidadState.init();

    this.comunidadSubscription =
      this.comunidadState.comunidad$
        .subscribe((comunidad) => {

          if (!comunidad) {
            this.comunidadId = null;
            this.nombreComunidadFiltro = '';
            this.movimientos = [];
            this.movimientosFiltrados = [];
            this.resumenTesoreria = null;
            this.mostrarMovimientos = true;
            this.movimientoSeleccionado = null;
            this.cargando = false;
            this.error =
              'Debe seleccionar una comunidad para ver sus movimientos bancarios.';

            this.cdr.detectChanges();
            return;
          }

          this.comunidadId = comunidad.id;
          this.nombreComunidadFiltro =
            comunidad.nombre;

          this.volverMovimientos();
          this.cargarMovimientos();
        });
  }

  ngOnDestroy(): void {
    this.comunidadSubscription?.unsubscribe();
  }

  cargarMovimientos(): void {
    this.cargando = true;
    this.error = '';
    this.movimientos = [];
    this.movimientosFiltrados = [];
    this.nombreComunidadFiltro = '';

    if (!this.comunidadId || this.comunidadId <= 0) {
      this.error =
        'Debe seleccionar una comunidad para ver sus movimientos bancarios.';
      this.cargando = false;
      this.cdr.detectChanges();
      return;
    }

    const comunidad = this.comunidadId;

    this.movimientoService
      .getNombreComunidad(
        comunidad,
        this.getUsuarioId()
      )
      .subscribe({
        next: (data) => {
          this.nombreComunidadFiltro = data.nombreComunidad;
          this.cdr.detectChanges();
        },
        error: () => {
          this.nombreComunidadFiltro = 'Comunidad ' + comunidad;
        }
      });

    this.movimientoService
      .getMovimientos(
        comunidad,
        this.getUsuarioId()
      )
      .subscribe({
        next: (data) => {
          this.movimientos = data;
          this.cargarResumenTesoreria();
          this.aplicarFiltros();
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando movimientos:', err);
          this.error =
            'No se pudieron cargar los movimientos bancarios de esta comunidad.';
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  aplicarFiltros(): void {
    const texto = this.filtroTexto.trim().toLowerCase();

    this.movimientosFiltrados =
      this.movimientos.filter(m => {

        const coincideTexto =
          !texto ||
          String(m.id).includes(texto) ||
          String(m.comunidadId).includes(texto) ||
          (m.concepto || '').toLowerCase().includes(texto) ||
          (m.referenciaBancaria || '').toLowerCase().includes(texto);

        const coincideConciliado =
          this.filtroConciliado === 'TODOS' ||
          (this.filtroConciliado === 'SI' && m.conciliado) ||
          (this.filtroConciliado === 'NO' && !m.conciliado);

        const fechaMovimiento =
          m.fechaOperacion
            ? new Date(m.fechaOperacion)
            : null;

        const cumpleDesde =
          !this.fechaDesde ||
          !fechaMovimiento ||
          fechaMovimiento >= new Date(this.fechaDesde);

        const cumpleHasta =
          !this.fechaHasta ||
          !fechaMovimiento ||
          fechaMovimiento <= new Date(this.fechaHasta);

        const importe =
          Math.abs(this.getImporteConSigno(m));

        const cumpleImporteMinimo =
          this.importeMinimo === null ||
          this.importeMinimo === undefined ||
          importe >= this.importeMinimo;

        const cumpleImporteMaximo =
          this.importeMaximo === null ||
          this.importeMaximo === undefined ||
          importe <= this.importeMaximo;

        return (
          coincideTexto &&
          coincideConciliado &&
          cumpleDesde &&
          cumpleHasta &&
          cumpleImporteMinimo &&
          cumpleImporteMaximo
        );
      });

    this.ordenarMovimientosFiltrados();
    this.paginaActual = 1;
  }

  get totalPaginas(): number {
    return Math.max(
      1,
      Math.ceil(this.movimientosFiltrados.length / this.registrosPorPagina)
    );
  }

  get movimientosPaginados(): MovimientoBancario[] {
    const inicio =
      (this.paginaActual - 1) * this.registrosPorPagina;

    return this.movimientosFiltrados.slice(
      inicio,
      inicio + this.registrosPorPagina
    );
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) {
      return;
    }

    this.paginaActual = pagina;
  }

  limpiarFiltros(): void {
    this.filtroTexto = '';
    this.filtroConciliado = 'TODOS';
    this.fechaDesde = '';
    this.fechaHasta = '';
    this.importeMinimo = null;
    this.importeMaximo = null;
    this.aplicarFiltros();
  }

  getImporteConSigno(m: MovimientoBancario): number {
    return m.signo === '2'
      ? Number(m.importe)
      : -Number(m.importe);
  }

  verRecibos(movimiento: any): void {
    this.movimientoSeleccionado = movimiento;

    this.contextoMovimiento = null;
    this.recibosPendientes = [];
    this.recibosSeleccionados = [];
    this.recibosSugeridos = [];
    this.totalSeleccionado = 0;
    this.paginaRecibos = 1;

    this.movimientoService
      .getContextoMovimiento(
        movimiento.id,
        this.getUsuarioId()
      )
      .subscribe({
        next: (data) => {
          this.contextoMovimiento = data;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(err);
        }
      });

    this.movimientoService
      .getRecibosPendientes(
        movimiento.id,
        this.getUsuarioId()
      )
      .subscribe({
        next: (data) => {
          this.recibosPendientes = data;
          this.mostrarMovimientos = false;

          this.sugerirConciliacionAutomatica();

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(err);
        }
      });
  }

  volverMovimientos(): void {
    this.mostrarMovimientos = true;
    this.movimientoSeleccionado = null;
    this.recibosPendientes = [];
    this.recibosSeleccionados = [];
    this.recibosSugeridos = [];
    this.totalSeleccionado = 0;
    this.contextoMovimiento = null;
  }

  seleccionarRecibo(
    recibo: any,
    event: Event
  ): void {
    const input = event.target as HTMLInputElement;
    const checked = input.checked;
    const reciboId = this.normalizarId(recibo?.id);

    if (checked) {
      const existe =
        this.recibosSeleccionados.some(
          r => this.normalizarId(r?.id) === reciboId
        );

      if (!existe) {
        this.recibosSeleccionados = [
          ...this.recibosSeleccionados,
          recibo
        ];
      }
    } else {
      this.recibosSeleccionados =
        this.recibosSeleccionados.filter(
          r => this.normalizarId(r?.id) !== reciboId
        );
    }

    this.recalcularTotalSeleccionado();
  }

  get totalPaginasRecibos(): number {
    return Math.max(
      1,
      Math.ceil(this.recibosPendientes.length / this.recibosPorPagina)
    );
  }

  get recibosPendientesPaginados(): any[] {
    const inicio =
      (this.paginaRecibos - 1) * this.recibosPorPagina;

    return this.recibosPendientes.slice(
      inicio,
      inicio + this.recibosPorPagina
    );
  }

  cambiarPaginaRecibos(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginasRecibos) {
      return;
    }

    this.paginaRecibos = pagina;
  }

  estaSeleccionado(recibo: any): boolean {
    const reciboId = this.normalizarId(recibo?.id);

    return this.recibosSeleccionados.some(
      r => this.normalizarId(r?.id) === reciboId
    );
  }

  conciliarSeleccionados(): void {
    if (!this.movimientoSeleccionado) {
      return;
    }

    if (this.recibosSeleccionados.length === 0) {
      alert('Debe seleccionar al menos un recibo.');
      return;
    }

    const reciboIds =
      this.recibosSeleccionados.map(r => r.id);

    this.movimientoService
      .conciliarMovimiento(
        this.movimientoSeleccionado.id,
        reciboIds,
        this.getUsuarioId()
      )
      .subscribe({
        next: () => {
          alert('Movimiento conciliado correctamente.');

          this.volverMovimientos();
          this.cargarMovimientos();
        },
        error: (err) => {
          console.error(err);
          alert(
            err?.error?.message ||
            'No se pudo conciliar el movimiento.'
          );
        }
      });
  }

  desconciliarMovimiento(
    movimiento: MovimientoBancario
  ): void {
    if (!movimiento?.id || !movimiento.conciliado) {
      return;
    }

    const confirmado = window.confirm(
      `Se va a desconciliar el movimiento #${movimiento.id}.\n\n` +
      'Se generará un contrasiento contable, los recibos asociados volverán a PENDIENTE ' +
      'y el movimiento quedará disponible para una nueva conciliación.\n\n' +
      '¿Desea continuar?'
    );

    if (!confirmado) {
      return;
    }

    this.movimientoDesconciliandoId = movimiento.id;
    this.error = '';

    this.movimientoService
      .desconciliarMovimiento(
        movimiento.id,
        this.getUsuarioId()
      )
      .subscribe({
        next: () => {
          this.movimientoDesconciliandoId = null;

          alert(
            'Movimiento desconciliado correctamente. ' +
            'Se ha generado el contrasiento contable y los recibos vuelven a estar pendientes.'
          );

          this.cargarMovimientos();
        },
        error: (err) => {
          console.error(
            'Error desconciliando el movimiento:',
            err
          );

          this.movimientoDesconciliandoId = null;

          const mensaje =
            err?.error?.detail ||
            err?.error?.message ||
            err?.error?.error ||
            'No se pudo desconciliar el movimiento.';

          alert(mensaje);
          this.cdr.detectChanges();
        }
      });
  }

  get diferenciaConciliacion(): number {
    if (!this.movimientoSeleccionado) {
      return 0;
    }

    return Math.abs(
      Number(this.getImporteConSigno(this.movimientoSeleccionado)) -
      Number(this.totalSeleccionado)
    );
  }

  get conciliacionCuadrada(): boolean {
    return this.diferenciaConciliacion < 0.01;
  }

  getUsuarioId(): number {
    const usuarioStorage = localStorage.getItem('usuario');

    if (!usuarioStorage) {
      return 0;
    }

    const usuario = JSON.parse(usuarioStorage);

    return Number(usuario.usuarioId || 0);
  }

  cargarResumenTesoreria(): void {
    if (!this.comunidadId) {
      this.resumenTesoreria = null;
      return;
    }

    this.movimientoService
      .getResumenTesoreria(
        this.comunidadId,
        this.getUsuarioId()
      )
      .subscribe({
        next: (data) => {
          this.resumenTesoreria = data;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando resumen tesorería:', err);
          this.resumenTesoreria = null;
        }
      });
  }

  abrirDetalleConcepto(movimiento: any): void {
    this.movimientoDetalleConcepto = movimiento;
  }

  cerrarDetalleConcepto(): void {
    this.movimientoDetalleConcepto = null;
  }

  sugerirConciliacionAutomatica(): void {
    if (!this.movimientoSeleccionado) {
      return;
    }

    this.movimientoService
      .getCandidatosConciliacion(
        this.movimientoSeleccionado.id,
        this.getUsuarioId()
      )
      .subscribe({
        next: (candidatos) => {
          this.recibosSeleccionados = [];
          this.recibosSugeridos = [];
          this.totalSeleccionado = 0;

          if (!candidatos || candidatos.length === 0) {
            this.cdr.detectChanges();
            return;
          }

          const candidatosUnicos = new Map<number, any>();

          for (const candidato of candidatos) {
            const candidatoId =
              this.normalizarId(candidato?.id);

            if (candidatoId <= 0) {
              continue;
            }

            const reciboPantalla =
              this.recibosPendientes.find(
                recibo =>
                  this.normalizarId(recibo?.id) ===
                  candidatoId
              );

            candidatosUnicos.set(
              candidatoId,
              reciboPantalla ?? candidato
            );
          }

          this.recibosSeleccionados =
            Array.from(candidatosUnicos.values());

          this.recibosSugeridos =
            Array.from(candidatosUnicos.keys());

          this.recalcularTotalSeleccionado();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error sugerencia conciliación:', err);
        }
      });
  }

  ordenarPor(campo: string): void {
    if (this.ordenCampo === campo) {
      this.ordenDireccion =
        this.ordenDireccion === 'asc'
          ? 'desc'
          : 'asc';
    } else {
      this.ordenCampo = campo;
      this.ordenDireccion = 'asc';
    }

    this.ordenarMovimientosFiltrados();
    this.paginaActual = 1;
    this.cdr.detectChanges();
  }

  ordenarMovimientosFiltrados(): void {
    const direccion =
      this.ordenDireccion === 'asc'
        ? 1
        : -1;

    this.movimientosFiltrados =
      [...this.movimientosFiltrados].sort((a: any, b: any) => {

        let valorA: any;
        let valorB: any;

        if (this.ordenCampo === 'importe') {
          valorA = this.getImporteConSigno(a);
          valorB = this.getImporteConSigno(b);
        } else {
          valorA = a[this.ordenCampo];
          valorB = b[this.ordenCampo];
        }

        if (valorA === null || valorA === undefined) {
          valorA = '';
        }

        if (valorB === null || valorB === undefined) {
          valorB = '';
        }

        if (
          this.ordenCampo === 'fechaOperacion' ||
          this.ordenCampo === 'fechaValor'
        ) {
          valorA = valorA ? new Date(valorA).getTime() : 0;
          valorB = valorB ? new Date(valorB).getTime() : 0;
        }

        if (typeof valorA === 'string') {
          valorA = valorA.toLowerCase();
        }

        if (typeof valorB === 'string') {
          valorB = valorB.toLowerCase();
        }

        if (valorA < valorB) {
          return -1 * direccion;
        }

        if (valorA > valorB) {
          return 1 * direccion;
        }

        return 0;
      });
  }

  iconoOrden(campo: string): string {
    if (this.ordenCampo !== campo) {
      return '';
    }

    return this.ordenDireccion === 'asc'
      ? ' ▲'
      : ' ▼';
  }

  esSugerido(recibo: any): boolean {
    const reciboId = this.normalizarId(recibo?.id);

    return this.recibosSugeridos.some(
      id => this.normalizarId(id) === reciboId
    );
  }

  private recalcularTotalSeleccionado(): void {
    this.totalSeleccionado =
      this.recibosSeleccionados.reduce(
        (suma, recibo) =>
          suma + Number(recibo?.importe ?? 0),
        0
      );
  }

  private normalizarId(valor: unknown): number {
    const id = Number(valor);

    return Number.isFinite(id)
      ? id
      : 0;
  }
}
