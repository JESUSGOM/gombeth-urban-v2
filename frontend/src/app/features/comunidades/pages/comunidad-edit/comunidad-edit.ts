import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { Comunidad } from '../../../../core/models/comunidad.model';
import { ComunidadService } from '../../../../core/services/comunidad';

@Component({
  selector: 'app-comunidad-edit',
  imports: [CommonModule, FormsModule],
  templateUrl: './comunidad-edit.html',
  styleUrl: './comunidad-edit.scss',
})
export class ComunidadEdit implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private comunidadService = inject(ComunidadService);
  private cdr = inject(ChangeDetectorRef);

  comunidad?: Comunidad;
  usuarioId?: number;
  administradorId?: number;

  metodoReparto = 'COEFICIENTE';

  guardando = false;
  mensajeExito = '';
  mensajeError = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    const usuario = JSON.parse(
        localStorage.getItem('usuario') || 'null'
    );

    this.usuarioId = usuario?.usuarioId;
    this.administradorId = usuario?.administradorId;

    this.comunidadService
        .getComunidad(
            id,
            this.usuarioId,
            this.administradorId
        )
        .subscribe({
          next: (data) => {
            console.log('COMUNIDAD:', data);
            this.comunidad = { ...data };
            this.cargarConfiguracionReparto();
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error cargando comunidad:', err);
            this.mensajeError =
                err?.error?.message || 'No se pudo cargar la comunidad.';
            this.cdr.detectChanges();
          }
        });
  }

  cargarConfiguracionReparto(): void {
    if (!this.comunidad?.id) {
      return;
    }

    this.comunidadService
        .getConfiguracionReparto(this.comunidad.id)
        .subscribe({
          next: (config) => {
            this.metodoReparto =
                config.metodoReparto || 'COEFICIENTE';

            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error(
                'Error cargando configuración de reparto:',
                err
            );

            this.metodoReparto = 'COEFICIENTE';
            this.cdr.detectChanges();
          }
        });
  }

  volver(): void {
    this.router.navigate(['/comunidades']);
  }

  guardar(): void {
    if (!this.comunidad || !this.comunidad.id) {
      return;
    }

    this.guardando = true;
    this.mensajeExito = '';
    this.mensajeError = '';

    this.comunidadService
        .actualizarComunidad(
            this.comunidad.id,
            this.comunidad,
            this.usuarioId,
            this.administradorId
        )
        .subscribe({
          next: () => {
            this.guardarConfiguracionReparto();
          },
          error: (error) => {
            console.error('Error al guardar comunidad', error);
            this.guardando = false;
            this.mensajeError = 'No se pudo guardar la comunidad';
            this.cdr.detectChanges();
          }
        });
  }

  guardarConfiguracionReparto(): void {
    if (!this.comunidad?.id) {
      this.guardando = false;
      this.mensajeError =
          'No se pudo guardar la configuración de reparto.';
      this.cdr.detectChanges();
      return;
    }

    this.comunidadService
        .guardarConfiguracionReparto(
            this.comunidad.id,
            this.metodoReparto
        )
        .subscribe({
          next: () => {
            this.guardando = false;
            this.mensajeExito = 'Comunidad guardada correctamente';
            this.cdr.detectChanges();

            setTimeout(() => {
              this.router.navigate(['/comunidades']);
            }, 3000);
          },
          error: (err) => {
            console.error(
                'Error guardando configuración de reparto:',
                err
            );

            this.guardando = false;
            this.mensajeError =
                'La comunidad se guardó, pero no se pudo guardar el método de reparto.';
            this.cdr.detectChanges();
          }
        });
  }
}
