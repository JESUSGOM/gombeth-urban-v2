import {
  Component,
  DestroyRef,
  OnInit,
  inject
} from '@angular/core';

import {
  CommonModule,
  DecimalPipe
} from '@angular/common';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  finalize
} from 'rxjs';

import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';

import {
  RemesaDetalle as RemesaDetalleModel,
  RemesaLineaDetalle
} from '../../../../core/models/remesa-detalle.model';

import {
  RemesaService
} from '../../../../core/services/remesa.service';

import {
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

type CampoOrdenacion =
  | 'vecino'
  | 'reciboContableId'
  | 'importe'
  | 'domiciliado'
  | 'incluidoSepa';

@Component({
  selector: 'app-remesa-detalle',
  standalone: true,
  imports: [
    CommonModule,
    DecimalPipe
  ],
  templateUrl: './remesa-detalle.html',
  styleUrl: './remesa-detalle.scss'
})
export class RemesaDetalle implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private remesaService = inject(RemesaService);
  private comunidadState = inject(ComunidadStateService);
  private destroyRef = inject(DestroyRef);

  remesaId = 0;

  detalle: RemesaDetalleModel | null = null;

  lineasOrdenadas: RemesaLineaDetalle[] = [];
  lineasPaginadas: RemesaLineaDetalle[] = [];

  cargando = false;
  validando = false;

  error = '';
  mensajeValidacion = '';
  erroresValidacion: string[] = [];

  paginaActual = 1;
  tamanioPagina = 10;

  opcionesTamPagina = [
    10,
    15,
    25,
    50,
    100
  ];

  totalPaginas = 0;

  campoOrdenacion: CampoOrdenacion =
    'vecino';

  direccionOrdenacion:
    'asc' | 'desc' = 'asc';

  ngOnInit(): void {
    this.remesaId = Number(
      this.route.snapshot.paramMap.get('id')
    );

    if (
      !Number.isInteger(this.remesaId) ||
      this.remesaId <= 0
    ) {
      this.error =
        'No se ha recibido un identificador de remesa válido.';

      return;
    }

    /*
     * Si el usuario cambia el combo superior mientras está
     * en el detalle, se abre el listado de la nueva comunidad.
     */
    this.comunidadState.init();

    this.comunidadState.comunidad$
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(comunidad => {
        if (
          !this.detalle ||
          !comunidad ||
          comunidad.id === this.detalle.comunidadId
        ) {
          return;
        }

        void this.router.navigate(
          [
            '/remesas/comunidad',
            comunidad.id
          ],
          {
            replaceUrl: true
          }
        );
      });

    this.cargarDetalle();
  }

  cargarDetalle(): void {
    this.cargando = true;
    this.error = '';

    this.remesaService
      .getDetalle(this.remesaId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),

        finalize(() => {
          this.cargando = false;
        })
      )
      .subscribe({
        next: detalle => {
          this.detalle = detalle;

          /*
           * Sincroniza automáticamente el combo superior
           * con la comunidad de la remesa.
           */
          this.comunidadState.setComunidad({
            id: detalle.comunidadId,
            nombre: detalle.comunidad
          });

          this.paginaActual = 1;

          this.aplicarOrdenacionYPaginacion();
        },

        error: error => {
          console.error(
            'Error cargando el detalle de la remesa:',
            error
          );

          this.detalle = null;
          this.lineasOrdenadas = [];
          this.lineasPaginadas = [];

          if (error?.status === 404) {
            this.error =
              'No existe la remesa solicitada.';

            return;
          }

          this.error =
            error?.error?.message ||
            error?.error?.detail ||
            'No se pudo cargar el detalle de la remesa.';
        }
      });
  }

  volver(): void {
    if (!this.detalle?.comunidadId) {
      void this.router.navigate([
        '/remesas'
      ]);

      return;
    }

    void this.router.navigate([
      '/remesas/comunidad',
      this.detalle.comunidadId
    ]);
  }

  descargarXml(): void {
    this.remesaService
      .descargarXml(this.remesaId);
  }

  descargarC19(): void {
    this.remesaService
      .descargarC19(this.remesaId);
  }

  validar(): void {
    this.validando = true;

    this.mensajeValidacion = '';
    this.erroresValidacion = [];

    this.remesaService
      .validarRemesa(this.remesaId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),

        finalize(() => {
          this.validando = false;
        })
      )
      .subscribe({
        next: resultado => {
          if (resultado.valida) {
            this.mensajeValidacion =
              'La remesa ha sido validada correctamente.';

            return;
          }

          this.mensajeValidacion =
            'La remesa contiene errores de validación.';

          this.erroresValidacion =
            resultado.mensajes ?? [];
        },

        error: error => {
          console.error(
            'Error validando la remesa:',
            error
          );

          this.mensajeValidacion =
            'No se pudo validar la remesa.';
        }
      });
  }

  ordenarPor(
    campo: CampoOrdenacion
  ): void {

    if (this.campoOrdenacion === campo) {
      this.direccionOrdenacion =
        this.direccionOrdenacion === 'asc'
          ? 'desc'
          : 'asc';
    } else {
      this.campoOrdenacion = campo;
      this.direccionOrdenacion = 'asc';
    }

    this.paginaActual = 1;

    this.aplicarOrdenacionYPaginacion();
  }

  getIndicadorOrden(
    campo: CampoOrdenacion
  ): string {

    if (this.campoOrdenacion !== campo) {
      return '⇅';
    }

    return this.direccionOrdenacion === 'asc'
      ? '▲'
      : '▼';
  }

  cambiarTamPagina(
    event: Event
  ): void {

    const select =
      event.target as HTMLSelectElement;

    const tamanio =
      Number(select.value);

    if (
      Number.isNaN(tamanio) ||
      tamanio <= 0
    ) {
      return;
    }

    this.tamanioPagina = tamanio;
    this.paginaActual = 1;

    this.aplicarPaginacion();
  }

  irPrimera(): void {
    if (this.paginaActual === 1) {
      return;
    }

    this.paginaActual = 1;
    this.aplicarPaginacion();
  }

  irAnterior(): void {
    if (this.paginaActual <= 1) {
      return;
    }

    this.paginaActual--;
    this.aplicarPaginacion();
  }

  irSiguiente(): void {
    if (
      this.paginaActual >=
      this.totalPaginas
    ) {
      return;
    }

    this.paginaActual++;
    this.aplicarPaginacion();
  }

  irUltima(): void {
    if (this.totalPaginas <= 0) {
      return;
    }

    this.paginaActual =
      this.totalPaginas;

    this.aplicarPaginacion();
  }

  get inicioRegistro(): number {
    if (this.lineasOrdenadas.length === 0) {
      return 0;
    }

    return (
      (this.paginaActual - 1) *
      this.tamanioPagina
    ) + 1;
  }

  get finRegistro(): number {
    const fin =
      this.paginaActual *
      this.tamanioPagina;

    return Math.min(
      fin,
      this.lineasOrdenadas.length
    );
  }

  textoSiNo(
    valor: boolean | null | undefined
  ): string {

    return valor ? 'Sí' : 'No';
  }

  private aplicarOrdenacionYPaginacion(): void {
    this.aplicarOrdenacion();
    this.aplicarPaginacion();
  }

  private aplicarOrdenacion(): void {
    const lineas =
      [...(this.detalle?.lineas ?? [])];

    lineas.sort((a, b) => {
      const valorA =
        this.obtenerValorOrdenable(a);

      const valorB =
        this.obtenerValorOrdenable(b);

      let resultado: number;

      if (
        typeof valorA === 'number' &&
        typeof valorB === 'number'
      ) {
        resultado = valorA - valorB;
      } else {
        resultado = String(valorA ?? '')
          .localeCompare(
            String(valorB ?? ''),
            'es',
            {
              sensitivity: 'base',
              numeric: true
            }
          );
      }

      return this.direccionOrdenacion === 'asc'
        ? resultado
        : -resultado;
    });

    this.lineasOrdenadas = lineas;
  }

  private aplicarPaginacion(): void {
    this.totalPaginas = Math.max(
      1,
      Math.ceil(
        this.lineasOrdenadas.length /
        this.tamanioPagina
      )
    );

    if (
      this.paginaActual >
      this.totalPaginas
    ) {
      this.paginaActual =
        this.totalPaginas;
    }

    const inicio =
      (this.paginaActual - 1) *
      this.tamanioPagina;

    this.lineasPaginadas =
      this.lineasOrdenadas.slice(
        inicio,
        inicio + this.tamanioPagina
      );
  }

  private obtenerValorOrdenable(
    linea: RemesaLineaDetalle
  ): string | number {

    switch (this.campoOrdenacion) {
      case 'vecino':
        return linea.vecino ?? '';

      case 'reciboContableId':
        return linea.reciboContableId ?? 0;

      case 'importe':
        return Number(linea.importe ?? 0);

      case 'domiciliado':
        return linea.domiciliado ? 1 : 0;

      case 'incluidoSepa':
        return linea.incluidoSepa ? 1 : 0;

      default:
        return '';
    }
  }
}
