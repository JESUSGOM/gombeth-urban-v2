import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Vecino } from '../../../../core/models/vecino.model';
import { VecinoService } from '../../../../core/services/vecino';

@Component({
  selector: 'app-vecino-edit',
  imports: [CommonModule, FormsModule],
  templateUrl: './vecino-edit.html',
  styleUrl: './vecino-edit.scss',
})
export class VecinoEdit implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private vecinoService = inject(VecinoService);
  private cdr = inject(ChangeDetectorRef);

  vecino?: Vecino;

  esNuevo = false;
  guardando = false;
  mensaje = '';
  error = '';
  archivoSeleccionado?: File;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const comunidadId = this.route.snapshot.paramMap.get('comunidadId');

    if (id) {
      this.cargarVecino(Number(id));
      return;
    }

    if (comunidadId) {
      this.esNuevo = true;

      this.vecino = {
        id: 0,
        comunidadId: Number(comunidadId),
        nombre: '',
        vivienda: '',
        nif: '',
        iban: '',
        bic: '',
        email: '',
        telefono1: '',
        telefono2: '',
        telefono3: '',
        direccion: '',
        poblacion: '',
        provincia: '',
        codigoPostal: '',
        paisCod: 'ES',
        domiciliado: true,
        activo: true
      };

      this.cdr.detectChanges();
    }
  }

  cargarVecino(id: number): void {
    this.vecinoService.getVecino(id).subscribe({
      next: (data) => {
        this.vecino = { ...data };
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando propietario:', err);
        this.error = 'No se pudo cargar el propietario.';
        this.cdr.detectChanges();
      }
    });
  }

  guardar(): void {
    if (!this.vecino) {
      return;
    }

    this.guardando = true;
    this.mensaje = '';
    this.error = '';

    if (this.esNuevo) {
      this.crear();
      return;
    }

    this.actualizar();
  }

  crear(): void {
    if (!this.vecino) {
      return;
    }

    this.vecinoService.crearVecino(this.vecino).subscribe({
      next: (data) => {
        this.mensaje = 'Propietario creado correctamente.';
        this.guardando = false;

        this.router.navigate([
          '/vecinos/comunidad',
          data.comunidadId
        ]);
      },
      error: (err) => {
        console.error('Error creando propietario:', err);
        this.error = 'No se pudo crear el propietario.';
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  actualizar(): void {
    if (!this.vecino || !this.vecino.id) {
      return;
    }

    this.vecinoService.actualizarVecino(this.vecino.id, this.vecino).subscribe({
      next: (data) => {
        this.vecino = { ...data };
        this.mensaje = 'Propietario guardado correctamente.';
        this.guardando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error guardando propietario:', err);
        this.error = 'No se pudo guardar el propietario.';
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  volver(): void {
    if (this.vecino?.comunidadId) {
      this.router.navigate(['/vecinos/comunidad', this.vecino.comunidadId]);
      return;
    }

    this.router.navigate(['/comunidades']);
  }

  seleccionarDocumento(event: Event): void {
    const input = event.target as HTMLInputElement;

    if (!input.files || input.files.length === 0) {
      this.archivoSeleccionado = undefined;
      return;
    }

    this.archivoSeleccionado = input.files[0];
  }

  subirDocumento(): void {
    if (!this.vecino?.id) {
      alert('Primero debe guardar el propietario.');
      return;
    }

    if (!this.archivoSeleccionado) {
      alert('Seleccione un PDF.');
      return;
    }

    const formData = new FormData();

    formData.append('file', this.archivoSeleccionado);

    fetch(`http://localhost:8080/api/documentos/${this.vecino.id}`, {
      method: 'POST',
      body: formData
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Error HTTP ' + response.status);
        }

        alert('Documento subido correctamente.');
      })
      .catch((err) => {
        console.error('Error subiendo documento:', err);
        alert('Error subiendo documento.');
      });
  }
}
