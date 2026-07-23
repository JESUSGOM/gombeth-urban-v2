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
  Remesa
} from '../../../../core/models/remesa.model';

import {
  RemesaService
} from '../../../../core/services/remesa.service';

import {
  ProcesoRemesaService,
  ProcesoRemesaResponse
} from '../../../../core/services/proceso-remesa.service';

import {
  ComunidadService
} from '../../../../core/services/comunidad';

import {
  ComunidadSeleccionada,
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

@Component({
  selector: 'app-remesas-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './remesas-list.html',
  styleUrl: './remesas-list.scss'
})
export class RemesasList implements OnInit {

  private remesaService =
    inject(RemesaService);

  private procesoRemesaService =
    inject(ProcesoRemesaService);

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

  private cargaRemesasActual?: Subscription;
  private cargaComunidadActual?: Subscription;

  /*
   * Mientras se procesa /remesas/comunidad/:id,
   * se ignora momentáneamente la comunidad anterior
   * almacenada en localStorage.
   */
  private sincronizandoRuta = false;

  comunidadId = 0;
  nombreComunidad = '';

  remesas: Remesa[] = [];

  anio = new Date().getFullYear();
  mes = new Date().getMonth() + 1;
  fechaCobro = '';

  generando = false;
  mensaje = '';
  error = '';

  resultadoProceso:
    ProcesoRemesaResponse | null = null;

  resultadoValidacion = '';
  erroresValidacion: string[] = [];

  remesasValidadas =
    new Set<number>();

  paginaActual = 1;
  registrosPorPagina = 10;

  cargando = false;

  ngOnInit(): void {
    this.actualizarFechaCobro();

    const comunidadIdRuta =
      this.obtenerComunidadIdRuta();

    this.sincronizandoRuta =
      comunidadIdRuta !== null;

    this.comunidadState.init();

    /*
     * Escucha cambios realizados desde el combo superior.
     */
    this.comunidadState.comunidad$
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe(comunidad => {

        if (this.sincronizandoRuta) {
          return;
        }

        if (
          !comunidad ||
          !comunidad.id
        ) {
          this.limpiarPantallaCompleta();
          return;
        }

        this.activarComunidad(
          comunidad,
          true
        );
      });

    /*
     * Cuando se llega desde el botón RM,
     * la comunidad de la URL tiene prioridad.
     */
    if (comunidadIdRuta !== null) {
      this.activarComunidadDesdeRuta(
        comunidadIdRuta
      );
    }
  }

  actualizarFechaCobro(): void {
    const mesTexto =
      String(this.mes)
        .padStart(2, '0');

    this.fechaCobro =
      `${this.anio}-${mesTexto}-05`;
  }

  /**
   * Lee rutas como:
   *
   * /remesas/comunidad/3
   */
  private obtenerComunidadIdRuta():
    number | null {

    const valor = Number(
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
   * Activa inmediatamente la comunidad indicada
   * en la dirección del navegador.
   */
  private activarComunidadDesdeRuta(
    comunidadId: number
  ): void {

    const comunidadTemporal:
      ComunidadSeleccionada = {

      id: comunidadId,
      nombre: `Comunidad ${comunidadId}`
    };

    this.activarComunidad(
      comunidadTemporal,
      false
    );

    /*
     * A partir de este momento se vuelven a admitir
     * cambios realizados desde el combo superior.
     */
    this.sincronizandoRuta = false;

    /*
     * Recuperamos el nombre real de la comunidad
     * y sincronizamos el estado global.
     */
    this.cargarDatosComunidad(
      comunidadId
    );
  }

  /**
   * Activa una comunidad y carga sus remesas.
   */
  private activarComunidad(
    comunidad: ComunidadSeleccionada,
    actualizarUrl: boolean
  ): void {

    const nuevoId =
      Number(comunidad.id);

    if (
      !Number.isInteger(nuevoId) ||
      nuevoId <= 0
    ) {
      return;
    }

    const cambiaComunidad =
      this.comunidadId !== nuevoId;

    this.comunidadId =
      nuevoId;

    if (
      comunidad.nombre &&
      comunidad.nombre.trim() !== ''
    ) {
      this.nombreComunidad =
        comunidad.nombre;
    }

    /*
     * Cuando la comunidad se cambia desde el combo,
     * actualizamos también la URL.
     */
    if (actualizarUrl) {
      void this.router.navigate(
        [
          '/remesas/comunidad',
          nuevoId
        ],
        {
          replaceUrl: true
        }
      );
    }

    /*
     * Si solamente ha llegado el nombre real de
     * la misma comunidad, no repetimos las consultas.
     */
    if (!cambiaComunidad) {
      return;
    }

    this.cancelarPeticiones();

    this.limpiarDatosRemesas();

    this.cargarRemesas();
  }

  /**
   * Recupera el nombre real de la comunidad
   * indicada por la URL.
   */
  private cargarDatosComunidad(
    comunidadId: number
  ): void {

    this.cargaComunidadActual
      ?.unsubscribe();

    this.cargaComunidadActual =
      this.comunidadService
        .getComunidad(comunidadId)
        .pipe(
          takeUntilDestroyed(
            this.destroyRef
          )
        )
        .subscribe({
          next: comunidad => {

            /*
             * Ignora una respuesta antigua cuando
             * ya se ha seleccionado otra comunidad.
             */
            if (
              this.comunidadId !==
              comunidadId
            ) {
              return;
            }

            const nombre =
              comunidad.nombre ||
              `Comunidad ${comunidadId}`;

            this.nombreComunidad =
              nombre;

            /*
             * Esto provoca que el combo superior
             * seleccione automáticamente la comunidad.
             */
            this.comunidadState
              .setComunidad({
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
              this.comunidadId !==
              comunidadId
            ) {
              return;
            }

            const nombreTemporal =
              `Comunidad ${comunidadId}`;

            this.nombreComunidad =
              nombreTemporal;

            this.comunidadState
              .setComunidad({
                id: comunidadId,
                nombre: nombreTemporal
              });
          }
        });
  }

  private limpiarDatosRemesas(): void {
    this.remesas = [];
    this.remesasValidadas.clear();

    this.paginaActual = 1;

    this.mensaje = '';
    this.error = '';

    this.resultadoProceso = null;

    this.resultadoValidacion = '';
    this.erroresValidacion = [];

    this.cargando = false;
  }

  private limpiarPantallaCompleta(): void {
    this.cancelarPeticiones();

    this.comunidadId = 0;
    this.nombreComunidad = '';

    this.limpiarDatosRemesas();

    this.error =
      'Seleccione una comunidad en la barra superior.';
  }

  cargarRemesas(): void {
    if (this.comunidadId <= 0) {
      return;
    }

    this.cargaRemesasActual
      ?.unsubscribe();

    const comunidadSolicitada =
      this.comunidadId;

    this.cargando = true;
    this.error = '';

    this.cargaRemesasActual =
      this.remesaService
        .getRemesas(
          comunidadSolicitada
        )
        .pipe(
          takeUntilDestroyed(
            this.destroyRef
          )
        )
        .subscribe({
          next: data => {

            /*
             * No se muestran respuestas correspondientes
             * a una comunidad anterior.
             */
            if (
              this.comunidadId !==
              comunidadSolicitada
            ) {
              return;
            }

            this.remesas =
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
              'Error cargando remesas:',
              error
            );

            this.remesas = [];

            this.error =
              error?.error?.message ||
              error?.error ||
              'No se pudieron cargar las remesas.';

            this.cargando = false;
          }
        });
  }

  generarRemesa(): void {
    if (this.comunidadId <= 0) {
      this.mensaje =
        'No hay comunidad seleccionada.';

      return;
    }

    if (
      !this.anio ||
      !this.mes ||
      !this.fechaCobro
    ) {
      this.mensaje =
        'Debe indicar año, mes y fecha de cobro.';

      return;
    }

    this.generando = true;
    this.mensaje = '';
    this.error = '';

    this.resultadoProceso = null;

    const comunidadSolicitada =
      this.comunidadId;

    this.procesoRemesaService
      .generarProceso({
        comunidadId:
        comunidadSolicitada,

        anio:
        this.anio,

        mes:
        this.mes,

        fechaCobro:
        this.fechaCobro
      })
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: response => {

          if (
            this.comunidadId !==
            comunidadSolicitada
          ) {
            return;
          }

          this.generando = false;

          this.resultadoProceso =
            response;

          this.mensaje =
            response.mensaje;

          this.cargarRemesas();
        },

        error: error => {

          if (
            this.comunidadId !==
            comunidadSolicitada
          ) {
            return;
          }

          console.error(
            'Error generando proceso de remesa:',
            error
          );

          this.generando = false;

          this.error =
            error?.error?.message ||
            error?.error ||
            'Error generando el proceso de remesa.';
        }
      });
  }

  get totalPaginas(): number {
    return Math.max(
      1,
      Math.ceil(
        this.remesas.length /
        this.registrosPorPagina
      )
    );
  }

  get remesasPaginadas(): Remesa[] {
    const inicio =
      (this.paginaActual - 1) *
      this.registrosPorPagina;

    return this.remesas.slice(
      inicio,
      inicio +
      this.registrosPorPagina
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

    this.paginaActual =
      pagina;
  }

  descargarXml(
    remesa: Remesa
  ): void {

    if (!remesa.id) {
      return;
    }

    window.open(
      `/api/remesas/${remesa.id}/xml`,
      '_blank'
    );
  }

  descargarC19(
    remesa: Remesa
  ): void {

    if (!remesa.id) {
      return;
    }

    window.open(
      `/api/remesas/${remesa.id}/c19`,
      '_blank'
    );
  }

  validarRemesa(
    remesa: Remesa
  ): void {

    if (!remesa.id) {
      return;
    }

    this.resultadoValidacion = '';
    this.erroresValidacion = [];

    this.remesaService
      .validarRemesa(remesa.id)
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: response => {

          if (response.valida) {
            this.remesasValidadas
              .add(remesa.id!);

            this.resultadoValidacion =
              'Remesa ' +
              remesa.id +
              ' validada correctamente.';

          } else {
            this.resultadoValidacion =
              'Remesa ' +
              remesa.id +
              ' contiene errores.';

            this.erroresValidacion =
              response.mensajes ?? [];
          }
        },

        error: error => {
          console.error(
            'Error validando la remesa:',
            error
          );

          this.resultadoValidacion =
            'Error validando la remesa.';
        }
      });
  }

  verDetalleRemesa(
    id: number | undefined
  ): void {

    if (!id) {
      return;
    }

    this.router.navigate([
      '/remesas',
      id
    ]);
  }

  private cancelarPeticiones(): void {
    this.cargaRemesasActual
      ?.unsubscribe();

    this.cargaRemesasActual =
      undefined;

    this.cargaComunidadActual
      ?.unsubscribe();

    this.cargaComunidadActual =
      undefined;
  }
}
