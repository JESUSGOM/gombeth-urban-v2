import {
  Component,
  OnInit,
  inject,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MovimientoBancario } from '../../../../core/models/movimiento-bancario.model';
import { MovimientoBancarioService } from '../../../../core/services/movimiento-bancario.service';
import { Comunidad } from '../../../../core/models/comunidad.model';
import { ComunidadService } from '../../../../core/services/comunidad';

@Component({
  selector: 'app-movimientos-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './movimientos-list.html',
  styleUrl: './movimientos-list.scss'
})
export class MovimientosList implements OnInit {

  private movimientoService = inject(MovimientoBancarioService);
  private comunidadService = inject(ComunidadService);
  private cdr = inject(ChangeDetectorRef);

  movimientos: MovimientoBancario[] = [];
  movimientosFiltrados: MovimientoBancario[] = [];
  comunidades: Comunidad[] = [];

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

  ngOnInit(): void {
    this.cargarComunidadesUsuario();
  }

  cargarComunidadesUsuario(): void {
    this.comunidadService
        .getComunidades(
            0,
            500
        )
        .subscribe({
          next: (response) => {
            this.comunidades = response.content || [];

            if (this.comunidades.length > 0) {
              this.comunidadId = this.comunidades[0].id!;
              this.cargarMovimientos();
            }

            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error cargando comunidades', err);
          }
        });
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
            comunidad
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
            comunidad
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
    this.totalSeleccionado = 0;
    this.paginaRecibos = 1;

    this.movimientoService
        .getContextoMovimiento(
            movimiento.id
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
            movimiento.id
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
    this.totalSeleccionado = 0;
    this.contextoMovimiento = null;
  }

  seleccionarRecibo(
      recibo: any,
      event: any
  ): void {
    const checked = event.target.checked;

    if (checked) {
      const existe =
          this.recibosSeleccionados.some(
              r => r.id === recibo.id
          );

      if (!existe) {
        this.recibosSeleccionados.push(recibo);
      }
    } else {
      this.recibosSeleccionados =
          this.recibosSeleccionados.filter(
              r => r.id !== recibo.id
          );
    }

    this.totalSeleccionado =
        this.recibosSeleccionados.reduce(
            (suma, r) => suma + Number(r.importe),
            0
        );
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
    return this.recibosSeleccionados.some(
        r => r.id === recibo.id
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
            reciboIds
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
            this.comunidadId
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
            this.movimientoSeleccionado.id
        )
        .subscribe({
          next: (candidatos) => {
            if (!candidatos || candidatos.length === 0) {
              return;
            }

            this.recibosSeleccionados = candidatos;

            this.recibosSugeridos =
                candidatos.map((r: any) => r.id);

            this.totalSeleccionado =
                candidatos.reduce(
                    (suma, r) => suma + Number(r.importe),
                    0
                );

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

    return this.recibosSugeridos.includes(
        recibo.id
    );

  }
}
