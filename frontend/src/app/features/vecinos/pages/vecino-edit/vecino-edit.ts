import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Vecino } from '../../../../core/models/vecino.model';
import { VecinoService } from '../../../../core/services/vecino';

@Component({
  selector: 'app-vecino-edit',
  imports: [CommonModule, FormsModule],
  templateUrl: './vecino-edit.html',
  styleUrl: './vecino-edit.scss',
})
export class VecinoEdit implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private vecinoService = inject(VecinoService);
  private cdr = inject(ChangeDetectorRef);

  vecino?: Vecino;

  guardando = false;
  mensaje = '';
  error = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.vecinoService.getVecino(id).subscribe({
      next: (data) => {
        this.vecino = { ...data };
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando propietario:', err);
        this.error = 'No se pudo cargar el propietario.';
        this.cdr.detectChanges();
      }
    });
  }

  guardar(): void {
    if (!this.vecino || !this.vecino.id) {
      return;
    }

    this.guardando = true;
    this.mensaje = '';
    this.error = '';

    this.vecinoService
      .actualizarVecino(this.vecino.id, this.vecino)
      .subscribe({
        next: (data) => {
          this.vecino = { ...data };
          this.mensaje = 'Propietario guardado correctamente.';
          this.guardando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error guardando propietario:', err);
          this.error = 'No se pudo guardar el propietario.';
          this.guardando = false;
          this.cdr.detectChanges();
        }
      });
  }

  volver(): void {
    if (this.vecino?.comunidadId) {
      this.router.navigate(['/vecinos/comunidad', this.vecino.comunidadId]);
      return;
    }

    this.router.navigate(['/comunidades']);
  }
}
