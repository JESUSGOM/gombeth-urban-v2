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

  private diarioService =
    inject(DiarioService);

  private comunidadState =
    inject(ComunidadStateService);

  diarios: Diario[] = [];

  comunidadId: number | null = null;
  ejercicio = 2026;

  cargando = false;
  error = '';

  paginaActual = 1;
  elementosPorPagina = 10;

  readonly opcionesElementosPorPagina: number[] = [
    10,
    25,
    50
  ];

  get diariosPaginados(): Diario[] {

    const indiceInicio =
      (this.paginaActual - 1) *
      this.elementosPorPagina;

    const indiceFin =
      indiceInicio +
      this.elementosPorPagina;

    return this.diarios.slice(
      indiceInicio,
      indiceFin
    );
  }

  get totalPaginas(): number {

    return Math.max(
      1,
      Math.ceil(
        this.diarios.length /
        this.elementosPorPagina
      )
    );
  }

  get primerRegistroMostrado(): number {

    if (this.diarios.length === 0) {
      return 0;
    }

    return (
      (this.paginaActual - 1) *
      this.elementosPorPagina
    ) + 1;
  }

  get ultimoRegistroMostrado(): number {

    return Math.min(
      this.paginaActual *
      this.elementosPorPagina,
      this.diarios.length
    );
  }

  ngOnInit(): void {

    this.comunidadState.init();

    this.comunidadState
      .comunidad$
      .subscribe(comunidad => {

        if (!comunidad?.id) {
          return;
        }

        this.comunidadId =
          comunidad.id;

        this.cargarDiario();
      });
  }

  cargarDiario(): void {

    if (!this.comunidadId) {
      return;
    }

    this.paginaActual = 1;
    this.cargando = true;
    this.error = '';

    this.diarioService
      .listar(
        this.comunidadId,
        this.ejercicio
      )
      .subscribe({
        next: (data: Diario[]) => {

          this.diarios =
            data ?? [];

          this.ajustarPaginaActual();

          this.cargando = false;
        },

        error: (err: unknown) => {

          console.error(
            'Error cargando el diario contable:',
            err
          );

          this.diarios = [];
          this.paginaActual = 1;

          this.error =
            'No se pudo cargar el diario contable.';

          this.cargando = false;
        }
      });
  }

  cambiarPagina(
    nuevaPagina: number
  ): void {

    if (
      nuevaPagina < 1 ||
      nuevaPagina > this.totalPaginas
    ) {
      return;
    }

    this.paginaActual =
      nuevaPagina;
  }

  cambiarElementosPorPagina(): void {

    this.paginaActual = 1;
    this.ajustarPaginaActual();
  }

  private ajustarPaginaActual(): void {

    if (
      this.paginaActual >
      this.totalPaginas
    ) {
      this.paginaActual =
        this.totalPaginas;
    }

    if (this.paginaActual < 1) {
      this.paginaActual = 1;
    }
  }
}