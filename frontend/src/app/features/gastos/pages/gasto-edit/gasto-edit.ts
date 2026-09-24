import {
  Component,
  DestroyRef,
  OnInit,
  inject
} from '@angular/core';

import {
  CommonModule
} from '@angular/common';

import {
  FormBuilder,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import {
  finalize,
  timeout
} from 'rxjs';

import {
  takeUntilDestroyed
} from '@angular/core/rxjs-interop';

import {
  Gasto,
  GastoGuardarRequest
} from '../../../../core/models/gasto.model';

import {
  GastoService
} from '../../../../core/services/gasto.service';

import {
  CuentaContable,
  CuentasContablesService
} from '../../../../core/services/cuentas-contables.service';

import {
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

import {
  ProveedorComunidad
} from '../../../../core/models/proveedor.model';

import {
  ProveedorService
} from '../../../../core/services/proveedor.service';

@Component({
  selector: 'app-gasto-edit',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './gasto-edit.html',
  styleUrl: './gasto-edit.scss'
})
export class GastoEdit implements OnInit {

  private readonly route =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly gastoService =
    inject(GastoService);

  private readonly proveedorService =
    inject(ProveedorService);

  private readonly cuentasService =
    inject(CuentasContablesService);

  private readonly comunidadState =
    inject(ComunidadStateService);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly formBuilder =
    inject(FormBuilder);

  readonly gastoForm =
    this.formBuilder.group({

      comunidadId:
        this.formBuilder.control<number | null>(
          null,
          [
            Validators.required,
            Validators.min(1)
          ]
        ),

      concepto:
        this.formBuilder.nonNullable.control(
          '',
          Validators.required
        ),

      fechaFactura:
        this.formBuilder.nonNullable.control(
          '',
          Validators.required
        ),

      importeTotal:
        this.formBuilder.control<number | null>(
          null,
          [
            Validators.required,
            Validators.min(
              Number.EPSILON
            )
          ]
        ),

      numeroFactura:
        this.formBuilder.nonNullable.control(
          ''
        ),

      proveedor:
        this.formBuilder.nonNullable.control(
          '',
          Validators.required
        ),

      cuentaGastoId:
        this.formBuilder.control<number | null>(
          null,
          [
            Validators.required,
            Validators.min(1)
          ]
        )
    });

  gastoId: number | null = null;

  nombreComunidad = '';

  cuentas: CuentaContable[] = [];

  proveedoresComunidad: ProveedorComunidad[] = [];

  proveedorComunidadSeleccionadoId:
    number | null = null;

  cargando = false;
  cargandoCuentas = false;
  cargandoProveedores = false;
  guardando = false;

  errorProveedores = '';

  bloqueado = false;
  motivoBloqueo = '';

  error = '';
  mensaje = '';

  facturaSeleccionada: File | null =
    null;

  analizandoFactura = false;

  mensajeOcr = '';
  errorOcr = '';

  advertenciasOcr: string[] = [];

  ngOnInit(): void {

    this.comunidadState.init();

    const idParametro =
      this.route.snapshot.paramMap.get(
        'id'
      );

    if (idParametro) {

      const id =
        Number(idParametro);

      if (
        !Number.isInteger(id)
        || id <= 0
      ) {
        this.error =
          'El identificador del gasto no es válido.';

        return;
      }

      this.gastoId = id;

      this.cargarGasto();

      return;
    }

    this.prepararAlta();
  }

  get esEdicion(): boolean {

    return this.gastoId !== null;
  }

  get tituloPantalla(): string {

    return this.esEdicion
      ? 'Editar gasto'
      : 'Nuevo gasto';
  }

  prepararAlta(): void {

    const comunidad =
      this.comunidadState.getComunidad();

    if (
      !comunidad
      || !Number.isInteger(
        Number(comunidad.id)
      )
      || Number(comunidad.id) <= 0
    ) {
      this.error =
        'Seleccione una comunidad antes de crear un gasto.';

      return;
    }

    const comunidadId =
      Number(comunidad.id);

    this.gastoForm.reset({
      comunidadId,
      concepto: '',
      fechaFactura:
        this.fechaHoy(),
      importeTotal: null,
      numeroFactura: '',
      proveedor: '',
      cuentaGastoId: null
    });

    this.nombreComunidad =
      comunidad.nombre;

    this.proveedorComunidadSeleccionadoId =
      null;

    this.cargarProveedores(
      comunidadId
    );

    this.cargarCuentas(
      comunidadId
    );
  }

  cargarGasto(): void {

    if (!this.gastoId) {
      return;
    }

    this.cargando = true;
    this.error = '';
    this.mensaje = '';

    this.gastoService
      .obtener(
        this.gastoId
      )
      .pipe(
        timeout(15000),

        takeUntilDestroyed(
          this.destroyRef
        ),

        finalize(() => {
          this.cargando = false;
        })
      )
      .subscribe({

        next: gasto => {

          this.aplicarGasto(
            gasto
          );
        },

        error: error => {

          console.error(
            'Error cargando gasto:',
            error
          );

          this.error =
            this.mensajeError(
              error,
              'No se pudo cargar el gasto.'
            );
        }
      });
  }

  private aplicarGasto(
    gasto: Gasto
  ): void {

    if (
      !gasto
      || !Number.isInteger(
        Number(gasto.comunidadId)
      )
      || Number(gasto.comunidadId) <= 0
    ) {
      this.error =
        'El gasto recuperado no contiene una comunidad válida.';

      return;
    }

    const comunidadId =
      Number(gasto.comunidadId);

    const seleccionada =
      this.comunidadState.getComunidad();

    this.nombreComunidad =
      seleccionada?.id === comunidadId
        ? seleccionada.nombre
        : `Comunidad ${comunidadId}`;

    this.gastoForm.reset({
      comunidadId,

      concepto:
        gasto.concepto ?? '',

      fechaFactura:
        gasto.fechaFactura ?? '',

      importeTotal:
        gasto.importeTotal === null
        || gasto.importeTotal === undefined
          ? null
          : Number(
            gasto.importeTotal
          ),

      numeroFactura:
        gasto.numeroFactura ?? '',

      proveedor:
        gasto.proveedor ?? '',

      cuentaGastoId:
        gasto.cuentaGastoId === null
        || gasto.cuentaGastoId === undefined
          ? null
          : Number(
            gasto.cuentaGastoId
          )
    });

    this.bloqueado =
      gasto.pagado === true
      || Boolean(
        gasto.numeroAsiento?.trim()
      );

    if (
      gasto.pagado === true
    ) {
      this.motivoBloqueo =
        'Este gasto está pagado. '
        + 'No puede editarse hasta implementar '
        + 'el deshacer pago de forma segura.';

    } else if (
      gasto.numeroAsiento?.trim()
    ) {
      this.motivoBloqueo =
        'Este gasto está contabilizado. '
        + 'No puede editarse hasta implementar '
        + 'la reversión contable segura.';

    } else {
      this.motivoBloqueo = '';
    }

    this.cargarCuentas(
      comunidadId
    );

    this.proveedorComunidadSeleccionadoId =
      null;

    this.cargarProveedores(
      comunidadId
    );
  }

  cargarProveedores(
    comunidadId: number
  ): void {

    this.cargandoProveedores = true;
    this.errorProveedores = '';

    this.proveedorService
      .listarPorComunidad(
        comunidadId
      )
      .pipe(
        timeout(15000),

        takeUntilDestroyed(
          this.destroyRef
        ),

        finalize(() => {
          this.cargandoProveedores =
            false;
        })
      )
      .subscribe({

        next: proveedores => {

          this.proveedoresComunidad =
            [
              ...(proveedores ?? [])
            ].sort(
              (a, b) =>
                String(
                  a.nombre ?? ''
                ).localeCompare(
                  String(
                    b.nombre ?? ''
                  ),
                  'es',
                  {
                    sensitivity: 'base'
                  }
                )
            );
        },

        error: error => {

          console.error(
            'Error cargando proveedores:',
            error
          );

          this.proveedoresComunidad = [];

          this.errorProveedores =
            error?.error?.detail
            || error?.error?.message
            || 'No se pudo cargar el catálogo de proveedores.';
        }
      });
  }

  seleccionarProveedorCatalogo(
    event: Event
  ): void {

    const select =
      event.target as HTMLSelectElement;

    const proveedorId =
      Number(
        select.value
      );

    if (
      !Number.isInteger(
        proveedorId
      )
      || proveedorId <= 0
    ) {
      this.proveedorComunidadSeleccionadoId =
        null;

      return;
    }

    const proveedor =
      this.proveedoresComunidad.find(
        item =>
          Number(
            item.proveedorId
          ) === proveedorId
      );

    if (!proveedor) {
      this.proveedorComunidadSeleccionadoId =
        null;

      return;
    }

    this.proveedorComunidadSeleccionadoId =
      proveedorId;

    this.gastoForm.patchValue({
      proveedor:
        proveedor.nombre ?? ''
    });
  }

  marcarProveedorManual(): void {

    this.proveedorComunidadSeleccionadoId =
      null;
  }

  cargarCuentas(
    comunidadId: number
  ): void {

    this.cargandoCuentas = true;

    this.cuentasService
      .listarPorComunidad(
        comunidadId
      )
      .pipe(
        timeout(15000),

        takeUntilDestroyed(
          this.destroyRef
        ),

        finalize(() => {
          this.cargandoCuentas = false;
        })
      )
      .subscribe({

        next: cuentas => {

          this.cuentas = [
            ...(cuentas ?? [])
          ].sort(
            (a, b) =>
              String(
                a.codigo ?? ''
              ).localeCompare(
                String(
                  b.codigo ?? ''
                ),
                'es',
                {
                  numeric: true,
                  sensitivity: 'base'
                }
              )
          );
        },

        error: error => {

          console.error(
            'Error cargando cuentas:',
            error
          );

          this.cuentas = [];

          this.error =
            this.mensajeError(
              error,
              'No se pudieron cargar las cuentas contables de la comunidad.'
            );
        }
      });
  }

  seleccionarFactura(
    event: Event
  ): void {

    const input =
      event.target as HTMLInputElement;

    const archivo =
      input.files?.[0] ?? null;

    this.facturaSeleccionada =
      null;

    this.mensajeOcr = '';
    this.errorOcr = '';
    this.advertenciasOcr = [];

    if (!archivo) {
      return;
    }

    if (
      archivo.type !== 'application/pdf'
      && !archivo.name
        .toLowerCase()
        .endsWith('.pdf')
    ) {
      this.errorOcr =
        'Seleccione un fichero PDF.';

      input.value = '';

      return;
    }

    this.facturaSeleccionada =
      archivo;
  }

  analizarFactura(): void {

    if (
      this.analizandoFactura
      || this.bloqueado
    ) {
      return;
    }

    const archivo =
      this.facturaSeleccionada;

    if (!archivo) {
      this.errorOcr =
        'Seleccione primero la factura en PDF.';

      return;
    }

    const comunidadId =
      Number(
        this.gastoForm.controls
          .comunidadId.value
      );

    if (
      !Number.isInteger(
        comunidadId
      )
      || comunidadId <= 0
    ) {
      this.errorOcr =
        'La comunidad del gasto no es válida.';

      return;
    }

    this.analizandoFactura = true;

    this.mensajeOcr = '';
    this.errorOcr = '';
    this.advertenciasOcr = [];

    this.gastoService
      .analizarFactura(
        comunidadId,
        archivo
      )
      .pipe(
        timeout(60000),

        takeUntilDestroyed(
          this.destroyRef
        ),

        finalize(() => {
          this.analizandoFactura =
            false;
        })
      )
      .subscribe({

        next: resultado => {

          const datos: {
            proveedor?: string;
            fechaFactura?: string;
            importeTotal?: number;
            numeroFactura?: string;
          } = {};

          if (
            resultado.proveedor
            && resultado.proveedor.trim()
          ) {
            datos.proveedor =
              resultado.proveedor.trim();
          }

          if (
            resultado.fechaFactura
            && resultado.fechaFactura.trim()
          ) {
            datos.fechaFactura =
              resultado.fechaFactura.trim();
          }

          if (
            resultado.importeTotal !== null
            && resultado.importeTotal !== undefined
            && Number.isFinite(
              Number(
                resultado.importeTotal
              )
            )
          ) {
            datos.importeTotal =
              Number(
                resultado.importeTotal
              );
          }

          if (
            resultado.numeroFactura
            && resultado.numeroFactura.trim()
          ) {
            datos.numeroFactura =
              resultado.numeroFactura.trim();
          }

          this.gastoForm.patchValue(
            datos
          );

          if (
            datos.proveedor !== undefined
          ) {
            this.proveedorComunidadSeleccionadoId =
              null;
          }

          this.advertenciasOcr =
            [
              ...(
                resultado.advertencias
                ?? []
              )
            ];

          this.mensajeOcr =
            resultado.ocrAplicado
              ? 'Factura analizada mediante OCR. Revise los datos antes de guardar.'
              : 'Datos extraídos del PDF. Revise los datos antes de guardar.';
        },

        error: error => {

          console.error(
            'Error analizando factura:',
            error
          );

          if (
            error?.name === 'TimeoutError'
          ) {
            this.errorOcr =
              'El análisis de la factura ha tardado demasiado.';

            return;
          }

          this.errorOcr =
            error?.error?.message
            || error?.error?.detail
            || 'No se pudo analizar la factura.';
        }
      });
  }

  guardar(): void {

    if (
      this.bloqueado
      || this.guardando
    ) {
      return;
    }

    this.error = '';
    this.mensaje = '';

    const request =
      this.construirRequest();

    if (!request) {
      return;
    }

    this.guardando = true;

    const operacion =
      this.esEdicion
      && this.gastoId
        ? this.gastoService.actualizar(
          this.gastoId,
          request
        )
        : this.gastoService.crear(
          request
        );

    operacion
      .pipe(
        timeout(15000),

        takeUntilDestroyed(
          this.destroyRef
        ),

        finalize(() => {
          this.guardando = false;
        })
      )
      .subscribe({

        next: () => {

          this.mensaje =
            this.esEdicion
              ? 'Gasto actualizado correctamente.'
              : 'Gasto creado correctamente.';

          setTimeout(() => {

            void this.router.navigate([
              '/gastos'
            ]);

          }, 350);
        },

        error: error => {

          console.error(
            'Error guardando gasto:',
            error
          );

          this.error =
            this.mensajeError(
              error,
              this.esEdicion
                ? 'No se pudo actualizar el gasto.'
                : 'No se pudo crear el gasto.'
            );
        }
      });
  }

  private construirRequest():
    GastoGuardarRequest | null {

    const valor =
      this.gastoForm.getRawValue();

    const comunidadId =
      Number(
        valor.comunidadId
      );

    if (
      !Number.isInteger(
        comunidadId
      )
      || comunidadId <= 0
    ) {
      this.error =
        'La comunidad del gasto no es válida.';

      return null;
    }

    const proveedor =
      String(
        valor.proveedor ?? ''
      ).trim();

    if (!proveedor) {

      this.error =
        'El proveedor es obligatorio.';

      return null;
    }

    const concepto =
      String(
        valor.concepto ?? ''
      ).trim();

    if (!concepto) {

      this.error =
        'El concepto es obligatorio.';

      return null;
    }

    const fechaFactura =
      String(
        valor.fechaFactura ?? ''
      ).trim();

    if (!fechaFactura) {

      this.error =
        'La fecha de factura es obligatoria.';

      return null;
    }

    const importeTotal =
      Number(
        valor.importeTotal
      );

    if (
      !Number.isFinite(
        importeTotal
      )
      || importeTotal <= 0
    ) {
      this.error =
        'El importe debe ser mayor que cero.';

      return null;
    }

    const numeroFactura =
      String(
        valor.numeroFactura ?? ''
      ).trim();

    if (
      valor.cuentaGastoId === null
      || valor.cuentaGastoId === undefined
    ) {
      this.error =
        'La cuenta contable es obligatoria.';

      return null;
    }

    const cuentaGastoId =
      Number(
        valor.cuentaGastoId
      );

    if (
      !Number.isInteger(
        cuentaGastoId
      )
      || cuentaGastoId <= 0
    ) {
      this.error =
        'La cuenta contable seleccionada no es válida.';

      return null;
    }

    return {
      comunidadId,
      concepto,
      fechaFactura,
      importeTotal,
      numeroFactura:
        numeroFactura || null,
      proveedor,
      cuentaGastoId
    };
  }

  volver(): void {

    void this.router.navigate([
      '/gastos'
    ]);
  }

  private fechaHoy(): string {

    const ahora =
      new Date();

    const local =
      new Date(
        ahora.getTime()
        - ahora.getTimezoneOffset()
        * 60000
      );

    return local
      .toISOString()
      .slice(
        0,
        10
      );
  }

  private mensajeError(
    error: any,
    fallback: string
  ): string {

    if (
      error?.name === 'TimeoutError'
    ) {
      return (
        'El backend no respondió '
        + 'en 15 segundos.'
      );
    }

    if (
      error?.status === 401
    ) {
      return (
        'La sesión ha caducado. '
        + 'Vuelva a iniciar sesión.'
      );
    }

    if (
      error?.status === 403
    ) {
      return (
        'No tiene permiso para operar '
        + 'con esta comunidad.'
      );
    }

    if (
      error?.status === 404
    ) {
      return (
        'No existe el gasto solicitado.'
      );
    }

    if (
      error?.status === 409
    ) {
      return (
        error?.error?.detail
        || error?.error?.message
        || 'El gasto no puede editarse en su estado actual.'
      );
    }

    return (
      error?.error?.detail
      || error?.error?.message
      || fallback
    );
  }
}
