import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';

import { CommonModule } from '@angular/common';

import { Recibo } from '../../../../core/models/recibo.model';
import { ReciboService } from '../../../../core/services/recibo';
import { ComunidadService } from '../../../../core/services/comunidad';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { RemesaService } from '../../../../core/services/remesa.service';

@Component({
  selector: 'app-recibos-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './recibos-list.html',
  styleUrl: './recibos-list.scss'
})
export class RecibosList implements OnInit {

  private reciboService = inject(ReciboService);
  private remesaService = inject(RemesaService);
  private comunidadService = inject(ComunidadService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);

  paginaActual = 1;
  registrosPorPagina = 10;

  comunidadId = 0;
  nombreComunidad = '';

  campoOrden: keyof Recibo = 'fechaEmision';
  direccionOrden: 'asc' | 'desc' = 'desc';

  recibosSeleccionados = new Set<number>();

  estadoFiltro = '';

  fechaDesde = '';
  fechaHasta = '';

  importeMinimo: number | null = null;
  importeMaximo: number | null = null;

  recibos: Recibo[] = [];

  cargando = false;
  error = '';

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.comunidadId = Number(params['id'] || 0);

      if (this.comunidadId > 0) {
        this.cargarNombreComunidad();
        this.cargarRecibos();
      }
    });
  }

  cargarNombreComunidad(): void {
    this.comunidadService
      .getComunidad(this.comunidadId)
      .subscribe({
        next: (comunidad) => {
          this.nombreComunidad = comunidad.nombre;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando comunidad:', err);
          this.nombreComunidad = '';
          this.cdr.detectChanges();
        }
      });
  }

  cargarRecibos(): void {
    if (this.comunidadId <= 0) {
      return;
    }

    this.cargando = true;
    this.error = '';

    this.reciboService
      .getRecibos(this.comunidadId)
      .subscribe({
        next: (data) => {
          this.recibos = data;
          this.paginaActual = 1;
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando recibos:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudieron cargar los recibos.';
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  get totalPaginas(): number {

    let resultado = [...this.recibos];

    if (this.estadoFiltro) {
      resultado =
        resultado.filter(
          r => r.estado === this.estadoFiltro
        );
    }

    return Math.max(
      1,
      Math.ceil(
        resultado.length /
        this.registrosPorPagina
      )
    );
  }

  get recibosPaginados(): Recibo[] {

    let resultado = [...this.recibos];

    if (this.estadoFiltro) {
      resultado = resultado.filter(
        r => r.estado === this.estadoFiltro
      );
    }

    if (this.fechaDesde) {
      resultado = resultado.filter(
        r => r.fechaEmision >= this.fechaDesde
      );
    }

    if (this.fechaHasta) {
      resultado = resultado.filter(
        r => r.fechaEmision <= this.fechaHasta
      );
    }

    if (this.importeMinimo !== null) {
      resultado = resultado.filter(
        r => r.importe >= this.importeMinimo!
      );
    }

    if (this.importeMaximo !== null) {
      resultado = resultado.filter(
        r => r.importe <= this.importeMaximo!
      );
    }

    const inicio =
      (this.paginaActual - 1) *
      this.registrosPorPagina;

    const fin =
      inicio +
      this.registrosPorPagina;

    return resultado.slice(inicio, fin);
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) {
      return;
    }

    this.paginaActual = pagina;
  }

  ordenar(campo: keyof Recibo): void {

    if (this.campoOrden === campo) {

      this.direccionOrden =
        this.direccionOrden === 'asc'
          ? 'desc'
          : 'asc';

    } else {

      this.campoOrden = campo;
      this.direccionOrden = 'asc';

    }

    this.recibos.sort((a: any, b: any) => {

      const valorA = a[campo];
      const valorB = b[campo];

      if (valorA == null) return 1;
      if (valorB == null) return -1;

      const resultado =
        valorA > valorB ? 1 :
          valorA < valorB ? -1 : 0;

      return this.direccionOrden === 'asc'
        ? resultado
        : -resultado;
    });

    this.paginaActual = 1;
  }

  get totalSeleccionado(): number {

    return this.recibos
      .filter(r =>
        this.recibosSeleccionados.has(r.id)
      )
      .reduce(
        (total, r) =>
          total + r.importe,
        0
      );
  }

  toggleRecibo(id: number): void {

    if (this.recibosSeleccionados.has(id)) {

      this.recibosSeleccionados.delete(id);

    } else {

      this.recibosSeleccionados.add(id);

    }
  }

  estaSeleccionado(id: number): boolean {

    return this.recibosSeleccionados.has(id);
  }

  seleccionarTodos(event: any): void {

    if (event.target.checked) {

      this.recibosPaginados.forEach(r => {
        this.recibosSeleccionados.add(r.id);
      });

    } else {

      this.recibosPaginados.forEach(r => {
        this.recibosSeleccionados.delete(r.id);
      });

    }
  }

  generarRemesa(): void {

    if (this.recibosSeleccionados.size === 0) {

      alert(
        'Debe seleccionar al menos un recibo'
      );

      return;
    }

    const fechaCobro = new Date()
      .toISOString()
      .split('T')[0];

    this.remesaService
      .generarRemesaSeleccion(
        this.comunidadId,
        fechaCobro,
        [...this.recibosSeleccionados]
      )
      .subscribe({

        next: (response) => {

          alert(
            'Remesa generada correctamente. ID: '
            + response.remesaId
          );

          this.recibosSeleccionados.clear();

        },

        error: (err) => {

          console.error(err);

          alert(
            'Error generando remesa'
          );

        }

      });
  }
}
