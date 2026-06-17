import {
  Component,
  OnInit,
  inject,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { Remesa } from '../../../../core/models/remesa.model';
import { RemesaService } from '../../../../core/services/remesa.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-remesas-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './remesas-list.html',
  styleUrl: './remesas-list.scss'
})
export class RemesasList implements OnInit {

  private remesaService = inject(RemesaService);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  comunidadId!: number;

  remesas: Remesa[] = [];

  fechaCobro = '';
  fechaDesde = '';
  fechaHasta = '';

  generando = false;
  mensaje = '';

  paginaActual = 1;
  registrosPorPagina = 10;

  cargando = false;
  error = '';

  ngOnInit(): void {
    const hoy = new Date();

    this.fechaCobro =
      hoy.toISOString().substring(0, 10);

    this.fechaDesde =
      hoy.getFullYear() + '-01-01';

    this.fechaHasta =
      hoy.toISOString().substring(0, 10);
    this.route.params.subscribe(params => {
      this.comunidadId = Number(params['id']);
      this.cargarRemesas();
    });
  }

  cargarRemesas(): void {
    this.cargando = true;
    this.error = '';

    this.remesaService
      .getRemesas(this.comunidadId)
      .subscribe({
        next: (data) => {
          this.remesas = data;
          this.paginaActual = 1;
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando remesas:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudieron cargar las remesas.';
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  generarRemesa(): void {

    if (!this.fechaCobro ||
      !this.fechaDesde ||
      !this.fechaHasta) {

      this.mensaje =
        'Debe indicar todas las fechas.';

      return;
    }

    this.generando = true;
    this.mensaje = '';

    this.remesaService
      .generarRemesa(
        this.comunidadId,
        this.fechaCobro,
        this.fechaDesde,
        this.fechaHasta
      )
      .subscribe({

        next: (response) => {

          this.generando = false;

          this.mensaje =
            response.mensaje;

          this.cargarRemesas();
        },

        error: (err) => {

          console.error(err);

          this.generando = false;

          this.mensaje =
            'Error generando remesa.';
        }

      });

  }

  get totalPaginas(): number {
    return Math.ceil(this.remesas.length / this.registrosPorPagina);
  }

  get remesasPaginadas(): Remesa[] {
    const inicio =
      (this.paginaActual - 1) * this.registrosPorPagina;

    const fin =
      inicio + this.registrosPorPagina;

    return this.remesas.slice(inicio, fin);
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) {
      return;
    }

    this.paginaActual = pagina;
  }

  descargarXml(remesa: Remesa): void {
    window.open(
      `http://localhost:8080/api/remesas/${remesa.id}/xml`,
      '_blank'
    );
  }
}
