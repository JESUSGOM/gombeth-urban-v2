import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { ConceptosService } from '../../../../core/services/conceptos.service';
import { CuentasContablesService } from '../../../../core/services/cuentas-contables.service';

@Component({
  selector: 'app-conceptos-edit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './conceptos-edit.html',
  styleUrl: './conceptos-edit.scss'
})
export class ConceptosEdit implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(ConceptosService);
  private cuentasService = inject(CuentasContablesService);

  concepto: any = {
    id: null,
    descripcion: '',
    importe: 0,
    periodicidad: 'MENSUAL',
    cuentaContableId: null,
    activo: true
  };

  cuentas: any[] = [];
  comunidadId!: number;

  ngOnInit(): void {

    this.comunidadId = Number(this.route.snapshot.paramMap.get('id'));

    this.cargarCuentas();

    const conceptoId = this.route.snapshot.paramMap.get('conceptoId');

    if (conceptoId) {
      this.service.getById(Number(conceptoId))
        .subscribe(data => this.concepto = data);
    }
  }

  cargarCuentas(): void {
    this.cuentasService.getByComunidad(this.comunidadId)
      .subscribe(data => this.cuentas = data);
  }

  guardar(): void {

    this.service.save(this.concepto)
      .subscribe(() => {
        this.router.navigate(['/conceptos/comunidad', this.comunidadId]);
      });
  }

  volver(): void {
    this.router.navigate(['/conceptos/comunidad', this.comunidadId]);
  }
}
