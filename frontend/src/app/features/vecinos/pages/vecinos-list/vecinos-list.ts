import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { Vecino } from '../../../../core/models/vecino.model';
import { VecinoService } from '../../../../core/services/vecino';
import { Comunidad } from '../../../../core/models/comunidad.model';
import { ComunidadService } from '../../../../core/services/comunidad';

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
  private comunidadService = inject(ComunidadService);
  private cdr = inject(ChangeDetectorRef);

  comunidadId = 0;
  comunidad?: Comunidad;
  vecinos: Vecino[] = [];

  cargando = true;
  error = '';

  paginaActual = 1;
  tamanioPagina = 10;
  totalPaginas = 0;
  totalElementos = 0;

  estadoFiltro = 'activos';

  ngOnInit(): void {
    this.comunidadId = Number(
      this.route.snapshot.paramMap.get('id')
    );

    this.cargarComunidad();
    this.cargarVecinos();
  }

  cargarComunidad(): void {
    this.comunidadService
      .getComunidad(this.comunidadId)
      .subscribe({
        next: (data) => {
          this.comunidad = data;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando comunidad:', err);
        }
      });
  }

  cargarVecinos(): void {
    this.cargando = true;
    this.error = '';

    this.vecinoService
      .getVecinosPorComunidad(
        this.comunidadId,
        this.paginaActual - 1,
        this.tamanioPagina,
        this.estadoFiltro
      )
      .subscribe({
        next: (data) => {
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

  cambiarEstadoFiltro(estado: string): void {
    this.estadoFiltro = estado;
    this.paginaActual = 1;
    this.cargarVecinos();
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

  editarVecino(id: number | undefined): void {
    if (!id) {
      return;
    }

    this.router.navigate(['/vecinos/editar', id]);
  }

  nuevoVecino(): void {
    this.router.navigate(['/vecinos/nuevo/comunidad', this.comunidadId]);
  }

  eliminarVecino(id: number | undefined): void {

    if (!id) {
      return;
    }

    const confirmar = confirm(
      '¿Desea dar de baja este propietario?'
    );

    if (!confirmar) {
      return;
    }

    this.vecinoService
      .eliminarVecino(id)
      .subscribe({
        next: () => {
          this.cargarVecinos();
        },
        error: (err) => {
          console.error(err);

          this.error =
            'No se pudo dar de baja el propietario.';

          this.cdr.detectChanges();
        }
      });
  }
}
