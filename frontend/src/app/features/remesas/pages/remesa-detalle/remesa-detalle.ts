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
  RemesaEvento
} from '../../../../core/models/remesa-evento.model';

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

  eventos: RemesaEvento[] = [];

  cargando = false;
  cargandoEventos = false;
  validando = false;
  presentando = false;
  anulando = false;

  error = '';
  errorEventos = '';
  mensajeOperacion = '';
  errorOperacion = '';
  mensajeValidacion = '';
  erroresValidacion: string[] = [];
  advertenciasValidacion: string[] = [];

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
    this.cargarEventos();
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

  cargarEventos(): void {
    this.cargandoEventos = true;
    this.errorEventos = '';

    this.remesaService
      .getEventos(this.remesaId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),

        finalize(() => {
          this.cargandoEventos = false;
        })
      )
      .subscribe({
        next: eventos => {
          this.eventos = eventos ?? [];
        },

        error: error => {
          console.error(
            'Error cargando el historial de la remesa:',
            error
          );

          this.eventos = [];

          this.errorEventos =
            error?.error?.message ||
            error?.error?.detail ||
            'No se pudo cargar el historial de la remesa.';
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

  get puedeAnular(): boolean {
    const estado =
      this.detalle?.estado;

    return (
      estado === 'GENERADA' ||
      estado === 'VALIDADA' ||
      estado === 'FICHERO_GENERADO'
    );
  }

  get puedePresentar(): boolean {
    return (
      this.detalle?.estado ===
      'FICHERO_GENERADO'
    );
  }

  anular(): void {
    if (
      !this.detalle ||
      !this.puedeAnular ||
      this.anulando
    ) {
      return;
    }

    const confirmado =
      window.confirm(
        '¿Confirmas que quieres anular esta remesa?\n\n' +
        'La remesa quedará marcada como ANULADA y ' +
        'se registrará el evento correspondiente en el historial.\n\n' +
        'Esta opción no está disponible para remesas ya PRESENTADAS.'
      );

    if (!confirmado) {
      return;
    }

    this.anulando = true;
    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.remesaService
      .anularRemesa(this.remesaId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),

        finalize(() => {
          this.anulando = false;
        })
      )
      .subscribe({
        next: resultado => {
          this.mensajeOperacion =
            resultado?.mensaje ||
            'La remesa ha quedado marcada como ANULADA.';

          this.cargarDetalle();
          this.cargarEventos();
        },

        error: error => {
          console.error(
            'Error anulando la remesa:',
            error
          );

          if (error?.status === 409) {
            this.errorOperacion =
              error?.error?.detail ||
              error?.error?.message ||
              'El estado actual de la remesa no permite anularla.';

            return;
          }

          if (error?.status === 403) {
            this.errorOperacion =
              'No tienes permiso para modificar esta remesa.';

            return;
          }

          this.errorOperacion =
            error?.error?.detail ||
            error?.error?.message ||
            'No se pudo anular la remesa.';
        }
      });
  }

  presentar(): void {
    if (
      !this.detalle ||
      !this.puedePresentar ||
      this.presentando
    ) {
      return;
    }

    const confirmado =
      window.confirm(
        '¿Confirmas que esta remesa ya ha sido presentada al banco?\n\n' +
        'Esta acción dejará registrada la remesa como PRESENTADA ' +
        'y añadirá el evento correspondiente al historial.'
      );

    if (!confirmado) {
      return;
    }

    this.presentando = true;
    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.remesaService
      .presentarRemesa(this.remesaId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),

        finalize(() => {
          this.presentando = false;
        })
      )
      .subscribe({
        next: resultado => {
          this.mensajeOperacion =
            resultado?.mensaje ||
            'La remesa ha quedado marcada como PRESENTADA.';

          this.cargarDetalle();
          this.cargarEventos();
        },

        error: error => {
          console.error(
            'Error marcando la remesa como presentada:',
            error
          );

          if (error?.status === 409) {
            this.errorOperacion =
              error?.error?.detail ||
              error?.error?.message ||
              'El estado actual de la remesa no permite marcarla como presentada.';

            return;
          }

          if (error?.status === 403) {
            this.errorOperacion =
              'No tienes permiso para modificar esta remesa.';

            return;
          }

          this.errorOperacion =
            error?.error?.detail ||
            error?.error?.message ||
            'No se pudo marcar la remesa como presentada.';
        }
      });
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
    this.advertenciasValidacion = [];

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
            this.advertenciasValidacion =
              resultado.mensajes ?? [];

            this.mensajeValidacion =
              this.advertenciasValidacion.length > 0
                ? 'La remesa es válida, pero contiene advertencias.'
                : 'La remesa ha sido validada correctamente.';

            /*
             * La validación correcta cambia el estado de la remesa
             * en backend. Recarga el detalle para reflejar inmediatamente
             * el nuevo estado sin perder los mensajes de validación.
             */
            this.cargarDetalle();
            this.cargarEventos();

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

  textoTipoEvento(
    tipoEvento: string | null | undefined
  ): string {

    switch (tipoEvento) {
      case 'REMESA_GENERADA':
        return 'Remesa generada';

      case 'VALIDACION_CORRECTA':
        return 'Validación correcta';

      case 'VALIDACION_INCORRECTA':
        return 'Validación incorrecta';

      case 'XML_GENERADO':
        return 'XML generado';

      case 'XML_DESCARGADO':
        return 'XML descargado';

      case 'C19_GENERADO':
        return 'C19 generado';

      case 'C19_DESCARGADO':
        return 'C19 descargado';

      case 'PRESENTADA':
        return 'Remesa presentada';

      case 'ANULADA':
        return 'Remesa anulada';

      default:
        return tipoEvento || 'Evento';
    }
  }

  textoEstado(
    estado: string | null | undefined
  ): string {

    if (!estado) {
      return '-';
    }

    return estado.replaceAll(
      '_',
      ' '
    );
  }

  claseEvento(
    tipoEvento: string | null | undefined
  ): string {

    switch (tipoEvento) {
      case 'VALIDACION_CORRECTA':
        return 'evento-ok';

      case 'VALIDACION_INCORRECTA':
      case 'ANULADA':
        return 'evento-error';

      case 'PRESENTADA':
        return 'evento-presentada';

      default:
        return 'evento-normal';
    }
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
