import { CommonModule, DecimalPipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  OnInit,
  inject
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';

import {
  ConceptosService,
  ConceptoCobroListado
} from '../../../../core/services/conceptos.service';
import { ComunidadStateService } from '../../../../core/state/comunidad-state.service';

type CampoOrdenacion =
  | 'descripcion'
  | 'importe'
  | 'periodicidad'
  | 'cuentaContableCodigo'
  | 'cuentaContableNombre'
  | 'activo';

@Component({
  selector: 'app-conceptos-list',
  standalone: true,
  imports: [CommonModule, DecimalPipe],
  templateUrl: './conceptos-list.html',
  styleUrl: './conceptos-list.scss'
})
export class ConceptosList implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private service = inject(ConceptosService);
  private comunidadState = inject(ComunidadStateService);
  private destroyRef = inject(DestroyRef);

  private cargaActual?: Subscription;

  comunidadId: number | null = null;

  conceptos: ConceptoCobroListado[] = [];
  conceptosOrdenados: ConceptoCobroListado[] = [];
  conceptosPaginados: ConceptoCobroListado[] = [];

  cargando = false;
  error = '';

  campoOrdenacion: CampoOrdenacion = 'descripcion';
  direccionOrdenacion: 'asc' | 'desc' = 'asc';

  paginaActual = 1;
  tamanioPagina = 10;
  opcionesTamPagina = [10, 15, 25, 50, 100];

  totalRegistros = 0;
  totalPaginas = 0;

  ngOnInit(): void {
    const idRuta = Number(
      this.route.snapshot.paramMap.get('id')
    );

    /*
     * Compatibilidad con la ruta antigua:
     *
     * /conceptos/comunidad/2
     */
    if (Number.isInteger(idRuta) && idRuta > 0) {
      this.comunidadId = idRuta;
      this.cargarConceptos();
      return;
    }

    this.comunidadState.init();

    this.comunidadState.comunidad$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(comunidad => {
        this.cargaActual?.unsubscribe();
        this.cargaActual = undefined;

        if (!comunidad) {
          this.comunidadId = null;
          this.limpiarListado();
          this.cargando = false;
          this.error =
            'Seleccione una comunidad en la barra superior.';
          return;
        }

        this.comunidadId = comunidad.id;
        this.paginaActual = 1;
        this.cargarConceptos();
      });
  }

  cargarConceptos(): void {
    if (!this.comunidadId) {
      return;
    }

    this.cargaActual?.unsubscribe();

    this.limpiarListado();

    this.cargando = true;
    this.error = '';

    const comunidadSolicitada = this.comunidadId;

    this.cargaActual = this.service
      .getByComunidad(comunidadSolicitada)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: data => {
          if (this.comunidadId !== comunidadSolicitada) {
            return;
          }

          this.conceptos = data ?? [];
          this.totalRegistros = this.conceptos.length;

          this.aplicarOrdenacionYPaginacion();

          this.cargando = false;
        },
        error: error => {
          if (this.comunidadId !== comunidadSolicitada) {
            return;
          }

          console.error(
            'Error cargando conceptos:',
            error
          );

          this.limpiarListado();

          this.error =
            'No se pudieron cargar los conceptos de la comunidad.';

          this.cargando = false;
        }
      });
  }

  nuevoConcepto(): void {
    if (!this.comunidadId) {
      this.error =
        'Seleccione una comunidad antes de crear un concepto.';
      return;
    }

    this.router.navigate([
      '/conceptos/comunidad',
      this.comunidadId,
      'nuevo'
    ]);
  }

  editarConcepto(concepto: ConceptoCobroListado): void {
    if (!this.comunidadId || !concepto.id) {
      return;
    }

    this.router.navigate([
      '/conceptos/comunidad',
      this.comunidadId,
      'editar',
      concepto.id
    ]);
  }

  asignarCuenta(concepto: ConceptoCobroListado): void {
    if (!this.comunidadId || !concepto.id) {
      return;
    }

    this.router.navigate(
      [
        '/conceptos/comunidad',
        this.comunidadId,
        'editar',
        concepto.id
      ],
      {
        queryParams: {
          modo: 'cuenta'
        }
      }
    );
  }

  ordenarPor(campo: CampoOrdenacion): void {
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

  cambiarTamPagina(event: Event): void {
    const select =
      event.target as HTMLSelectElement;

    const nuevoTamanio = Number(select.value);

    if (
      Number.isNaN(nuevoTamanio) ||
      nuevoTamanio <= 0
    ) {
      return;
    }

    this.tamanioPagina = nuevoTamanio;
    this.paginaActual = 1;

    this.aplicarOrdenacionYPaginacion();
  }

  irPrimera(): void {
    if (this.paginaActual <= 1) {
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
    if (this.paginaActual >= this.totalPaginas) {
      return;
    }

    this.paginaActual++;
    this.aplicarPaginacion();
  }

  irUltima(): void {
    if (
      this.totalPaginas <= 0 ||
      this.paginaActual >= this.totalPaginas
    ) {
      return;
    }

    this.paginaActual = this.totalPaginas;
    this.aplicarPaginacion();
  }

  get inicioRegistro(): number {
    if (this.totalRegistros === 0) {
      return 0;
    }

    return (
      (this.paginaActual - 1) *
      this.tamanioPagina
    ) + 1;
  }

  get finRegistro(): number {
    const fin =
      this.paginaActual * this.tamanioPagina;

    return fin > this.totalRegistros
      ? this.totalRegistros
      : fin;
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

  getTextoCuentaCodigo(
    concepto: ConceptoCobroListado
  ): string {
    if (
      concepto.cuentaContableCodigo &&
      concepto.cuentaContableCodigo.trim() !== ''
    ) {
      return concepto.cuentaContableCodigo;
    }

    if (concepto.cuentaContableId) {
      return `ID ${concepto.cuentaContableId}`;
    }

    return 'Sin cuenta';
  }

  getTextoCuentaNombre(
    concepto: ConceptoCobroListado
  ): string {
    if (
      concepto.cuentaContableNombre &&
      concepto.cuentaContableNombre.trim() !== ''
    ) {
      return concepto.cuentaContableNombre;
    }

    if (concepto.cuentaContableId) {
      return 'Cuenta no encontrada';
    }

    return 'Sin cuenta';
  }

  private limpiarListado(): void {
    this.conceptos = [];
    this.conceptosOrdenados = [];
    this.conceptosPaginados = [];

    this.totalRegistros = 0;
    this.totalPaginas = 0;
  }

  private aplicarOrdenacionYPaginacion(): void {
    this.aplicarOrdenacion();
    this.aplicarPaginacion();
  }

  private aplicarOrdenacion(): void {
    const copia = [...this.conceptos];

    copia.sort((a, b) => {
      const valorA =
        this.obtenerValorOrdenable(
          a,
          this.campoOrdenacion
        );

      const valorB =
        this.obtenerValorOrdenable(
          b,
          this.campoOrdenacion
        );

      let resultado: number;

      if (
        typeof valorA === 'number' &&
        typeof valorB === 'number'
      ) {
        resultado = valorA - valorB;

      } else if (
        typeof valorA === 'boolean' &&
        typeof valorB === 'boolean'
      ) {
        resultado =
          Number(valorA) - Number(valorB);

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

    this.conceptosOrdenados = copia;
    this.totalRegistros =
      this.conceptosOrdenados.length;

    this.totalPaginas = Math.ceil(
      this.totalRegistros /
      this.tamanioPagina
    );

    if (this.totalPaginas === 0) {
      this.paginaActual = 1;

    } else if (
      this.paginaActual > this.totalPaginas
    ) {
      this.paginaActual = this.totalPaginas;
    }
  }

  private aplicarPaginacion(): void {
    const inicio =
      (this.paginaActual - 1) *
      this.tamanioPagina;

    const fin =
      inicio + this.tamanioPagina;

    this.conceptosPaginados =
      this.conceptosOrdenados.slice(
        inicio,
        fin
      );

    this.totalPaginas = Math.ceil(
      this.totalRegistros /
      this.tamanioPagina
    );
  }

  private obtenerValorOrdenable(
    concepto: ConceptoCobroListado,
    campo: CampoOrdenacion
  ): string | number | boolean {

    switch (campo) {
      case 'descripcion':
        return concepto.descripcion ?? '';

      case 'importe':
        return Number(
          concepto.importe ?? 0
        );

      case 'periodicidad':
        return concepto.periodicidad ?? '';

      case 'cuentaContableCodigo':
        return concepto.cuentaContableCodigo ?? '';

      case 'cuentaContableNombre':
        return concepto.cuentaContableNombre ?? '';

      case 'activo':
        return concepto.activo ?? false;

      default:
        return '';
    }
  }
}
