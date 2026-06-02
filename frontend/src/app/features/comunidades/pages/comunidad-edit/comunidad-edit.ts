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

  guardando = false;
  mensaje = '';
  error = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.comunidadService.getComunidad(id).subscribe({
      next: (data) => {
        console.log('COMUNIDAD:', data);
        this.comunidad = { ...data };
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando comunidad:', err);
        this.error = 'No se pudo cargar la comunidad.';
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
    this.mensaje = '';
    this.error = '';

    this.comunidadService
      .actualizarComunidad(this.comunidad.id, this.comunidad)
      .subscribe({
        next: (data) => {
          this.comunidad = { ...data };
          this.mensaje = 'Comunidad guardada correctamente.';
          this.guardando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error guardando comunidad:', err);
          this.error = 'No se pudo guardar la comunidad.';
          this.guardando = false;
          this.cdr.detectChanges();
        }
      });
  }
}
