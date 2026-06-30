import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DiarioService } from '../../../../core/services/diario.service';
import { Diario } from '../../../../core/models/diario.model';
import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';

@Component({
  selector: 'app-diario-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './diario-list.html',
  styleUrl: './diario-list.scss'
})
export class DiarioListComponent implements OnInit {

  private diarioService = inject(DiarioService);
  private comunidadState = inject(ComunidadStateService);

  diarios: Diario[] = [];

  comunidadId: number | null = null;
  ejercicio = 2026;

  cargando = false;
  error = '';

  ngOnInit(): void {
    this.comunidadState.init();

    this.comunidadState.comunidad$.subscribe(comunidad => {
      if (!comunidad?.id) {
        return;
      }

      this.comunidadId = comunidad.id;
      this.cargarDiario();
    });
  }

  cargarDiario(): void {
    if (!this.comunidadId) {
      return;
    }

    this.cargando = true;
    this.error = '';

    this.diarioService
      .listar(this.comunidadId, this.ejercicio)
      .subscribe({
        next: (data: Diario[]) => {
          this.diarios = data;
          this.cargando = false;
        },
        error: (err: unknown) => {
          console.error(err);
          this.error = 'No se pudo cargar el diario contable.';
          this.cargando = false;
        }
      });
  }
}
