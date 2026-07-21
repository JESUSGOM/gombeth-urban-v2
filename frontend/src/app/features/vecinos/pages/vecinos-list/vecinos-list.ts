import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import {
  ActivatedRoute,
  Router
} from '@angular/router';

import { Vecino } from '../../../../core/models/vecino.model';
import { VecinoService } from '../../../../core/services/vecino';
import { Comunidad } from '../../../../core/models/comunidad.model';
import { ComunidadService } from '../../../../core/services/comunidad';
import { CoeficientesResumen } from '../../../../core/models/coeficientes-resumen.model';
import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';

@Component({
  selector: 'app-vecinos-list',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './vecinos-list.html',
  styleUrl: './vecinos-list.scss',
})
export class VecinosList implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private vecinoService = inject(VecinoService);
  private comunidadService = inject(ComunidadService);
  private comunidadState = inject(ComunidadStateService);
  private cdr = inject(ChangeDetectorRef);

  comunidadId = 0;
  comunidad?: Comunidad;
  vecinos: Vecino[] = [];

  cargando = false;
  error = '';

  resumenCoeficientes?: CoeficientesResumen;

  paginaActual = 1;
  tamanioPagina = 10;
  totalPaginas = 0;
  totalElementos = 0;

  estadoFiltro = 'activos';

  private suscripcionEstadoInicializada = false;

  ngOnInit(): void {
    this.inicializarSuscripcionEstado();

    this.route.paramMap.subscribe(parametros => {
      const parametroId = parametros.get('id');

      if (!parametroId) {
        const comunidadActiva =
          this.comunidadState.getComunidad();

        if (comunidadActiva?.id) {
          this.router.navigate(
            [
              '/vecinos/comunidad',
              comunidadActiva.id
            ],
            {
              replaceUrl: true
            }
          );

          return;
        }

        this.error =
          'No hay una comunidad seleccionada.';

        this.comunidadId = 0;
        this.limpiarDatos();
        this.cdr.detectChanges();
        return;
      }

      const comunidadIdParametro =
        Number(parametroId);

      if (
        !Number.isInteger(comunidadIdParametro)
        || comunidadIdParametro <= 0
      ) {
        this.error =
          'El identificador de la comunidad no es válido.';

        this.comunidadId = 0;
        this.limpiarDatos();
        this.cdr.detectChanges();
        return;
      }

      if (this.comunidadId === comunidadIdParametro) {
        return;
      }

      this.comunidadId = comunidadIdParametro;
      this.paginaActual = 1;
      this.estadoFiltro = 'activos';
      this.error = '';

      this.limpiarDatos();
      this.cargarTodo();
    });
  }

  private inicializarSuscripcionEstado(): void {
    if (this.suscripcionEstadoInicializada) {
      return;
    }

    this.suscripcionEstadoInicializada = true;

    this.comunidadState.comunidad$.subscribe(comunidad => {
      if (!comunidad?.id) {
        return;
      }

      if (comunidad.id === this.comunidadId) {
        return;
      }

      this.router.navigate([
        '/vecinos/comunidad',
        comunidad.id
      ]);
    });
  }

  private limpiarDatos(): void {
    this.comunidad = undefined;
    this.resumenCoeficientes = undefined;
    this.vecinos = [];
    this.totalPaginas = 0;
    this.totalElementos = 0;
  }

  private cargarTodo(): void {
    this.cargarComunidad();
    this.cargarResumenCoeficientes();
    this.cargarVecinos();
  }

  cargarComunidad(): void {
    this.comunidadService
      .getComunidad(this.comunidadId)
      .subscribe({
        next: (data: Comunidad) => {
          this.comunidad = data;

          if (!data.id) {
            this.error =
              'La comunidad recibida no tiene un identificador válido.';

            this.cdr.detectChanges();
            return;
          }

          const comunidadActiva =
            this.comunidadState.getComunidad();

          if (comunidadActiva?.id !== data.id) {
            this.comunidadState.setComunidad({
              id: data.id,
              nombre: data.nombre
            });
          }

          this.cdr.detectChanges();
        },
        error: (err: unknown) => {
          console.error(
            'Error cargando comunidad:',
            err
          );

          this.error =
            'No se pudo cargar la comunidad seleccionada.';

          this.cdr.detectChanges();
        }
      });
  }

  cargarResumenCoeficientes(): void {
    this.comunidadService
      .getResumenCoeficientes(this.comunidadId)
      .subscribe({
        next: (data: CoeficientesResumen) => {
          this.resumenCoeficientes = data;
          this.cdr.detectChanges();
        },
        error: (err: unknown) => {
          console.error(
            'Error cargando resumen de coeficientes:',
            err
          );
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
        next: (data: any) => {
          this.vecinos = [
            ...(data.content ?? [])
          ];

          this.totalPaginas =
            data.totalPages ?? 0;

          this.totalElementos =
            data.totalElements ?? 0;

          this.cargando = false;
          this.cdr.detectChanges();
        },
        error: (err: unknown) => {
          console.error(
            'Error cargando propietarios:',
            err
          );

          this.error =
            'No se pudieron cargar los propietarios.';

          this.cargando = false;
          this.cdr.detectChanges();
        }
      });
  }

  cambiarEstadoFiltro(
    estado: string
  ): void {
    this.estadoFiltro = estado;
    this.paginaActual = 1;
    this.cargarVecinos();
  }

  cambiarPagina(
    pagina: number
  ): void {
    if (
      pagina < 1
      || pagina > this.totalPaginas
    ) {
      return;
    }

    this.paginaActual = pagina;
    this.cargarVecinos();
  }

  paginas(): number[] {
    return Array.from(
      {
        length: this.totalPaginas
      },
      (_, indice) => indice + 1
    );
  }

  volver(): void {
    this.router.navigate([
      '/comunidades'
    ]);
  }

  editarVecino(
    id?: number
  ): void {
    if (!id) {
      return;
    }

    this.router.navigate([
      '/vecinos/editar',
      id
    ]);
  }

  nuevoVecino(): void {
    this.router.navigate([
      '/vecinos/nuevo/comunidad',
      this.comunidadId
    ]);
  }

  eliminarVecino(
    id?: number
  ): void {
    if (!id) {
      return;
    }

    const confirmado =
      confirm(
        '¿Desea dar de baja este propietario?'
      );

    if (!confirmado) {
      return;
    }

    this.vecinoService
      .eliminarVecino(id)
      .subscribe({
        next: () => {
          this.cargarResumenCoeficientes();
          this.cargarVecinos();
        },
        error: (err: unknown) => {
          console.error(
            'Error dando de baja al propietario:',
            err
          );

          this.error =
            'No se pudo dar de baja el propietario.';

          this.cdr.detectChanges();
        }
      });
  }

  verMandato(
    id?: number
  ): void {
    if (!id) {
      return;
    }

    window.open(
      `http://localhost:8080/api/vecinos/${id}/mandato-pdf`,
      '_blank'
    );
  }
}
