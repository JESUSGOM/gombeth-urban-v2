import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import {
  GestionIncidencia,
  IncidenciasService
} from '../../services/incidencias.service';

import {
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

interface ResumenIncidencias {
  total: number;
  pendientes: number;
  enProceso: number;
  esperandoProveedor: number;
  finalizadas: number;
  cerradas: number;
  canceladas: number;
}

@Component({
  selector: 'app-incidencias-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './incidencias-list.html',
  styleUrl: './incidencias-list.scss'
})
export class IncidenciasList implements OnInit, OnDestroy {

  private incidenciasService = inject(IncidenciasService);
  private comunidadState = inject(ComunidadStateService);
  private router = inject(Router);
  private changeDetectorRef = inject(ChangeDetectorRef);

  private readonly destruir$ = new Subject<void>();
  private componenteDestruido = false;

  incidencias: GestionIncidencia[] = [];
  incidenciasFiltradas: GestionIncidencia[] = [];

  comunidadId: number | null = null;
  nombreComunidad = '';

  cargando = false;
  error = '';

  textoBusqueda = '';
  filtroEstado = 'TODOS';
  filtroPrioridad = 'TODAS';

  resumen: ResumenIncidencias = {
    total: 0,
    pendientes: 0,
    enProceso: 0,
    esperandoProveedor: 0,
    finalizadas: 0,
    cerradas: 0,
    canceladas: 0
  };

  ngOnInit(): void {
    this.comunidadState.init();

    this.comunidadState.comunidad$
      .pipe(
        takeUntil(this.destruir$)
      )
      .subscribe(comunidad => {
        if (!comunidad) {
          this.comunidadId = null;
          this.nombreComunidad = '';
          this.error =
            'Seleccione una comunidad en la parte superior.';

          this.incidencias = [];
          this.incidenciasFiltradas = [];
          this.reiniciarResumen();
          this.actualizarVista();
          return;
        }

        const haCambiadoComunidad =
          this.comunidadId !== comunidad.id;

        this.comunidadId = comunidad.id;
        this.nombreComunidad = comunidad.nombre;

        if (haCambiadoComunidad) {
          this.textoBusqueda = '';
          this.filtroEstado = 'TODOS';
          this.filtroPrioridad = 'TODAS';
        }

        this.cargarIncidencias(comunidad.id);
      });
  }

  ngOnDestroy(): void {
    this.componenteDestruido = true;

    this.destruir$.next();
    this.destruir$.complete();
  }

  cargarIncidencias(
    comunidadId: number
  ): void {
    if (
      !Number.isInteger(comunidadId)
      || comunidadId <= 0
    ) {
      this.error =
        'El identificador de la comunidad no es válido.';

      this.actualizarVista();
      return;
    }

    this.cargando = true;
    this.error = '';

    this.incidenciasService
      .listarPorComunidad(comunidadId)
      .subscribe({
        next: (data: GestionIncidencia[]) => {
          this.incidencias = [
            ...(data ?? [])
          ];

          this.calcularResumen();
          this.aplicarFiltros();

          this.cargando = false;
          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error cargando incidencias:',
            error
          );

          this.error =
            error.status === 403
              ? 'Acceso denegado al módulo de incidencias.'
              : 'No se pudieron cargar las incidencias.';

          this.incidencias = [];
          this.incidenciasFiltradas = [];
          this.reiniciarResumen();

          this.cargando = false;
          this.actualizarVista();
        }
      });
  }

  actualizarListado(): void {
    if (this.comunidadId === null) {
      return;
    }

    this.cargarIncidencias(
      this.comunidadId
    );
  }

  nuevaIncidencia(): void {
    if (this.comunidadId === null) {
      return;
    }

    this.router.navigate([
      '/incidencias/nueva'
    ]);
  }

  editarIncidencia(
    id: number
  ): void {
    if (
      !Number.isInteger(id)
      || id <= 0
    ) {
      return;
    }

    this.router.navigate([
      '/incidencias',
      id
    ]);
  }

  aplicarFiltros(): void {
    const busqueda =
      this.normalizarTexto(
        this.textoBusqueda
      );

    this.incidenciasFiltradas =
      this.incidencias.filter(incidencia => {
        const estado =
          (incidencia.estado || '')
            .trim()
            .toUpperCase();

        const prioridad =
          (incidencia.prioridad || '')
            .trim()
            .toUpperCase();

        const coincideEstado =
          this.filtroEstado === 'TODOS'
          || estado === this.filtroEstado;

        const coincidePrioridad =
          this.filtroPrioridad === 'TODAS'
          || prioridad === this.filtroPrioridad;

        const contenidoBusqueda =
          this.normalizarTexto(
            [
              incidencia.id,
              incidencia.titulo,
              incidencia.descripcion,
              incidencia.observacionesInternas
            ]
              .filter(valor => valor !== null)
              .filter(valor => valor !== undefined)
              .join(' ')
          );

        const coincideBusqueda =
          !busqueda
          || contenidoBusqueda.includes(busqueda);

        return (
          coincideEstado
          && coincidePrioridad
          && coincideBusqueda
        );
      });

    this.actualizarVista();
  }

  limpiarFiltros(): void {
    this.textoBusqueda = '';
    this.filtroEstado = 'TODOS';
    this.filtroPrioridad = 'TODAS';

    this.aplicarFiltros();
  }

  hayFiltrosActivos(): boolean {
    return (
      this.textoBusqueda.trim().length > 0
      || this.filtroEstado !== 'TODOS'
      || this.filtroPrioridad !== 'TODAS'
    );
  }

  clasePrioridad(
    prioridad: string | null
  ): string {
    switch (
      (prioridad || '')
        .trim()
        .toUpperCase()
      ) {
      case 'URGENTE':
        return 'prioridad urgente';

      case 'ALTA':
        return 'prioridad alta';

      case 'MEDIA':
        return 'prioridad media';

      case 'BAJA':
        return 'prioridad baja';

      default:
        return 'prioridad desconocida';
    }
  }

  textoPrioridad(
    prioridad: string | null
  ): string {
    switch (
      (prioridad || '')
        .trim()
        .toUpperCase()
      ) {
      case 'URGENTE':
        return 'Urgente';

      case 'ALTA':
        return 'Alta';

      case 'MEDIA':
        return 'Media';

      case 'BAJA':
        return 'Baja';

      default:
        return 'Sin prioridad';
    }
  }

  claseEstado(
    estado: string
  ): string {
    switch (
      (estado || '')
        .trim()
        .toUpperCase()
      ) {
      case 'PENDIENTE':
        return 'estado pendiente';

      case 'EN_PROCESO':
        return 'estado proceso';

      case 'ESPERANDO_PROVEEDOR':
        return 'estado esperando';

      case 'FINALIZADA':
        return 'estado finalizada';

      case 'CERRADA':
        return 'estado cerrada';

      case 'CANCELADA':
        return 'estado cancelada';

      default:
        return 'estado desconocido';
    }
  }

  textoEstado(
    estado: string
  ): string {
    switch (
      (estado || '')
        .trim()
        .toUpperCase()
      ) {
      case 'PENDIENTE':
        return 'Pendiente';

      case 'EN_PROCESO':
        return 'En proceso';

      case 'ESPERANDO_PROVEEDOR':
        return 'Esperando proveedor';

      case 'FINALIZADA':
        return 'Finalizada';

      case 'CERRADA':
        return 'Cerrada';

      case 'CANCELADA':
        return 'Cancelada';

      default:
        return estado || 'Sin estado';
    }
  }

  trackByIncidencia(
    _indice: number,
    incidencia: GestionIncidencia
  ): number {
    return incidencia.id;
  }

  private calcularResumen(): void {
    this.resumen = {
      total: this.incidencias.length,

      pendientes:
        this.contarEstado('PENDIENTE'),

      enProceso:
        this.contarEstado('EN_PROCESO'),

      esperandoProveedor:
        this.contarEstado(
          'ESPERANDO_PROVEEDOR'
        ),

      finalizadas:
        this.contarEstado('FINALIZADA'),

      cerradas:
        this.contarEstado('CERRADA'),

      canceladas:
        this.contarEstado('CANCELADA')
    };
  }

  private contarEstado(
    estadoBuscado: string
  ): number {
    return this.incidencias.filter(
      incidencia =>
        (incidencia.estado || '')
          .trim()
          .toUpperCase()
        === estadoBuscado
    ).length;
  }

  private reiniciarResumen(): void {
    this.resumen = {
      total: 0,
      pendientes: 0,
      enProceso: 0,
      esperandoProveedor: 0,
      finalizadas: 0,
      cerradas: 0,
      canceladas: 0
    };
  }

  private normalizarTexto(
    valor: unknown
  ): string {
    return String(valor ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase();
  }

  private actualizarVista(): void {
    if (!this.componenteDestruido) {
      this.changeDetectorRef.detectChanges();
    }
  }
}
