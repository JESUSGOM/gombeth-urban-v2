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
import { CoeficientesResumen } from '../../../../core/models/coeficientes-resumen.model';
import { CuotaPresupuesto } from '../../../../core/models/cuota-presupuesto.model';
import { ConfiguracionReparto } from '../../../../core/models/configuracion-reparto.model';
import { Comunidad } from '../../../../core/models/comunidad.model';
import { PresupuestoRevision } from '../../../../core/models/presupuesto-revision.model';

import { PresupuestoService } from '../../../../core/services/presupuesto';
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
  private comunidadService = inject(ComunidadService);
  private comunidadState = inject(ComunidadStateService);
  private cdr = inject(ChangeDetectorRef);

  comunidadId = 0;
  comunidad?: Comunidad;

  anio = 2026;
  mesRecibos = new Date().getMonth() + 1;
  generandoRecibos = false;

  metodoReparto = 'IGUALITARIO';
  configuracionReparto?: ConfiguracionReparto;

  presupuestos: Presupuesto[] = [];
  reparto: RepartoPresupuesto[] = [];
  cuotasBorrador: CuotaPresupuesto[] = [];
  revisiones: PresupuestoRevision[] = [];

  resumenCoeficientes?: CoeficientesResumen;

  total = 0;
  error = '';
  mensaje = '';
  cargando = false;
  generandoCuotas = false;
  guardandoMetodo = false;

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
    this.reparto = [];
    this.cuotasBorrador = [];
    this.revisiones = [];
    this.resumenCoeficientes = undefined;
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
    this.cargarConfiguracionReparto();
    this.cargarPresupuestos();
  }

  cargarComunidad(): void {

    this.comunidadService
      .getComunidad(this.comunidadId)
      .subscribe({

        next: (data) => {

          this.comunidad = data;

          switch (data.tipoReparto) {

            case 'COEFICIENTE':
              this.metodoReparto = 'COEFICIENTE';
              break;

            case 'PARTES_IGUALES':
              this.metodoReparto = 'PARTES_IGUALES';
              break;

            default:
              this.metodoReparto = 'PARTES_IGUALES';
              break;
          }

          this.cdr.detectChanges();
        },

        error: (err) => {
          console.error('Error cargando comunidad:', err);
        }

      });

  }

  cargarConfiguracionReparto(): void {
    // El método de reparto se toma desde comunidades.tipo_reparto
  }

  guardarMetodoReparto(): void {
    if (this.comunidadId <= 0) return;

    this.guardandoMetodo = true;
    this.error = '';
    this.mensaje = '';

    this.comunidadService
      .guardarConfiguracionReparto(this.comunidadId, this.metodoReparto)
      .subscribe({
        next: (data) => {
          this.configuracionReparto = data;
          this.mensaje = 'Método de reparto guardado correctamente.';
          this.guardandoMetodo = false;
          this.cargarPresupuestos();
        },
        error: (err) => {
          console.error('Error guardando método de reparto:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudo guardar el método de reparto.';
          this.guardandoMetodo = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarPresupuestos(): void {
    if (this.comunidadId <= 0) return;

    this.cargando = true;
    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .getPresupuestosComunidad(this.comunidadId, this.anio)
      .subscribe({
        next: (data) => {
          this.presupuestos = data;
          this.total = data.reduce(
            (suma, p) => suma + Number(p.importe || 0),
            0
          );

          this.cargarRevisiones();
          this.cargarReparto();
        },
        error: (err) => {
          console.error('Error cargando presupuesto:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudo cargar el presupuesto.';
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarRevisiones(): void {
    this.presupuestoService
      .getRevisiones(this.comunidadId, this.anio)
      .subscribe({
        next: (data) => {
          this.revisiones = data;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando revisiones:', err);
        }
      });
  }

  cargarReparto(): void {
    this.presupuestoService
      .getRepartoComunidad(this.comunidadId, this.anio)
      .subscribe({
        next: (data) => {
          this.reparto = data;
          this.cargarCuotasBorrador();
        },
        error: (err) => {
          console.error('Error cargando reparto:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudo cargar el reparto.';
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  cargarCuotasBorrador(): void {
    this.presupuestoService
      .getCuotasBorrador(this.comunidadId, this.anio)
      .subscribe({
        next: (data) => {
          this.cuotasBorrador = data;
          this.cargarResumenCoeficientes();
        },
        error: (err) => {
          console.error('Error cargando cuotas:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudieron cargar las cuotas.';
          this.cargarResumenCoeficientes();
        }
      });
  }

  cargarResumenCoeficientes(): void {
    this.comunidadService
      .getResumenCoeficientes(this.comunidadId)
      .subscribe({
        next: (data) => {
          this.resumenCoeficientes = data;
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando resumen coeficientes:', err);
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  generarBorradorCuotas(): void {
    if (this.comunidadId <= 0) return;

    this.generandoCuotas = true;
    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .generarBorradorCuotas(this.comunidadId, this.anio)
      .subscribe({
        next: (data) => {
          this.mensaje =
            data?.mensaje ||
            'Borrador de cuotas generado correctamente.';

          this.generandoCuotas = false;
          this.cargarCuotasBorrador();
          this.cargarRevisiones();
        },
        error: (err) => {
          console.error('Error generando borrador de cuotas:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudo generar el borrador de cuotas.';

          this.generandoCuotas = false;
          this.cdr.detectChanges();
        }
      });
  }

  aprobarCuotas(): void {
    if (this.comunidadId <= 0) return;

    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .aprobarCuotas(this.comunidadId, this.anio)
      .subscribe({
        next: (data) => {
          this.mensaje =
            data?.mensaje ||
            'Cuotas aprobadas correctamente.';

          this.cargarCuotasBorrador();
          this.cargarRevisiones();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error aprobando cuotas:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudieron aprobar las cuotas.';

          this.cdr.detectChanges();
        }
      });
  }

  aprobarRevision(id: number): void {
    if (!confirm('¿Desea aprobar esta revisión presupuestaria?')) return;

    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .aprobarRevision(id)
      .subscribe({
        next: (resp) => {
          this.mensaje =
            resp?.mensaje ||
            'Revisión aprobada correctamente.';

          this.cargarPresupuestos();
        },
        error: (err) => {
          console.error('Error aprobando revisión:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudo aprobar la revisión.';

          this.cdr.detectChanges();
        }
      });
  }

  eliminarRevision(id: number): void {
    if (!confirm('¿Desea eliminar esta revisión presupuestaria?')) return;

    this.error = '';
    this.mensaje = '';

    this.presupuestoService
      .eliminarRevision(id)
      .subscribe({
        next: () => {
          this.mensaje = 'Revisión eliminada correctamente.';
          this.cargarPresupuestos();
        },
        error: (err) => {
          console.error('Error eliminando revisión:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudo eliminar la revisión.';

          this.cdr.detectChanges();
        }
      });
  }

  generarRecibos(): void {
    if (this.comunidadId <= 0) return;

    if (!confirm('¿Desea generar los recibos desde las cuotas aprobadas?')) {
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
        next: (data) => {
          this.mensaje =
            data?.mensaje ||
            'Recibos generados correctamente.';

          this.generandoRecibos = false;
          this.cargarCuotasBorrador();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error generando recibos:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudieron generar los recibos.';

          this.generandoRecibos = false;
          this.cdr.detectChanges();
        }
      });
  }
}
