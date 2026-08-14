import {
  Component,
  DestroyRef,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  Subscription
} from 'rxjs';

import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';

import {
  Recibo
} from '../../../../core/models/recibo.model';

import {
  ReciboService
} from '../../../../core/services/recibo';

import {
  ComunidadService
} from '../../../../core/services/comunidad';

import {
  RemesaService
} from '../../../../core/services/remesa.service';

import {
  CuentaPresentador
} from '../../../../core/models/cuenta-presentador.model';

import {
  CuentaPresentadorService
} from '../../../../core/services/cuenta-presentador.service';

import {
  ComunidadSeleccionada,
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

@Component({
  selector: 'app-recibos-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './recibos-list.html',
  styleUrl: './recibos-list.scss'
})
export class RecibosList implements OnInit {

  private reciboService =
    inject(ReciboService);

  private remesaService =
    inject(RemesaService);

  private cuentaPresentadorService =
    inject(CuentaPresentadorService);

  private comunidadService =
    inject(ComunidadService);

  private comunidadState =
    inject(ComunidadStateService);

  private route =
    inject(ActivatedRoute);

  private router =
    inject(Router);

  private destroyRef =
    inject(DestroyRef);

  /*
   * Peticiones activas.
   *
   * Se cancelan cuando el usuario cambia rápidamente
   * de comunidad.
   */
  private cargaRecibosActual?: Subscription;
  private cargaComunidadActual?: Subscription;

  /*
   * Durante la apertura de /recibos/comunidad/:id
   * ignoramos momentáneamente una comunidad antigua
   * que pudiera estar guardada en localStorage.
   */
  private sincronizandoRuta = false;

  paginaActual = 1;
  registrosPorPagina = 10;

  comunidadId = 0;
  nombreComunidad = '';

  cuentasPresentador: CuentaPresentador[] = [];
  cuentaPresentadorId: number | null = null;
  cargandoCuentasPresentador = false;
  errorCuentasPresentador = '';

  campoOrden: keyof Recibo =
    'fechaEmision';

  direccionOrden:
    'asc' | 'desc' = 'desc';

  recibosSeleccionados =
    new Set<number>();

  estadoFiltro = '';

  fechaDesde = '';
  fechaHasta = '';

  importeMinimo: number | null = null;
  importeMaximo: number | null = null;

  recibos: Recibo[] = [];

  cargando = false;
  error = '';

  procesandoReciboId: number | null = null;
  mensajeOperacion = '';
  errorOperacion = '';

  ngOnInit(): void {
    this.cargarCuentasPresentador();

    const comunidadIdRuta =
      this.obtenerComunidadIdRuta();

    /*
     * Si la URL contiene una comunidad, impedimos que
     * primero se cargue la comunidad antigua almacenada.
     */
    this.sincronizandoRuta =
      comunidadIdRuta !== null;

    this.comunidadState.init();

    /*
     * Escucha permanentemente el combo superior.
     */
    this.comunidadState.comunidad$
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(comunidad => {
        if (
          this.sincronizandoRuta
        ) {
          return;
        }

        if (
          !comunidad ||
          !comunidad.id
        ) {
          this.limpiarPantalla();
          return;
        }

        this.activarComunidad(
          comunidad,
          true
        );
      });

    /*
     * Cuando se entra desde Comunidades mediante el botón R,
     * activa inmediatamente la comunidad de la URL.
     */
    if (comunidadIdRuta !== null) {
      this.activarComunidadDesdeRuta(
        comunidadIdRuta
      );
    }
  }

  private cargarCuentasPresentador(): void {
    this.cargandoCuentasPresentador = true;
    this.errorCuentasPresentador = '';

    this.cuentaPresentadorService
      .listarActivas()
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: cuentas => {
          this.cuentasPresentador =
            cuentas ?? [];

          if (
            this.cuentasPresentador.length > 0
          ) {
            this.cuentaPresentadorId =
              this.cuentasPresentador[0].id;
          } else {
            this.cuentaPresentadorId = null;

            this.errorCuentasPresentador =
              'No existen cuentas presentadoras activas.';
          }

          this.cargandoCuentasPresentador =
            false;
        },

        error: error => {
          console.error(
            'Error cargando cuentas presentadoras:',
            error
          );

          this.cuentasPresentador = [];
          this.cuentaPresentadorId = null;

          this.errorCuentasPresentador =
            error?.error?.message ||
            error?.error ||
            'No se pudieron cargar las cuentas presentadoras.';

          this.cargandoCuentasPresentador =
            false;
        }
      });
  }

  /**
   * Lee rutas como:
   *
   * /recibos/comunidad/3
   */
  private obtenerComunidadIdRuta(): number | null {
    const valor =
      Number(
        this.route.snapshot.paramMap.get('id')
      );

    if (
      !Number.isInteger(valor) ||
      valor <= 0
    ) {
      return null;
    }

    return valor;
  }

  /**
   * Activa una comunidad recibida desde la URL.
   *
   * Los recibos comienzan a cargarse inmediatamente,
   * sin esperar a una segunda selección en el combo.
   */
  private activarComunidadDesdeRuta(
    comunidadId: number
  ): void {
    const seleccionInicial: ComunidadSeleccionada = {
      id: comunidadId,
      nombre: `Comunidad ${comunidadId}`
    };

    this.activarComunidad(
      seleccionInicial,
      false
    );

    /*
     * Una vez aplicada la comunidad de la URL,
     * ya permitimos cambios desde el combo superior.
     */
    this.sincronizandoRuta = false;

    this.cargarDatosComunidad(
      comunidadId
    );
  }

  /**
   * Cambia la comunidad mostrada y carga sus recibos.
   */
  private activarComunidad(
    comunidad: ComunidadSeleccionada,
    actualizarUrl: boolean
  ): void {
    const comunidadId =
      Number(comunidad.id);

    if (
      !Number.isInteger(comunidadId) ||
      comunidadId <= 0
    ) {
      return;
    }

    const cambiaComunidad =
      this.comunidadId !== comunidadId;

    this.comunidadId =
      comunidadId;

    if (
      comunidad.nombre &&
      comunidad.nombre.trim() !== ''
    ) {
      this.nombreComunidad =
        comunidad.nombre;
    }

    /*
     * Mantiene la dirección del navegador asociada
     * a la comunidad actualmente seleccionada.
     */
    if (actualizarUrl) {
      void this.router.navigate(
        [
          '/recibos/comunidad',
          comunidadId
        ],
        {
          replaceUrl: true
        }
      );
    }

    /*
     * Cuando solo cambia el nombre de la misma comunidad,
     * no repetimos la consulta de recibos.
     */
    if (!cambiaComunidad) {
      return;
    }

    this.cancelarPeticiones();

    this.recibosSeleccionados.clear();
    this.recibos = [];

    this.paginaActual = 1;
    this.error = '';

    this.cargarRecibos();
  }

  /**
   * Obtiene el nombre real de la comunidad indicada
   * en la URL y sincroniza el combo superior.
   */
  private cargarDatosComunidad(
    comunidadId: number
  ): void {
    this.cargaComunidadActual?.unsubscribe();

    this.cargaComunidadActual =
      this.comunidadService
        .getComunidad(comunidadId)
        .pipe(
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: comunidad => {
            /*
             * El usuario puede haber cambiado de comunidad
             * mientras llegaba esta respuesta.
             */
            if (
              this.comunidadId !== comunidadId
            ) {
              return;
            }

            const nombre =
              comunidad.nombre ||
              `Comunidad ${comunidadId}`;

            this.nombreComunidad =
              nombre;

            /*
             * Al actualizar el estado global,
             * el combo superior seleccionará automáticamente
             * esta comunidad.
             */
            this.comunidadState.setComunidad({
              id: comunidadId,
              nombre
            });
          },

          error: error => {
            console.error(
              'Error cargando la comunidad:',
              error
            );

            if (
              this.comunidadId !== comunidadId
            ) {
              return;
            }

            const nombreTemporal =
              `Comunidad ${comunidadId}`;

            this.nombreComunidad =
              nombreTemporal;

            /*
             * Incluso cuando no se puede recuperar el nombre,
             * dejamos sincronizado el identificador.
             */
            this.comunidadState.setComunidad({
              id: comunidadId,
              nombre: nombreTemporal
            });
          }
        });
  }

  cargarRecibos(): void {
    if (this.comunidadId <= 0) {
      this.limpiarPantalla();
      return;
    }

    this.cargaRecibosActual?.unsubscribe();

    const comunidadSolicitada =
      this.comunidadId;

    this.cargando = true;
    this.error = '';

    this.cargaRecibosActual =
      this.reciboService
        .getRecibos(comunidadSolicitada)
        .pipe(
          takeUntilDestroyed(this.destroyRef)
        )
        .subscribe({
          next: data => {
            /*
             * Ignora respuestas antiguas cuando el usuario
             * ya ha cambiado de comunidad.
             */
            if (
              this.comunidadId !==
              comunidadSolicitada
            ) {
              return;
            }

            this.recibos =
              data ?? [];

            this.paginaActual = 1;
            this.cargando = false;
          },

          error: error => {
            if (
              this.comunidadId !==
              comunidadSolicitada
            ) {
              return;
            }

            console.error(
              'Error cargando recibos:',
              error
            );

            this.recibos = [];

            this.error =
              error?.error?.message ||
              error?.error ||
              'No se pudieron cargar los recibos.';

            this.cargando = false;
          }
        });
  }

  private aplicarFiltros(
    recibos: Recibo[]
  ): Recibo[] {
    let resultado =
      [...recibos];

    if (this.estadoFiltro) {
      resultado = resultado.filter(
        recibo =>
          recibo.estado ===
          this.estadoFiltro
      );
    }

    if (this.fechaDesde) {
      resultado = resultado.filter(
        recibo =>
          recibo.fechaEmision >=
          this.fechaDesde
      );
    }

    if (this.fechaHasta) {
      resultado = resultado.filter(
        recibo =>
          recibo.fechaEmision <=
          this.fechaHasta
      );
    }

    if (this.importeMinimo !== null) {
      resultado = resultado.filter(
        recibo =>
          recibo.importe >=
          Number(this.importeMinimo)
      );
    }

    if (this.importeMaximo !== null) {
      resultado = resultado.filter(
        recibo =>
          recibo.importe <=
          Number(this.importeMaximo)
      );
    }

    return resultado;
  }

  get totalPaginas(): number {
    const cantidad =
      this.aplicarFiltros(
        this.recibos
      ).length;

    return Math.max(
      1,
      Math.ceil(
        cantidad /
        this.registrosPorPagina
      )
    );
  }

  get recibosPaginados(): Recibo[] {
    const resultado =
      this.aplicarFiltros(
        this.recibos
      );

    const inicio =
      (this.paginaActual - 1) *
      this.registrosPorPagina;

    const fin =
      inicio +
      this.registrosPorPagina;

    return resultado.slice(
      inicio,
      fin
    );
  }

  cambiarPagina(
    pagina: number
  ): void {
    if (
      pagina < 1 ||
      pagina > this.totalPaginas
    ) {
      return;
    }

    this.paginaActual = pagina;
  }

  ordenar(
    campo: keyof Recibo
  ): void {
    if (
      this.campoOrden === campo
    ) {
      this.direccionOrden =
        this.direccionOrden === 'asc'
          ? 'desc'
          : 'asc';
    } else {
      this.campoOrden = campo;
      this.direccionOrden = 'asc';
    }

    this.recibos.sort(
      (a: Recibo, b: Recibo) => {
        const valorA =
          a[campo];

        const valorB =
          b[campo];

        if (
          valorA === null ||
          valorA === undefined
        ) {
          return 1;
        }

        if (
          valorB === null ||
          valorB === undefined
        ) {
          return -1;
        }

        let resultado = 0;

        if (
          typeof valorA === 'number' &&
          typeof valorB === 'number'
        ) {
          resultado =
            valorA - valorB;
        } else {
          resultado =
            String(valorA).localeCompare(
              String(valorB),
              'es',
              {
                sensitivity: 'base',
                numeric: true
              }
            );
        }

        return this.direccionOrden === 'asc'
          ? resultado
          : -resultado;
      }
    );

    this.paginaActual = 1;
  }

  get totalSeleccionado(): number {
    return this.recibos
      .filter(recibo =>
        this.recibosSeleccionados.has(
          recibo.id
        )
      )
      .reduce(
        (total, recibo) =>
          total + recibo.importe,
        0
      );
  }

  toggleRecibo(
    id: number
  ): void {
    if (
      this.recibosSeleccionados.has(id)
    ) {
      this.recibosSeleccionados.delete(id);
    } else {
      this.recibosSeleccionados.add(id);
    }
  }

  estaSeleccionado(
    id: number
  ): boolean {
    return this.recibosSeleccionados.has(id);
  }

  seleccionarTodos(
    event: Event
  ): void {
    const marcado =
      (event.target as HTMLInputElement)
        .checked;

    if (marcado) {
      this.recibosPaginados.forEach(
        recibo => {
          this.recibosSeleccionados.add(
            recibo.id
          );
        }
      );
    } else {
      this.recibosPaginados.forEach(
        recibo => {
          this.recibosSeleccionados.delete(
            recibo.id
          );
        }
      );
    }
  }

  private obtenerFechaCobroRemesaSeleccionada(): string {
    const fechaHoy =
      this.obtenerFechaHoyLocal();

    const fechaEmisionMaxima =
      this.recibos
        .filter(recibo =>
          this.recibosSeleccionados.has(
            recibo.id
          )
        )
        .map(recibo =>
          recibo.fechaEmision
        )
        .filter(fecha =>
          Boolean(fecha)
        )
        .reduce(
          (
            maxima,
            fecha
          ) =>
            fecha > maxima
              ? fecha
              : maxima,
          ''
        );

    if (
      fechaEmisionMaxima !== ''
      && fechaEmisionMaxima > fechaHoy
    ) {
      return fechaEmisionMaxima;
    }

    return fechaHoy;
  }

  private obtenerFechaHoyLocal(): string {
    const hoy = new Date();

    const anio =
      hoy.getFullYear();

    const mes =
      String(
        hoy.getMonth() + 1
      ).padStart(
        2,
        '0'
      );

    const dia =
      String(
        hoy.getDate()
      ).padStart(
        2,
        '0'
      );

    return `${anio}-${mes}-${dia}`;
  }

  generarRemesa(): void {
    if (
      this.recibosSeleccionados.size === 0
    ) {
      alert(
        'Debe seleccionar al menos un recibo'
      );

      return;
    }

    if (
      this.cuentaPresentadorId === null
    ) {
      alert(
        'Debe seleccionar una cuenta presentadora activa.'
      );

      return;
    }

    const fechaCobro =
      this.obtenerFechaCobroRemesaSeleccionada();

    this.remesaService
      .generarRemesaSeleccion(
        this.comunidadId,
        this.cuentaPresentadorId,
        fechaCobro,
        [
          ...this.recibosSeleccionados
        ]
      )
      .pipe(
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: response => {
          alert(
            'Remesa generada correctamente. ID: ' +
            response.remesaId
          );

          this.recibosSeleccionados.clear();

          /*
           * Refresca los recibos después de generar
           * correctamente la remesa.
           */
          this.cargarRecibos();
        },

        error: error => {
          console.error(
            'Error generando remesa:',
            error
          );

          alert(
            'Error generando remesa'
          );
        }
      });
  }


  descargarPdf(
    recibo: Recibo
  ): void {
    if (!recibo.id) {
      return;
    }

    this.procesandoReciboId = recibo.id;
    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.reciboService
      .descargarPdf(recibo.id)
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: blob => {
          this.procesandoReciboId = null;

          const url = URL.createObjectURL(blob);
          const enlace = document.createElement('a');

          enlace.href = url;
          enlace.download =
            `RECIBO_${recibo.id}.pdf`;

          document.body.appendChild(enlace);
          enlace.click();
          enlace.remove();

          URL.revokeObjectURL(url);

          this.mensajeOperacion =
            `PDF del recibo ${recibo.id} descargado correctamente.`;
        },

        error: error => {
          console.error(
            'Error descargando PDF del recibo:',
            error
          );

          this.procesandoReciboId = null;
          this.mensajeOperacion = '';
          this.errorOperacion =
            this.obtenerMensajeError(
              error,
              'No se pudo descargar el PDF del recibo.'
            );
        }
      });
  }

  enviarEmail(
    recibo: Recibo
  ): void {
    if (!recibo.id) {
      return;
    }

    const confirmado = window.confirm(
      '¿Desea enviar por correo electrónico el PDF del recibo ' +
      recibo.id +
      ' al propietario?'
    );

    if (!confirmado) {
      return;
    }

    this.procesandoReciboId = recibo.id;
    this.mensajeOperacion = '';
    this.errorOperacion = '';

    this.reciboService
      .enviarEmail(recibo.id)
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: response => {
          this.procesandoReciboId = null;
          this.mensajeOperacion =
            response.mensaje ||
            'Recibo enviado correctamente por correo.';
        },

        error: error => {
          console.error(
            'Error enviando el recibo por correo:',
            error
          );

          this.procesandoReciboId = null;
          this.mensajeOperacion = '';
          this.errorOperacion =
            this.obtenerMensajeError(
              error,
              'No se pudo enviar el recibo por correo.'
            );
        }
      });
  }

  cobrarRecibo(
    recibo: Recibo
  ): void {
    if (!recibo.id) {
      return;
    }

    if (recibo.estado !== 'PENDIENTE') {
      this.mensajeOperacion = '';
      this.errorOperacion =
        'Solo se pueden cobrar recibos pendientes.';
      return;
    }

    const confirmado = window.confirm(
      '¿Desea registrar manualmente el cobro del recibo ' +
      recibo.id +
      ' por ' +
      recibo.importe.toFixed(2) +
      ' €?'
    );

    if (!confirmado) {
      return;
    }

    this.procesandoReciboId = recibo.id;
    this.mensajeOperacion = '';
    this.errorOperacion = '';

    const fechaCobro =
      new Date()
        .toISOString()
        .split('T')[0];

    this.reciboService
      .cobrarRecibo(
        recibo.id,
        fechaCobro
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: response => {
          this.procesandoReciboId = null;

          this.mensajeOperacion =
            response.mensaje ||
            'Recibo cobrado correctamente.';

          this.recibosSeleccionados.delete(
            recibo.id
          );

          this.cargarRecibos();
        },

        error: error => {
          console.error(
            'Error cobrando recibo:',
            error
          );

          this.procesandoReciboId = null;
          this.mensajeOperacion = '';

          this.errorOperacion =
            this.obtenerMensajeError(
              error,
              'No se pudo registrar el cobro del recibo.'
            );
        }
      });
  }

  anularCobro(
    recibo: Recibo
  ): void {
    if (!recibo.id) {
      return;
    }

    if (recibo.estado !== 'COBRADO') {
      this.mensajeOperacion = '';
      this.errorOperacion =
        'Solo se puede anular un recibo cobrado.';
      return;
    }

    const confirmado = window.confirm(
      '¿Desea anular el cobro del recibo ' +
      recibo.id +
      '? Se generará el asiento contable inverso.'
    );

    if (!confirmado) {
      return;
    }

    this.procesandoReciboId = recibo.id;
    this.mensajeOperacion = '';
    this.errorOperacion = '';

    const fechaAnulacion =
      new Date()
        .toISOString()
        .split('T')[0];

    this.reciboService
      .anularCobro(
        recibo.id,
        fechaAnulacion
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: response => {
          this.procesandoReciboId = null;

          this.mensajeOperacion =
            response.mensaje ||
            'Cobro anulado correctamente.';

          this.recibosSeleccionados.delete(
            recibo.id
          );

          this.cargarRecibos();
        },

        error: error => {
          console.error(
            'Error anulando cobro:',
            error
          );

          this.procesandoReciboId = null;
          this.mensajeOperacion = '';

          this.errorOperacion =
            this.obtenerMensajeError(
              error,
              'No se pudo anular el cobro del recibo.'
            );
        }
      });
  }

  private obtenerMensajeError(
    error: unknown,
    mensajeAlternativo: string
  ): string {
    if (
      typeof error === 'object' &&
      error !== null
    ) {
      const errorHttp =
        error as {
          error?: unknown;
          message?: unknown;
        };

      if (
        typeof errorHttp.error === 'string' &&
        errorHttp.error.trim() !== ''
      ) {
        return errorHttp.error;
      }

      if (
        typeof errorHttp.error === 'object' &&
        errorHttp.error !== null
      ) {
        const cuerpo =
          errorHttp.error as {
            message?: unknown;
            detail?: unknown;
          };

        if (
          typeof cuerpo.message === 'string' &&
          cuerpo.message.trim() !== ''
        ) {
          return cuerpo.message;
        }

        if (
          typeof cuerpo.detail === 'string' &&
          cuerpo.detail.trim() !== ''
        ) {
          return cuerpo.detail;
        }
      }

      if (
        typeof errorHttp.message === 'string' &&
        errorHttp.message.trim() !== ''
      ) {
        return errorHttp.message;
      }
    }

    return mensajeAlternativo;
  }

  private cancelarPeticiones(): void {
    this.cargaRecibosActual?.unsubscribe();
    this.cargaRecibosActual = undefined;

    this.cargaComunidadActual?.unsubscribe();
    this.cargaComunidadActual = undefined;
  }

  private limpiarPantalla(): void {
    this.cancelarPeticiones();

    this.comunidadId = 0;
    this.nombreComunidad = '';

    this.recibos = [];
    this.recibosSeleccionados.clear();

    this.paginaActual = 1;
    this.cargando = false;
    this.error =
      'Seleccione una comunidad en la barra superior.';
  }
}
