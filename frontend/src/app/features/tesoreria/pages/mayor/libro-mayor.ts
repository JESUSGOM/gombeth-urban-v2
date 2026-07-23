import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { LibroMayorService, LibroMayor } from '../../../../core/services/libro-mayor.service';
import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';

import { ComunidadActivaBar } from '../../../../shared/comunidad-activa-bar/comunidad-activa-bar';

@Component({
  selector: 'app-libro-mayor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ComunidadActivaBar
  ],
  templateUrl: './libro-mayor.html',
  styleUrl: './libro-mayor.scss'
})
export class LibroMayorComponent implements OnInit {

  private mayorService = inject(LibroMayorService);
  private comunidadState = inject(ComunidadStateService);

  data: LibroMayor | null = null;

  cuentaId = 1862;
  ejercicio = 2026;

  cargando = false;
  error = '';

  comunidadId: number | null = null;

  ngOnInit(): void {

    this.comunidadState.comunidad$.subscribe(comunidad => {

      if (!comunidad?.id) return;

      this.comunidadId = comunidad.id;

      this.cargar();
    });

  }

  cargar(): void {

    if (!this.comunidadId) return;

    this.cargando = true;
    this.error = '';

    this.mayorService.obtenerMayor(
      this.comunidadId,
      this.cuentaId,
      this.ejercicio
    ).subscribe({
      next: (res: LibroMayor) => {
        this.data = res;
        this.cargando = false;
      },
      error: (err) => {
        console.error(err);
        this.error = 'No se pudo cargar el Libro Mayor';
        this.cargando = false;
      }
    });
  }
}
