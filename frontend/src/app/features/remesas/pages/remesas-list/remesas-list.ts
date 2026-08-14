import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  HttpClient,
  HttpErrorResponse
} from '@angular/common/http';

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
  CuentaPresentador
} from '../../../../core/models/cuenta-presentador.model';

import {
  CuentaPresentadorService
} from '../../../../core/services/cuenta-presentador.service';

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

  private http =
    inject(HttpClient);

  private remesaService =
    inject(RemesaService);

  private procesoRemesaService =
    inject(ProcesoRemesaService);

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

  private changeDetectorRef =
    inject(ChangeDetectorRef);

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

  cuentasPresentador: CuentaPresentador[] = [];
  cuentaPresentadorId: number | null = null;
  cargandoCuentasPresentador = false;
  errorCuentasPresentador = '';

  generando = false;
  mensaje = '';
  error = '';

  resultadoProceso:
    ProcesoRemesaResponse | null = null;

  resultadoValidacion = '';
  erroresValidacion: string[] = [];
  advertenciasValidacion: string[] = [];

  remesasValidadas =
    new Set<number>();

  paginaActual = 1;
  registrosPorPagina = 10;

  cargando = false;

  ngOnInit(): void {
    this.actualizarFechaCobro();
    this.cargarCuentasPresentador();

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
    this.advertenciasValidacion = [];

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
      this.cuentaPresentadorId === null
    ) {
      this.mensaje =
        'Debe seleccionar una cuenta presentadora activa.';

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

        cuentaPresentadorId:
        this.cuentaPresentadorId,

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

    this.descargarFichero(
      remesa,
      'xml',
      'XML'
    );
  }

  descargarC19(
    remesa: Remesa
  ): void {

    this.descargarFichero(
      remesa,
      'c19',
      'C19'
    );
  }

  private descargarFichero(
    remesa: Remesa,
    extension: 'xml' | 'c19',
    tipo: 'XML' | 'C19'
  ): void {

    if (!remesa.id) {
      return;
    }

    const remesaId =
      remesa.id;

    this.resultadoValidacion = '';
    this.erroresValidacion = [];
    this.advertenciasValidacion = [];

    this.http
      .get(
        `/api/remesas/${remesaId}/${extension}`,
        {
          observe: 'response',
          responseType: 'blob'
        }
      )
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({
        next: response => {

          if (!response.body || response.body.size === 0) {
            this.resultadoValidacion =
              `No se pudo descargar el fichero ${tipo} de la remesa ${remesaId}.`;

            this.erroresValidacion = [
              'El servidor devolvió un fichero vacío.'
            ];

            return;
          }

          const nombreArchivo =
            this.obtenerNombreArchivo(
              response.headers.get(
                'Content-Disposition'
              ),
              `REMESA_${remesaId}.${extension}`
            );

          const url =
            URL.createObjectURL(
              response.body
            );

          const enlace =
            document.createElement('a');

          enlace.href = url;
          enlace.download = nombreArchivo;
          enlace.style.display = 'none';

          document.body.appendChild(
            enlace
          );

          enlace.click();
          enlace.remove();

          setTimeout(
            () => URL.revokeObjectURL(url),
            0
          );

          this.resultadoValidacion =
            `Fichero ${tipo} de la remesa ${remesaId} descargado correctamente.`;
        },

        error: error => {
          void this.mostrarErrorDescarga(
            error,
            remesaId,
            tipo
          );
        }
      });
  }

  private async mostrarErrorDescarga(
    error: unknown,
    remesaId: number,
    tipo: 'XML' | 'C19'
  ): Promise<void> {

    console.error(
      `Error descargando ${tipo}:`,
      error
    );

    let mensaje =
      `No se pudo generar el fichero ${tipo}.`;

    if (error instanceof HttpErrorResponse) {

      if (error.error instanceof Blob) {
        const texto =
          (await error.error.text())
            .trim();

        if (texto) {
          mensaje = texto;
        }

      } else if (
        typeof error.error === 'string' &&
        error.error.trim() !== ''
      ) {
        mensaje =
          error.error.trim();

      } else if (
        error.error?.message
      ) {
        mensaje =
          String(error.error.message);

      } else if (
        error.message
      ) {
        mensaje =
          error.message;
      }
    }

    this.resultadoValidacion =
      `No se pudo descargar el fichero ${tipo} de la remesa ${remesaId}.`;

    this.erroresValidacion = [
      mensaje
    ];

    this.changeDetectorRef.detectChanges();
  }

  private obtenerNombreArchivo(
    contentDisposition: string | null,
    nombreAlternativo: string
  ): string {

    if (!contentDisposition) {
      return nombreAlternativo;
    }

    const coincidenciaUtf8 =
      /filename\*=UTF-8''([^;]+)/i
        .exec(contentDisposition);

    if (coincidenciaUtf8?.[1]) {
      try {
        return decodeURIComponent(
          coincidenciaUtf8[1]
            .replace(/"/g, '')
            .trim()
        );
      } catch {
        return coincidenciaUtf8[1]
          .replace(/"/g, '')
          .trim();
      }
    }

    const coincidenciaNormal =
      /filename="?([^";]+)"?/i
        .exec(contentDisposition);

    return coincidenciaNormal?.[1]
        ?.trim() ||
      nombreAlternativo;
  }

  validarRemesa(
    remesa: Remesa
  ): void {

    if (!remesa.id) {
      return;
    }

    this.resultadoValidacion = '';
    this.erroresValidacion = [];
    this.advertenciasValidacion = [];

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

            this.advertenciasValidacion =
              response.mensajes ?? [];

            this.resultadoValidacion =
              this.advertenciasValidacion.length > 0
                ? 'Remesa ' +
                  remesa.id +
                  ' válida con advertencias.'
                : 'Remesa ' +
                  remesa.id +
                  ' validada correctamente.';

            this.cargarRemesas();

          } else {
            this.remesasValidadas
              .delete(remesa.id!);

            this.resultadoValidacion =
              'Remesa ' +
              remesa.id +
              ' contiene errores.';

            this.erroresValidacion =
              response.mensajes ?? [];
          }
        },

        error: error => {
          this.resultadoValidacion =
            'No se ha podido validar la remesa ' +
            remesa.id +
            '.';

          this.erroresValidacion = [
            error?.error?.message ??
            error?.message ??
            'Error desconocido al validar la remesa.'
          ];
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
