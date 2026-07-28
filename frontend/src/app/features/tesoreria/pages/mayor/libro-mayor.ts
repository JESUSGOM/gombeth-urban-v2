import {
  Component,
  DestroyRef,
  OnInit,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  LibroMayor,
  LibroMayorLinea,
  LibroMayorService
} from '../../../../core/services/libro-mayor.service';
import {
  CuentaContable,
  CuentasContablesService
} from '../../../../core/services/cuentas-contables.service';
import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';


@Component({
  selector: 'app-libro-mayor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './libro-mayor.html',
  styleUrl: './libro-mayor.scss'
})
export class LibroMayorComponent implements OnInit {

  private mayorService =
    inject(LibroMayorService);

  private cuentasService =
    inject(CuentasContablesService);

  private comunidadState =
    inject(ComunidadStateService);

  private destroyRef =
    inject(DestroyRef);

  data: LibroMayor | null = null;

  cuentas: CuentaContable[] = [];
  cuentaId: number | null = null;

  ejercicio =
    new Date().getFullYear();

  cargando = false;
  error = '';

  comunidadId: number | null = null;

  paginaActual = 1;
  tamanioPagina = 10;
  tamaniosPagina = [10, 25, 50];

  ngOnInit(): void {
    this.comunidadState.comunidad$
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe(comunidad => {

        if (!comunidad?.id) {
          this.comunidadId = null;
          this.cuentas = [];
          this.cuentaId = null;
          this.data = null;
          this.paginaActual = 1;
          return;
        }

        this.comunidadId = comunidad.id;
        this.cuentaId = null;
        this.data = null;
        this.error = '';
        this.paginaActual = 1;

        this.cargarCuentas();
      });
  }

  get cuentaSeleccionada(): CuentaContable | null {
    return this.cuentas.find(
      cuenta => cuenta.id === this.cuentaId
    ) ?? null;
  }

  get totalLineas(): number {
    return this.data?.lineas?.length ?? 0;
  }

  get totalPaginas(): number {
    if (this.totalLineas === 0) {
      return 1;
    }

    return Math.ceil(
      this.totalLineas / this.tamanioPagina
    );
  }

  get lineasPaginadas(): LibroMayorLinea[] {
    if (!this.data?.lineas?.length) {
      return [];
    }

    const inicio =
      (this.paginaActual - 1) * this.tamanioPagina;

    return this.data.lineas.slice(
      inicio,
      inicio + this.tamanioPagina
    );
  }

  get primerRegistroVisible(): number {
    if (this.totalLineas === 0) {
      return 0;
    }

    return (
      (this.paginaActual - 1) * this.tamanioPagina
    ) + 1;
  }

  get ultimoRegistroVisible(): number {
    return Math.min(
      this.paginaActual * this.tamanioPagina,
      this.totalLineas
    );
  }

  cargarCuentas(): void {
    if (!this.comunidadId) {
      return;
    }

    this.cargando = true;
    this.error = '';

    this.cuentasService
      .listarPorComunidad(
        this.comunidadId
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: cuentas => {
          this.cuentas = [...cuentas]
            .sort((cuentaA, cuentaB) =>
              cuentaA.codigo.localeCompare(
                cuentaB.codigo,
                'es',
                { numeric: true }
              )
            );

          if (this.cuentas.length === 0) {
            this.cuentaId = null;
            this.data = null;
            this.error =
              'La comunidad seleccionada no tiene cuentas contables.';
            this.cargando = false;
            return;
          }

          this.cuentaId =
            this.cuentas[0].id;

          this.cargar();
        },
        error: error => {
          console.error(
            'Error cargando las cuentas contables:',
            error
          );

          this.cuentas = [];
          this.cuentaId = null;
          this.data = null;
          this.error =
            'No se pudieron cargar las cuentas contables de la comunidad.';
          this.cargando = false;
        }
      });
  }

  cargar(): void {
    if (
      !this.comunidadId ||
      !this.cuentaId
    ) {
      return;
    }

    const ejercicio =
      Number(this.ejercicio);

    if (
      !Number.isInteger(ejercicio) ||
      ejercicio < 1900 ||
      ejercicio > 2100
    ) {
      this.data = null;
      this.error =
        'El ejercicio contable no es válido.';
      return;
    }

    this.cargando = true;
    this.error = '';
    this.paginaActual = 1;

    this.mayorService.obtenerMayor(
      this.comunidadId,
      this.cuentaId,
      ejercicio
    )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: (resultado: LibroMayor) => {
          this.data = resultado;
          this.paginaActual = 1;
          this.cargando = false;
        },
        error: error => {
          console.error(
            'Error cargando el Libro Mayor:',
            error
          );

          this.data = null;
          this.error =
            'No se pudo cargar el Libro Mayor.';
          this.cargando = false;
        }
      });
  }

  cambiarTamanioPagina(): void {
    this.paginaActual = 1;
  }

  paginaAnterior(): void {
    if (this.paginaActual > 1) {
      this.paginaActual--;
    }
  }

  paginaSiguiente(): void {
    if (this.paginaActual < this.totalPaginas) {
      this.paginaActual++;
    }
  }
}
