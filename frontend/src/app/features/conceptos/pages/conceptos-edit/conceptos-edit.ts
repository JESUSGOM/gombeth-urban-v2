import {
  AfterViewInit,
  ChangeDetectorRef,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  finalize,
  timeout
} from 'rxjs';

import {
  ConceptosService
} from '../../../../core/services/conceptos.service';

import {
  CuentaContable,
  CuentasContablesService
} from '../../../../core/services/cuentas-contables.service';

@Component({
  selector: 'app-conceptos-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './conceptos-edit.html',
  styleUrl: './conceptos-edit.scss'
})
export class ConceptosEdit
  implements OnInit, AfterViewInit {

  @ViewChild('cuentaSelect')
  cuentaSelect?: ElementRef<HTMLSelectElement>;

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(ConceptosService);
  private cuentasService =
    inject(CuentasContablesService);

  /*
   * Angular 21 utiliza detección de cambios sin ZoneJS
   * en las aplicaciones nuevas.
   *
   * Debemos avisar a Angular cuando una suscripción HTTP
   * modifica las propiedades utilizadas por la plantilla.
   */
  private changeDetector =
    inject(ChangeDetectorRef);

  concepto: any = {
    id: null,
    descripcion: '',
    importe: 0,
    periodicidad: 'MENSUAL',
    comunidadId: null,
    cuentaContableId: null,
    activo: true
  };

  cuentas: CuentaContable[] = [];

  comunidadId = 0;
  conceptoId: number | null = null;

  modoCuenta = false;
  cargando = false;
  cargandoCuentas = false;
  guardando = false;

  error = '';
  mensaje = '';

  ngOnInit(): void {
    this.comunidadId = Number(
      this.route.snapshot.paramMap.get('id')
    );

    const conceptoIdParametro =
      this.route.snapshot.paramMap.get(
        'conceptoId'
      );

    this.conceptoId = conceptoIdParametro
      ? Number(conceptoIdParametro)
      : null;

    this.modoCuenta =
      this.route.snapshot.queryParamMap.get(
        'modo'
      ) === 'cuenta';

    if (
      !Number.isInteger(this.comunidadId) ||
      this.comunidadId <= 0
    ) {
      this.error =
        'No se ha recibido una comunidad válida.';

      this.actualizarVista();
      return;
    }

    this.concepto.comunidadId =
      this.comunidadId;

    this.cargarCuentas();

    if (this.conceptoId) {
      this.cargarConcepto();
    }
  }

  ngAfterViewInit(): void {
    if (this.modoCuenta) {
      setTimeout(() => {
        this.enfocarCuenta();
      });
    }
  }

  cargarConcepto(): void {
    if (!this.conceptoId) {
      return;
    }

    this.cargando = true;
    this.error = '';

    this.actualizarVista();

    this.service
      .getById(this.conceptoId)
      .pipe(
        timeout(15000),

        finalize(() => {
          /*
           * Este cambio antes se realizaba internamente,
           * pero Angular no actualizaba la plantilla.
           */
          this.cargando = false;
          this.actualizarVista();
        })
      )
      .subscribe({
        next: data => {
          if (!data) {
            this.error =
              'No se ha encontrado el concepto solicitado.';

            this.actualizarVista();
            return;
          }

          if (
            Number(data.comunidadId) !==
            this.comunidadId
          ) {
            this.error =
              'El concepto solicitado no pertenece a la comunidad seleccionada.';

            this.actualizarVista();
            return;
          }

          this.concepto = {
            ...this.concepto,
            ...data,

            comunidadId:
            this.comunidadId,

            cuentaContableId:
              data.cuentaContableId === null ||
              data.cuentaContableId === undefined
                ? null
                : Number(
                  data.cuentaContableId
                )
          };

          /*
           * Fuerza el refresco de los campos del formulario.
           */
          this.actualizarVista();

          if (this.modoCuenta) {
            setTimeout(() => {
              this.enfocarCuenta();
            });
          }
        },

        error: error => {
          console.error(
            'Error cargando el concepto:',
            error
          );

          if (error?.name === 'TimeoutError') {
            this.error =
              'El backend no respondió al cargar el concepto en 15 segundos.';

            this.actualizarVista();
            return;
          }

          if (error?.status === 404) {
            this.error =
              'No existe el concepto de cobro solicitado.';

            this.actualizarVista();
            return;
          }

          this.error =
            'No se pudo cargar el concepto de cobro.';

          this.actualizarVista();
        }
      });
  }

  cargarCuentas(): void {
    this.cargandoCuentas = true;

    this.actualizarVista();

    this.cuentasService
      .getCatalogoGlobal(this.comunidadId)
      .pipe(
        timeout(15000),

        finalize(() => {
          this.cargandoCuentas = false;
          this.actualizarVista();
        })
      )
      .subscribe({
        next: data => {
          this.cuentas = [
            ...(data ?? [])
          ].sort((a, b) =>
            String(a.codigo ?? '')
              .localeCompare(
                String(b.codigo ?? ''),
                'es',
                {
                  numeric: true,
                  sensitivity: 'base'
                }
              )
          );

          /*
           * Actualiza inmediatamente el selector
           * de cuentas contables.
           */
          this.actualizarVista();

          if (this.modoCuenta) {
            setTimeout(() => {
              this.enfocarCuenta();
            });
          }
        },

        error: error => {
          console.error(
            'Error cargando cuentas contables:',
            error
          );

          this.cuentas = [];

          if (error?.name === 'TimeoutError') {
            this.error =
              'El backend no respondió al cargar las cuentas contables en 15 segundos.';

            this.actualizarVista();
            return;
          }

          this.error =
            'No se pudo cargar el catálogo global de cuentas contables.';

          this.actualizarVista();
        }
      });
  }

  guardar(): void {
    this.error = '';
    this.mensaje = '';

    const descripcion =
      String(
        this.concepto.descripcion ?? ''
      ).trim();

    if (!descripcion) {
      this.error =
        'La descripción del concepto es obligatoria.';

      this.actualizarVista();
      return;
    }

    const importe =
      Number(this.concepto.importe);

    if (
      Number.isNaN(importe) ||
      importe < 0
    ) {
      this.error =
        'El importe debe ser un número igual o superior a cero.';

      this.actualizarVista();
      return;
    }

    if (!this.concepto.periodicidad) {
      this.error =
        'Debe seleccionar una periodicidad.';

      this.actualizarVista();
      return;
    }

    this.concepto.descripcion =
      descripcion;

    this.concepto.importe =
      importe;

    this.concepto.comunidadId =
      this.comunidadId;
    /*
     * VINCULO_PROPIETARIO_CONCEPTO_V5
     * El vecino solo se informa en altas iniciadas
     * desde la pantalla de conceptos del propietario.
     */
    const vecinoIdContexto = Number(
      this.route.snapshot.queryParamMap.get(
        'vecinoId'
      )
    );

    if (
      !this.conceptoId &&
      Number.isInteger(vecinoIdContexto) &&
      vecinoIdContexto > 0
    ) {
      (this.concepto as any).vecinoId =
        vecinoIdContexto;
    }

    if (
      this.concepto.cuentaContableId === '' ||
      this.concepto.cuentaContableId === undefined
    ) {
      this.concepto.cuentaContableId = null;

    } else if (
      this.concepto.cuentaContableId !== null
    ) {
      this.concepto.cuentaContableId =
        Number(
          this.concepto.cuentaContableId
        );
    }

    this.guardando = true;
    this.actualizarVista();

    this.service
      .save(this.concepto)
      .pipe(
        timeout(15000),

        finalize(() => {
          this.guardando = false;
          this.actualizarVista();
        })
      )
      .subscribe({
        next: conceptoGuardado => {
          console.log(
            'Concepto guardado correctamente:',
            conceptoGuardado
          );

          /*
           * El vídeo confirma que esta navegación funciona.
           */
          void this.navegarAlListado(true)
            .then(navegacionCorrecta => {
              if (!navegacionCorrecta) {
                this.error =
                  'El concepto se guardó, pero no se pudo volver al listado.';

                this.actualizarVista();
              }
            })
            .catch(errorNavegacion => {
              console.error(
                'Error navegando al listado:',
                errorNavegacion
              );

              this.error =
                'El concepto se guardó, pero ocurrió un error al volver al listado.';

              this.actualizarVista();
            });
        },

        error: error => {
          console.error(
            'Error guardando el concepto:',
            error
          );

          if (error?.name === 'TimeoutError') {
            this.error =
              'El backend no respondió al guardar el concepto en 15 segundos.';

            this.actualizarVista();
            return;
          }

          const mensajeBackend =
            error?.error?.message ||
            error?.error?.detail;

          this.error = mensajeBackend
            ? String(mensajeBackend)
            : 'No se pudo guardar el concepto de cobro.';

          this.actualizarVista();
        }
      });
  }

  volver(): void {
    void this.navegarAlListado(false);
  }

  private navegarAlListado(
    replaceUrl: boolean
  ): Promise<boolean> {

    const vecinoIdContexto = Number(
      this.route.snapshot.queryParamMap.get(
        'vecinoId'
      )
    );

    if (
      Number.isInteger(vecinoIdContexto) &&
      vecinoIdContexto > 0 &&
      this.comunidadId > 0
    ) {
      return this.router.navigate(
        [
          '/conceptos/comunidad',
          this.comunidadId,
          'vecino',
          vecinoIdContexto
        ],
        {
          queryParams: {
            propietario:
              this.route.snapshot.queryParamMap.get(
                'propietario'
              ) ?? '',
            vivienda:
              this.route.snapshot.queryParamMap.get(
                'vivienda'
              ) ?? ''
          },
          replaceUrl
        }
      );
    }

    return this.router.navigate(
      [
        '/conceptos/comunidad',
        this.comunidadId
      ],
      {
        replaceUrl
      }
    );
  }
  get tituloPantalla(): string {
    if (!this.conceptoId) {
      return 'Nuevo concepto de cobro';
    }

    if (this.modoCuenta) {
      return 'Asignar cuenta contable';
    }

    return 'Editar concepto de cobro';
  }

  private enfocarCuenta(): void {
    if (!this.cuentaSelect) {
      return;
    }

    this.cuentaSelect.nativeElement.focus();

    this.cuentaSelect.nativeElement
      .scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      });
  }

  /**
   * Notifica a Angular 21 que una suscripción
   * ha modificado datos utilizados en la plantilla.
   */
  private actualizarVista(): void {
    this.changeDetector.markForCheck();
  }
}
