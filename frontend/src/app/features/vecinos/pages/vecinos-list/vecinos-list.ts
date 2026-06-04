import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { Vecino } from '../../../../core/models/vecino.model';
import { VecinoService } from '../../../../core/services/vecino';

@Component({
  selector: 'app-vecinos-list',
  imports: [CommonModule],
  templateUrl: './vecinos-list.html',
  styleUrl: './vecinos-list.scss',
})
export class VecinosList implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private vecinoService = inject(VecinoService);
  private cdr = inject(ChangeDetectorRef);

  comunidadId = 0;
  vecinos: Vecino[] = [];

  cargando = true;
  error = '';

  paginaActual = 1;
  tamanioPagina = 10;
  totalPaginas = 0;
  totalElementos = 0;

  ngOnInit(): void {
    this.comunidadId = Number(
      this.route.snapshot.paramMap.get('id')
    );

    this.cargarVecinos();
  }

  cargarVecinos(): void {
    this.cargando = true;
    this.error = '';

    this.vecinoService
      .getVecinosPorComunidad(
        this.comunidadId,
        this.paginaActual - 1,
        this.tamanioPagina
      )
      .subscribe({
        next: (data) => {
          console.log('VECINOS:', data);

          this.vecinos = [...(data.content ?? [])];
          this.totalPaginas = data.totalPages ?? 0;
          this.totalElementos = data.totalElements ?? 0;
          this.cargando = false;

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando vecinos:', err);

          this.error = 'No se pudieron cargar los propietarios.';
          this.cargando = false;

          this.cdr.detectChanges();
        }
      });
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) {
      return;
    }

    this.paginaActual = pagina;
    this.cargarVecinos();
  }

  paginas(): number[] {
    return Array.from(
      { length: this.totalPaginas },
      (_, i) => i + 1
    );
  }

  volver(): void {
    this.router.navigate(['/comunidades']);
  }
}
