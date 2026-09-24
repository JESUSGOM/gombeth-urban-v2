import { Injectable } from '@angular/core';

interface ComunidadImpresion {
  id: number;
  nombre: string;
}

interface IncidenciaImpresion {
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
  comunidad: ComunidadImpresion | null;
}

interface AdjuntoImpresion {
  nombreOriginal?: string | null;
  urlTemporal?: string | null;
}

export interface DatosImpresionIncidencia {
  id: number;
  incidencia: IncidenciaImpresion;
  adjuntos: readonly AdjuntoImpresion[];
}

@Injectable({
  providedIn: 'root'
})
export class IncidenciaImpresionService {

  imprimir(
    datos: DatosImpresionIncidencia
  ): string | null {

    const {
      id,
      incidencia,
      adjuntos
    } = datos;

    const comunidad = incidencia.comunidad;

    if (!comunidad) {
      return 'La incidencia no tiene una comunidad válida.';
    }

    const ventanaImpresion = window.open(
      '',
      '_blank',
      'width=1000,height=900'
    );

    if (!ventanaImpresion) {
      return 'El navegador ha bloqueado la ventana de impresión.';
    }

    const fotografiasDisponibles =
      adjuntos.filter(
        adjunto => adjunto.urlTemporal
      );

    const fotografiasHtml =
      fotografiasDisponibles.length > 0
        ? fotografiasDisponibles
          .map(
            (adjunto, indice) => `
                <article class="fotografia">
                  <img
                    src="${this.escaparHtml(
              adjunto.urlTemporal ?? ''
            )}"
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
          Incidencia ${id} -
          ${this.escaparHtml(incidencia.titulo)}
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
                Incidencia #${id}
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
            ${this.escaparHtml(incidencia.titulo)}
          </h2>

          <p class="comunidad">
            ${this.escaparHtml(
      comunidad.nombre
    )}
          </p>

          <section class="datos-grid">

            <div class="dato">
              <span>Estado</span>
              <strong>
                ${this.escaparHtml(
      this.formatearEstado(
        incidencia.estado
      )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Prioridad</span>
              <strong>
                ${this.escaparHtml(
      this.formatearPrioridad(
        incidencia.prioridad
      )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Coste estimado</span>
              <strong>
                ${this.escaparHtml(
      this.formatearImporte(
        incidencia.costeEstimado
      )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Comunidad</span>
              <strong>
                #${comunidad.id}
              </strong>
            </div>

          </section>

          <h3>Descripción comunicada</h3>

          <div class="texto">
            ${this.textoConSaltos(
      incidencia.descripcion
      || 'Sin descripción.'
    )}
          </div>

          <h3>Observaciones internas</h3>

          <div class="texto observaciones">
            ${this.textoConSaltos(
      incidencia.observacionesInternas
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
        incidencia.fechaRegistro
      )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Última actualización</span>
              <strong>
                ${this.escaparHtml(
      this.formatearFechaImpresion(
        incidencia.fechaActualizacion
      )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Finalización</span>
              <strong>
                ${this.escaparHtml(
      this.formatearFechaImpresion(
        incidencia.fechaFinalizacion
      )
    )}
              </strong>
            </div>

            <div class="dato">
              <span>Cierre</span>
              <strong>
                ${this.escaparHtml(
      this.formatearFechaImpresion(
        incidencia.fechaCierre
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
      return null;
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

    return null;
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
}
