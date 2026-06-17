import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';

import { CommonModule } from '@angular/common';

import { Recibo } from '../../../../core/models/recibo.model';
import { ReciboService } from '../../../../core/services/recibo';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-recibos-list',
  imports: [CommonModule],
  templateUrl: './recibos-list.html',
  styleUrl: './recibos-list.scss'
})
export class RecibosList implements OnInit {

  private reciboService = inject(ReciboService);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);

  paginaActual = 1;
  registrosPorPagina = 10;

  comunidadId!: number;

  recibos: Recibo[] = [];

  cargando = false;
  error = '';

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.comunidadId = Number(params['id']);
      this.cargarRecibos();
    });
  }

  cargarRecibos(): void {
    this.cargando = true;
    this.error = '';

    this.reciboService
      .getRecibos(this.comunidadId)
      .subscribe({
        next: (data) => {
          this.recibos = data;
          this.paginaActual = 1;
          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando recibos:', err);
          this.error =
            err?.error?.message ||
            err?.error ||
            'No se pudieron cargar los recibos.';
          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  get totalPaginas(): number {
    return Math.ceil(this.recibos.length / this.registrosPorPagina);
  }

  get recibosPaginados(): Recibo[] {
    const inicio = (this.paginaActual - 1) * this.registrosPorPagina;
    const fin = inicio + this.registrosPorPagina;
    return this.recibos.slice(inicio, fin);
  }

  cambiarPagina(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas) {
      return;
    }

    this.paginaActual = pagina;
  }
}
