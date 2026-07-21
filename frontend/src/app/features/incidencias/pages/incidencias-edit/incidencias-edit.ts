import { CommonModule } from '@angular/common';
import {
  ChangeDetectorRef,
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
  private changeDetectorRef = inject(ChangeDetectorRef);

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
            this.actualizarVista();
          },

          error: error => {
            console.error(
                'Error cargando la incidencia:',
                error
            );

            this.error =
                'No se pudo cargar la incidencia.';

            this.cargando = false;
            this.actualizarVista();
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
              this.actualizarVista();
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

            this.actualizarVista();
          },

          error: error => {
            console.error(
                'Error consultando los adjuntos:',
                error
            );

            this.errorAdjuntos =
                'No se pudieron consultar las fotografías de la incidencia.';

            this.adjuntos = [];
            this.actualizarVista();
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
    this.actualizarVista();
  }

  imprimirFicha(): void {
    if (
        !this.editando
        || this.id === null
        || !this.incidencia.comunidad
    ) {
      this.error =
          'La incidencia debe estar guardada antes de imprimirla.';

      this.actualizarVista();
      return;
    }

    if (!this.aplicarCosteEstimadoDesdeTexto()) {
      this.actualizarVista();
      return;
    }

    const ventanaImpresion = window.open(
        '',
        '_blank',
        'width=1000,height=900'
    );

    if (!ventanaImpresion) {
      this.error =
          'El navegador ha bloqueado la ventana de impresión.';

      this.actualizarVista();
      return;
    }

    const fotografiasDisponibles =
        this.adjuntos.filter(
            adjunto => adjunto.urlTemporal
        );

    const fotografiasHtml =
        fotografiasDisponibles.length > 0
            ? fotografiasDisponibles
                .map(
                    (adjunto, indice) => `
              <article class="fotografia">
                <img
                  src="${this.escaparHtml(adjunto.urlTemporal ?? '')}"
                  alt="Fotografía ${indice + 1} de la incidencia">

                <p>
                  ${this.escaparHtml(
                        adjunto.nombreOriginal
                        || `Fotografía ${indice + 1}`
                    )}
                </p>
              </article>
            `
                )
                .join('')
            : `
          <div class="sin-fotografias">
            La incidencia no tiene fotografías disponibles.
          </div>
        `;

    ventanaImpresion.document.open();

    ventanaImpresion.document.write(`
      <!DOCTYPE html>
      <html lang="es">

      <head>
        <meta charset="UTF-8">

        <meta
          name="viewport"
          content="width=device-width, initial-scale=1.0">

        <title>
          Incidencia ${this.id} -
          ${this.escaparHtml(this.incidencia.titulo)}
        </title>

        <style>
          * {
            box-sizing: border-box;
          }

          body {
            margin: 0;
            padding: 28px;
            color: #172033;
            background: #ffffff;
            font-family: Arial, Helvetica, sans-serif;
          }

          .documento {
            max-width: 1000px;
            margin: 0 auto;
          }

          .cabecera {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            gap: 24px;
            padding-bottom: 20px;
            border-bottom: 3px solid #123a63;
          }

          .marca h1 {
            margin: 0 0 6px;
            color: #123a63;
            font-size: 30px;
          }

          .marca p {
            margin: 0;
            color: #667085;
          }

          .referencia {
            padding: 12px 16px;
            text-align: right;
            background: #eef4fb;
            border-radius: 8px;
          }

          .referencia strong,
          .referencia span {
            display: block;
          }

          .referencia strong {
            color: #123a63;
            font-size: 18px;
          }

          .referencia span {
            margin-top: 4px;
            color: #667085;
            font-size: 13px;
          }

          h2 {
            margin: 26px 0 8px;
            color: #101828;
            font-size: 24px;
          }

          h3 {
            margin: 24px 0 12px;
            padding-bottom: 7px;
            color: #123a63;
            border-bottom: 1px solid #d0d5dd;
            font-size: 18px;
          }

          .comunidad {
            margin: 0 0 22px;
            color: #475467;
            font-size: 16px;
          }

          .datos-grid,
          .fechas-grid {
            display: grid;
            grid-template-columns: repeat(4, minmax(0, 1fr));
            gap: 12px;
          }

          .dato {
            padding: 12px;
            background: #f8fafc;
            border: 1px solid #e4e7ec;
            border-radius: 8px;
          }

          .dato span {
            display: block;
            margin-bottom: 5px;
            color: #667085;
            font-size: 12px;
            font-weight: bold;
          }

          .dato strong {
            color: #101828;
            font-size: 14px;
          }

          .texto {
            padding: 15px;
            color: #344054;
            background: #ffffff;
            border: 1px solid #d0d5dd;
            border-radius: 8px;
            font-size: 14px;
            line-height: 1.55;
          }

          .observaciones {
            background: #fffaeb;
            border-color: #fedf89;
          }

          .fotografias-grid {
            display: grid;
            grid-template-columns: repeat(2, minmax(0, 1fr));
            gap: 16px;
          }

          .fotografia {
            break-inside: avoid;
            overflow: hidden;
            border: 1px solid #d0d5dd;
            border-radius: 8px;
          }

          .fotografia img {
            display: block;
            width: 100%;
            max-height: 420px;
            object-fit: contain;
            background: #f2f4f7;
          }

          .fotografia p {
            margin: 0;
            padding: 9px 12px;
            color: #475467;
            font-size: 12px;
          }

          .sin-fotografias {
            padding: 18px;
            color: #667085;
            background: #f8fafc;
            border: 1px solid #e4e7ec;
            border-radius: 8px;
          }

          .pie {
            margin-top: 28px;
            padding-top: 12px;
            color: #667085;
            border-top: 1px solid #d0d5dd;
            font-size: 11px;
            text-align: center;
          }

          @media print {
            body {
              padding: 0;
            }

            .documento {
              max-width: none;
            }

            .fotografia,
            .dato,
            h3 {
              break-inside: avoid;
            }
          }
        </style>
      </head>

      <body>

        <main class="documento">

          <header class="cabecera">

            <div class="marca">
              <h1>Gombeth Urban</h1>
              <p>Ficha de seguimiento de incidencia</p>
            </div>

            <div class="referencia">

              <strong>
                Incidencia #${this.id}
              </strong>

              <span>
                Generada el
                ${this.escaparHtml(
        this.formatearFechaImpresion(
            new Date().toISOString()
        )
    )}
              </span>

            </div>

          </header>

          <h2>
            ${this.escaparHtml(this.incidencia.titulo)}
          </h2>

          <p class="comunidad">
            ${this.escaparHtml(
        this.incidencia.comunidad.nombre
    )}
          </p>

          <section class="datos-grid">

            <div class="dato">
              <span>Estado</span>
              <strong>
                ${this.escaparHtml(
        this.formatearEstado(
            this.incidencia.estado
        )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Prioridad</span>
              <strong>
                ${this.escaparHtml(
        this.formatearPrioridad(
            this.incidencia.prioridad
        )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Coste estimado</span>
              <strong>
                ${this.escaparHtml(
        this.formatearImporte(
            this.incidencia.costeEstimado
        )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Comunidad</span>
              <strong>
                #${this.incidencia.comunidad.id}
              </strong>
            </div>

          </section>

          <h3>Descripción comunicada</h3>

          <div class="texto">
            ${this.textoConSaltos(
        this.incidencia.descripcion
        || 'Sin descripción.'
    )}
          </div>

          <h3>Observaciones internas</h3>

          <div class="texto observaciones">
            ${this.textoConSaltos(
        this.incidencia.observacionesInternas
        || 'Sin observaciones internas.'
    )}
          </div>

          <h3>Fechas de seguimiento</h3>

          <section class="fechas-grid">

            <div class="dato">
              <span>Registro</span>
              <strong>
                ${this.escaparHtml(
        this.formatearFechaImpresion(
            this.incidencia.fechaRegistro
        )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Última actualización</span>
              <strong>
                ${this.escaparHtml(
        this.formatearFechaImpresion(
            this.incidencia.fechaActualizacion
        )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Finalización</span>
              <strong>
                ${this.escaparHtml(
        this.formatearFechaImpresion(
            this.incidencia.fechaFinalizacion
        )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Cierre</span>
              <strong>
                ${this.escaparHtml(
        this.formatearFechaImpresion(
            this.incidencia.fechaCierre
        )
    )}
              </strong>
            </div>

          </section>

          <h3>Fotografías</h3>

          <section class="fotografias-grid">
            ${fotografiasHtml}
          </section>

          <footer class="pie">
            Documento generado desde Gombeth Urban.
          </footer>

        </main>

      </body>

      </html>
    `);

    ventanaImpresion.document.close();

    const ejecutarImpresion = (): void => {
      ventanaImpresion.focus();

      setTimeout(() => {
        ventanaImpresion.print();
      }, 300);
    };

    const imagenes =
        Array.from(
            ventanaImpresion.document.images
        );

    if (imagenes.length === 0) {
      ejecutarImpresion();
      return;
    }

    let imagenesFinalizadas = 0;
    let impresionEjecutada = false;

    const comprobarImagenes = (): void => {
      imagenesFinalizadas++;

      if (
          imagenesFinalizadas >= imagenes.length
          && !impresionEjecutada
      ) {
        impresionEjecutada = true;
        ejecutarImpresion();
      }
    };

    for (const imagen of imagenes) {
      if (imagen.complete) {
        comprobarImagenes();
      } else {
        imagen.onload = comprobarImagenes;
        imagen.onerror = comprobarImagenes;
      }
    }

    setTimeout(() => {
      if (!impresionEjecutada) {
        impresionEjecutada = true;
        ejecutarImpresion();
      }
    }, 3000);
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
      this.actualizarVista();
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

        this.actualizarVista();
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

  private formatearEstado(
      estado: string
  ): string {
    switch (
        (estado || '').trim().toUpperCase()
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

  private formatearPrioridad(
      prioridad: string
  ): string {
    switch (
        (prioridad || '').trim().toUpperCase()
        ) {
      case 'BAJA':
        return 'Baja';

      case 'MEDIA':
        return 'Media';

      case 'ALTA':
        return 'Alta';

      case 'URGENTE':
        return 'Urgente';

      default:
        return prioridad || 'Sin prioridad';
    }
  }

  private formatearImporte(
      importe: number | null
  ): string {
    if (
        importe === null
        || !Number.isFinite(importe)
    ) {
      return 'Sin coste estimado';
    }

    return new Intl.NumberFormat(
        'es-ES',
        {
          style: 'currency',
          currency: 'EUR'
        }
    ).format(importe);
  }

  private formatearFechaImpresion(
      fecha: string | null
  ): string {
    if (!fecha) {
      return 'Pendiente';
    }

    const valorFecha = new Date(fecha);

    if (
        Number.isNaN(
            valorFecha.getTime()
        )
    ) {
      return 'Fecha no disponible';
    }

    return new Intl.DateTimeFormat(
        'es-ES',
        {
          day: '2-digit',
          month: '2-digit',
          year: 'numeric',
          hour: '2-digit',
          minute: '2-digit'
        }
    ).format(valorFecha);
  }

  private textoConSaltos(
      texto: string
  ): string {
    return this.escaparHtml(texto)
        .replace(/\r\n/g, '\n')
        .replace(/\r/g, '\n')
        .replace(/\n/g, '<br>');
  }

  private escaparHtml(
      valor: string
  ): string {
    return valor
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
  }

  private actualizarVista(): void {
    if (!this.componenteDestruido) {
      this.changeDetectorRef.detectChanges();
    }
  }
}
