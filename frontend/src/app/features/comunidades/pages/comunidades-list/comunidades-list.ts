import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';

import { Comunidad } from '../../../../core/models/comunidad.model';
import { ComunidadService } from '../../../../core/services/comunidad';
import { Router } from '@angular/router';

@Component({
  selector: 'app-comunidades-list',
  imports: [CommonModule],
  templateUrl: './comunidades-list.html',
  styleUrl: './comunidades-list.scss',
})
export class ComunidadesList implements OnInit {

  private comunidadService = inject(ComunidadService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  comunidades: Comunidad[] = [];

  cargando = true;
  error = '';

  paginaActual = 1;
  tamanioPagina = 10;
  totalPaginas = 0;
  totalElementos = 0;

  ngOnInit(): void {
    this.cargarComunidades();
  }

  cargarComunidades(): void {
    this.cargando = true;
    this.error = '';

    this.comunidadService
      .getComunidades(this.paginaActual - 1, this.tamanioPagina)
      .subscribe({
        next: (data) => {
          console.log('RESPUESTA PAGINADA:', data);
          console.log('CONTENT:', data.content);
          console.log('TOTAL:', data.totalElements);

          this.comunidades = [...(data.content ?? [])];
          this.totalPaginas = data.totalPages ?? 0;
          this.totalElementos = data.totalElements ?? 0;
          this.cargando = false;

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando comunidades:', err);
          this.error = 'No se pudieron cargar las comunidades.';
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
    this.cargarComunidades();
  }

  paginas(): number[] {
    return Array.from(
      { length: this.totalPaginas },
      (_, i) => i + 1
    );
  }

  editarComunidad(id: number | undefined): void {
    if (!id) {
      return;
    }

    this.router.navigate(['/comunidades/editar', id]);
  }

}
