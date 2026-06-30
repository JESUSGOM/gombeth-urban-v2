import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { BalanceService } from '../../../../core/services/balance.service';
import { BalanceLinea } from '../../../../core/models/balance.model';

import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';
import { ComunidadActivaBar } from '../../../../shared/comunidad-activa-bar/comunidad-activa-bar';

@Component({
  selector: 'app-balance',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ComunidadActivaBar
  ],
  templateUrl: './balance.html',
  styleUrl: './balance.scss'
})
export class BalanceComponent implements OnInit {

  private balanceService = inject(BalanceService);
  private comunidadState = inject(ComunidadStateService);

  lineas: BalanceLinea[] = [];

  cargando = false;
  error = '';

  ejercicio = 2026;

  comunidadId: number | null = null;

  ngOnInit(): void {

    this.comunidadState.comunidad$.subscribe(comunidad => {

      if (!comunidad?.id) return;

      this.comunidadId = comunidad.id;

      this.cargarBalance();
    });

  }

  cargarBalance(): void {

    if (!this.comunidadId) return;

    this.cargando = true;
    this.error = '';

    this.balanceService.obtenerBalance(this.comunidadId)
      .subscribe({
        next: (data: BalanceLinea[]) => {
          this.lineas = data;
          this.cargando = false;
        },
        error: (err) => {
          console.error(err);
          this.error = 'Error al cargar el balance';
          this.cargando = false;
        }
      });

  }

  get totalDebe(): number {
    return this.lineas.reduce((a, b) => a + b.debe, 0);
  }

  get totalHaber(): number {
    return this.lineas.reduce((a, b) => a + b.haber, 0);
  }

  get saldo(): number {
    return this.totalDebe - this.totalHaber;
  }
}
