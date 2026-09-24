import { CommonModule } from '@angular/common';
import {
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import {
  catchError,
  finalize,
  map,
  switchMap
} from 'rxjs/operators';

import {
  GestionIncidencia,
  IncidenciaAdjunto,
  IncidenciasService
} from '../../services/incidencias.service';

import {
  ComunidadStateService
} from '../../../../core/state/comunidad-state.service';

import {
  IncidenciaImpresionService
} from '../../services/incidencia-impresion.service';

interface ComunidadIncidenciaFormulario {
  id: number;
  nombre: string;
}

interface IncidenciaFormulario {
  titulo: string;
  descripcion: string;
  observacionesInternas: string;
  prioridad: string;
  estado: string;
  costeEstimado: number | null;
  fechaRegistro: string | null;
  fechaActualizacion: string | null;
  fechaFinalizacion: string | null;
  fechaCierre: string | null;
  comunidad: ComunidadIncidenciaFormulario | null;
}

interface IncidenciaAdjuntoVista extends IncidenciaAdjunto {
  urlTemporal: string | null;
  errorCarga: boolean;
}

@Component({
  selector: 'app-incidencias-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './incidencias-edit.html',
  styleUrl: './incidencias-edit.scss'
})
export class IncidenciasEdit implements OnInit, OnDestroy {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private incidenciasService = inject(IncidenciasService);
  private comunidadState = inject(ComunidadStateService);

  private incidenciaImpresionService =
    inject(IncidenciaImpresionService);
  private componenteDestruido = false;

  id: number | null = null;
  editando = false;

  cargando = false;
  guardando = false;
  cargandoAdjuntos = false;

  error = '';
  mensaje = '';
  errorAdjuntos = '';

  costeEstimadoTexto = '';

  adjuntos: IncidenciaAdjuntoVista[] = [];
  fotografiaSeleccionada: IncidenciaAdjuntoVista | null = null;

  incidencia: IncidenciaFormulario = {
    titulo: '',
    descripcion: '',
    observacionesInternas: '',
    prioridad: 'MEDIA',
    estado: 'PENDIENTE',
    costeEstimado: null,
    fechaRegistro: null,
    fechaActualizacion: null,
    fechaFinalizacion: null,
    fechaCierre: null,
    comunidad: null
  };

  ngOnInit(): void {
    this.comunidadState.init();

    const idParam =
        this.route.snapshot.paramMap.get('id');

    if (idParam && idParam !== 'nueva') {
      const idNumerico = Number(idParam);

      if (
          !Number.isInteger(idNumerico)
          || idNumerico <= 0
      ) {
        this.error =
            'El identificador de la incidencia no es válido.';

        return;
      }

      this.id = idNumerico;
      this.editando = true;
      this.cargarIncidencia();
      return;
    }

    const comunidad =
        this.comunidadState.getComunidad();

    if (!comunidad) {
      this.error =
          'Debe seleccionar una comunidad antes de crear la incidencia.';

      return;
    }

    this.incidencia.comunidad = {
      id: comunidad.id,
      nombre: comunidad.nombre
    };
  }

  ngOnDestroy(): void {
    this.componenteDestruido = true;
    this.liberarUrlsTemporales();
  }

  cargarIncidencia(): void {
    if (this.id === null) {
      return;
    }

    this.cargando = true;
    this.error = '';

    this.incidenciasService
        .obtener(this.id)
        .subscribe({
          next: (data: GestionIncidencia) => {
            this.incidencia = {
              titulo: data.titulo ?? '',
              descripcion: data.descripcion ?? '',
              observacionesInternas:
                  data.observacionesInternas ?? '',
              prioridad: data.prioridad ?? 'MEDIA',
              estado: data.estado ?? 'PENDIENTE',
              costeEstimado:
                  data.costeEstimado ?? null,
              fechaRegistro:
                  data.fechaRegistro ?? null,
              fechaActualizacion:
                  data.fechaActualizacion ?? null,
              fechaFinalizacion:
                  data.fechaFinalizacion ?? null,
              fechaCierre:
                  data.fechaCierre ?? null,
              comunidad: data.comunidad
                  ? {
                    id: data.comunidad.id,
                    nombre: data.comunidad.nombre
                  }
                  : null
            };

            this.costeEstimadoTexto =
                this.formatearCosteParaFormulario(
                    this.incidencia.costeEstimado
                );

            this.cargando = false;
            this.cargarAdjuntos();

          },

          error: error => {
            console.error(
                'Error cargando la incidencia:',
                error
            );

            this.error =
                'No se pudo cargar la incidencia.';

            this.cargando = false;

          }
        });
  }

  cargarAdjuntos(): void {
    if (this.id === null) {
      return;
    }

    const incidenciaId = this.id;

    this.liberarUrlsTemporales();

    this.cargandoAdjuntos = true;
    this.errorAdjuntos = '';
    this.adjuntos = [];

    this.incidenciasService
        .listarAdjuntos(incidenciaId)
        .pipe(
            switchMap(
                (adjuntos: IncidenciaAdjunto[]) => {
                  if (adjuntos.length === 0) {
                    return of(
                        [] as IncidenciaAdjuntoVista[]
                    );
                  }

                  const operaciones = adjuntos.map(
                      adjunto =>
                          this.incidenciasService
                              .obtenerContenidoAdjunto(
                                  incidenciaId,
                                  adjunto.id
                              )
                              .pipe(
                                  map(contenido => {
                                    const urlTemporal =
                                        URL.createObjectURL(contenido);

                                    return {
                                      ...adjunto,
                                      urlTemporal,
                                      errorCarga: false
                                    } as IncidenciaAdjuntoVista;
                                  }),

                                  catchError(error => {
                                    console.error(
                                        `Error cargando el adjunto ${adjunto.id}:`,
                                        error
                                    );

                                    return of({
                                      ...adjunto,
                                      urlTemporal: null,
                                      errorCarga: true
                                    } as IncidenciaAdjuntoVista);
                                  })
                              )
                  );

                  return forkJoin(operaciones);
                }
            ),

            finalize(() => {
              this.cargandoAdjuntos = false;

            })
        )
        .subscribe({
          next: adjuntos => {
            if (this.componenteDestruido) {
              this.revocarUrls(adjuntos);
              return;
            }

            this.adjuntos = adjuntos;

            if (
                adjuntos.some(
                    adjunto => adjunto.errorCarga
                )
            ) {
              this.errorAdjuntos =
                  'Alguna fotografía no pudo cargarse correctamente.';
            }


          },

          error: error => {
            console.error(
                'Error consultando los adjuntos:',
                error
            );

            this.errorAdjuntos =
                'No se pudieron consultar las fotografías de la incidencia.';

            this.adjuntos = [];

          }
        });
  }

  abrirFotografia(
      adjunto: IncidenciaAdjuntoVista
  ): void {
    if (!adjunto.urlTemporal) {
      return;
    }

    this.fotografiaSeleccionada = adjunto;
  }

  cerrarFotografia(): void {
    this.fotografiaSeleccionada = null;
  }

  descargarFotografia(
      adjunto: IncidenciaAdjuntoVista
  ): void {
    if (!adjunto.urlTemporal) {
      return;
    }

    const enlace =
        document.createElement('a');

    enlace.href = adjunto.urlTemporal;
    enlace.download =
        adjunto.nombreOriginal?.trim()
            ? adjunto.nombreOriginal
            : `adjunto-${adjunto.id}`;

    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
  }

  formatearTamanio(
      bytes: number
  ): string {
    if (
        !Number.isFinite(bytes)
        || bytes <= 0
    ) {
      return '0 bytes';
    }

    if (bytes < 1024) {
      return `${bytes} bytes`;
    }

    const kilobytes = bytes / 1024;

    if (kilobytes < 1024) {
      return `${kilobytes.toFixed(1)} KB`;
    }

    const megabytes = kilobytes / 1024;

    return `${megabytes.toFixed(2)} MB`;
  }

  normalizarCosteEstimado(): void {
    this.aplicarCosteEstimadoDesdeTexto();

  }

  imprimirFicha(): void {
    this.error = '';

    if (
      !this.editando
      || this.id === null
      || !this.incidencia.comunidad
    ) {
      this.error =
        'La incidencia debe estar guardada antes de imprimirla.';

      return;
    }

    if (!this.aplicarCosteEstimadoDesdeTexto()) {
      return;
    }

    const errorImpresion =
      this.incidenciaImpresionService.imprimir({
        id: this.id,
        incidencia: this.incidencia,
        adjuntos: this.adjuntos
      });

    if (errorImpresion) {
      this.error = errorImpresion;
    }
  }

  guardar(): void {
    this.error = '';
    this.mensaje = '';

    if (!this.incidencia.titulo.trim()) {
      this.error =
          'El título de la incidencia es obligatorio.';

      return;
    }

    if (!this.aplicarCosteEstimadoDesdeTexto()) {

      return;
    }

    if (!this.incidencia.comunidad) {
      const comunidad =
          this.comunidadState.getComunidad();

      if (!comunidad) {
        this.error =
            'No hay una comunidad seleccionada.';

        return;
      }

      this.incidencia.comunidad = {
        id: comunidad.id,
        nombre: comunidad.nombre
      };
    }

    const datosGuardar: Partial<GestionIncidencia> = {
      titulo: this.incidencia.titulo.trim(),
      descripcion: this.incidencia.descripcion,
      observacionesInternas:
      this.incidencia.observacionesInternas,
      prioridad: this.incidencia.prioridad,
      estado: this.incidencia.estado,
      costeEstimado:
      this.incidencia.costeEstimado,
      comunidad: {
        id: this.incidencia.comunidad.id,
        nombre: this.incidencia.comunidad.nombre
      }
    };

    this.guardando = true;

    const operacion =
        this.editando && this.id !== null
            ? this.incidenciasService.actualizar(
                this.id,
                datosGuardar
            )
            : this.incidenciasService.guardar(
                datosGuardar
            );

    operacion.subscribe({
      next: () => {
        this.guardando = false;
        this.router.navigate(['/incidencias']);
      },

      error: error => {
        console.error(
            'Error guardando la incidencia:',
            error
        );

        this.guardando = false;

        this.error =
            error?.error?.message
            || error?.error?.detail
            || 'No se pudo guardar la incidencia.';


      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/incidencias']);
  }

  private aplicarCosteEstimadoDesdeTexto(): boolean {
    const texto =
        this.costeEstimadoTexto.trim();

    if (!texto) {
      this.incidencia.costeEstimado = null;
      this.costeEstimadoTexto = '';
      return true;
    }

    let textoNormalizado = texto
        .replace(/\s/g, '');

    if (textoNormalizado.includes(',')) {
      textoNormalizado = textoNormalizado
          .replace(/\./g, '')
          .replace(',', '.');
    }

    if (
        !/^\d+(\.\d{1,2})?$/.test(
            textoNormalizado
        )
    ) {
      this.error =
          'El coste estimado debe tener un máximo de dos decimales.';

      return false;
    }

    const valor =
        Number(textoNormalizado);

    if (
        !Number.isFinite(valor)
        || valor < 0
    ) {
      this.error =
          'El coste estimado debe ser un importe válido y no negativo.';

      return false;
    }

    const valorRedondeado =
        Math.round(
            (valor + Number.EPSILON) * 100
        ) / 100;

    this.incidencia.costeEstimado =
        valorRedondeado;

    this.costeEstimadoTexto =
        this.formatearCosteParaFormulario(
            valorRedondeado
        );

    this.error = '';

    return true;
  }

  private formatearCosteParaFormulario(
      valor: number | null
  ): string {
    if (
        valor === null
        || !Number.isFinite(valor)
    ) {
      return '';
    }

    return new Intl.NumberFormat(
        'es-ES',
        {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
          useGrouping: false
        }
    ).format(valor);
  }

  private liberarUrlsTemporales(): void {
    this.revocarUrls(this.adjuntos);
    this.adjuntos = [];
    this.fotografiaSeleccionada = null;
  }

  private revocarUrls(
      adjuntos: IncidenciaAdjuntoVista[]
  ): void {
    for (const adjunto of adjuntos) {
      if (adjunto.urlTemporal) {
        URL.revokeObjectURL(
            adjunto.urlTemporal
        );
      }
    }
  }

}
