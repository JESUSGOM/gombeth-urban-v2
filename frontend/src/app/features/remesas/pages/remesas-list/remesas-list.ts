import {
  Component,
  OnInit,
  inject,
  ChangeDetectorRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { Remesa } from '../../../../core/models/remesa.model';
import { RemesaService } from '../../../../core/services/remesa.service';
import { ComunidadService } from '../../../../core/services/comunidad';

@Component({
  selector: 'app-remesas-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './remesas-list.html',
  styleUrl: './remesas-list.scss'
})
export class RemesasList implements OnInit {

  private remesaService = inject(RemesaService);
  private comunidadService = inject(ComunidadService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);

  comunidadId = 0;
  nombreComunidad = '';

  remesas: Remesa[] = [];

  fechaCobro = '';
  fechaDesde = '';
  fechaHasta = '';

  generando = false;
  mensaje = '';

  resultadoValidacion = '';
  erroresValidacion: string[] = [];
  remesasValidadas = new Set<number>();

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
      this.comunidadId = Number(params['id'] || 0);

      if (this.comunidadId > 0) {
        this.cargarNombreComunidad();
        this.cargarRemesas();
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

  cargarRemesas(): void {
    if (this.comunidadId <= 0) {
      return;
    }

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
    if (
      !this.fechaCobro ||
      !this.fechaDesde ||
      !this.fechaHasta
    ) {
      this.mensaje =
        'Debe indicar todas las fechas.';
      return;
    }

    if (this.comunidadId <= 0) {
      this.mensaje =
        'No hay comunidad seleccionada.';
      return;
    }

    this.generando = true;
    this.mensaje = '';
    this.error = '';

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
            response?.mensaje ||
            'Remesa generada correctamente.';

          this.cargarRemesas();
        },
        error: (err) => {
          console.error('Error generando remesa:', err);

          this.generando = false;
          this.error =
            err?.error?.message ||
            err?.error ||
            'Error generando remesa.';

          this.cdr.detectChanges();
        }
      });
  }

  get totalPaginas(): number {
    return Math.max(
      1,
      Math.ceil(
        this.remesas.length /
        this.registrosPorPagina
      )
    );
  }

  get remesasPaginadas(): Remesa[] {
    const inicio =
      (this.paginaActual - 1) *
      this.registrosPorPagina;

    const fin =
      inicio +
      this.registrosPorPagina;

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

  descargarC19(remesa: Remesa): void {
    alert(
      'Generación C19 pendiente para remesa '
      + remesa.id
    );
  }

  validarRemesa(remesa: Remesa): void {
    this.resultadoValidacion = '';
    this.erroresValidacion = [];

    this.remesaService
      .validarRemesa(remesa.id)
      .subscribe({
        next: (response) => {
          if (response.valida) {
            this.remesasValidadas.add(remesa.id);

            this.resultadoValidacion =
              'Remesa ' + remesa.id + ' validada correctamente.';
          } else {
            this.resultadoValidacion =
              'Remesa ' + remesa.id + ' contiene errores.';

            this.erroresValidacion =
              response.mensajes || [];
          }

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error validando remesa:', err);

          this.resultadoValidacion =
            'Error validando la remesa.';

          this.cdr.detectChanges();
        }
      });
  }

  verDetalleRemesa(id: number): void {
    this.router.navigate([
      '/remesas',
      id
    ]);
  }
}
