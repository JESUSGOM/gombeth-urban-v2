import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ConceptosService } from '../../../../core/services/conceptos.service';
import { CuentasContablesService } from '../../../../core/services/cuentas-contables.service';

@Component({
  selector: 'app-conceptos-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './conceptos-list.html',
  styleUrls: ['./conceptos-list.scss']
})
export class ConceptosList implements OnInit {

  private route = inject(ActivatedRoute);
  private service = inject(ConceptosService);

  comunidadId!: number;
  conceptos: any[] = [];
  cargando = true;
  cuentas: any[] = [];

  private cuentasService = inject(CuentasContablesService);
  error = '';

  ngOnInit(): void {
    this.comunidadId = Number(this.route.snapshot.paramMap.get('id'));
    this.cuentasService.getByComunidad(this.comunidadId)
      .subscribe(data => {
        this.cuentas = data;
      });
    this.load();
  }

  load(): void {
    this.cargando = true;
    this.error = '';

    this.service.getByComunidad(this.comunidadId)
      .subscribe({
        next: (data) => {
          this.conceptos = data ?? [];
          this.cargando = false;
        },
        error: (err) => {
          console.error(err);
          this.error = 'Error cargando conceptos';
          this.cargando = false;
        }
      });
  }
}
