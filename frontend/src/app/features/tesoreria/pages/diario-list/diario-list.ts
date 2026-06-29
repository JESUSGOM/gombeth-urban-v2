import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { Diario, DiarioDetalle, MovimientoDiario } from '../../../../core/models/diario.model';
import { DiarioService } from '../../../../core/services/diario.service';
import { CuentasContablesService, CuentaContable } from '../../../../core/services/cuentas-contables.service';

@Component({
  selector: 'app-diario-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './diario-list.html',
  styleUrl: './diario-list.scss'
})
export class DiarioList implements OnInit {

  private diarioService = inject(DiarioService);
  private cuentasService = inject(CuentasContablesService);
  private cdr = inject(ChangeDetectorRef);

  comunidadId = 17;
  ejercicio = 2026;

  diarios: Diario[] = [];
  asientoSeleccionado: DiarioDetalle | null = null;

  cuentasPorId = new Map<number, CuentaContable>();

  cargando = false;
  error = '';

  paginaActual = 1;
  registrosPorPagina = 15;
  filtroTexto = '';

  ngOnInit(): void {
    this.cargarCuentas();
    this.cargarDiario();
  }

  cargarCuentas(): void {
    this.cuentasService.listarPorComunidad(this.comunidadId).subscribe({
      next: (data: CuentaContable[]) => {
        this.cuentasPorId.clear();
        data.forEach(c => this.cuentasPorId.set(c.id, c));
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        console.error('Error cargando cuentas', err);
      }
    });
  }

  cargarDiario(): void {
    this.cargando = true;
    this.error = '';

    this.diarioService.listar(this.comunidadId, this.ejercicio).subscribe({
      next: (data: Diario[]) => {
        this.diarios = data;
        this.cargando = false;
        this.paginaActual = 1;
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        console.error(err);
        this.error = 'No se pudo cargar el Diario';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  verAsiento(id: number): void {
    this.diarioService.detalle(id).subscribe({
      next: (data: DiarioDetalle) => {
        data.movimientos = data.movimientos.map(m => this.enriquecerMovimiento(m));
        this.asientoSeleccionado = data;
        this.cdr.detectChanges();
      },
      error: (err: unknown) => {
        console.error(err);
        alert('No se pudo cargar el asiento.');
      }
    });
  }

  enriquecerMovimiento(movimiento: MovimientoDiario): MovimientoDiario {
    const cuenta = this.cuentasPorId.get(movimiento.cuentaId);

    return {
      ...movimiento,
      codigoCuenta: cuenta?.codigo || String(movimiento.cuentaId),
      nombreCuenta: cuenta?.nombre || ''
    };
  }

  cerrarDetalle(): void {
    this.asientoSeleccionado = null;
  }

  get diariosFiltrados(): Diario[] {
    let resultado = [...this.diarios];

    if (this.filtroTexto.trim()) {
      const texto = this.filtroTexto.toLowerCase();
      resultado = resultado.filter(d =>
        d.concepto.toLowerCase().includes(texto) ||
        d.origen.toLowerCase().includes(texto)
      );
    }

    const inicio = (this.paginaActual - 1) * this.registrosPorPagina;
    return resultado.slice(inicio, inicio + this.registrosPorPagina);
  }

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.diarios.length / this.registrosPorPagina));
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) {
      return;
    }

    this.paginaActual = pagina;
  }
}
