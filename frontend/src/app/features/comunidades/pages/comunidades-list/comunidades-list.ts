import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { Comunidad } from '../../../../core/models/comunidad.model';
import { ComunidadService } from '../../../../core/services/comunidad';

@Component({
  selector: 'app-comunidades-list',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './comunidades-list.html',
  styleUrl: './comunidades-list.scss'
})
export class ComunidadesList implements OnInit, OnDestroy {

  private comunidadService = inject(ComunidadService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  private componenteDestruido = false;
  private secuenciaSolicitudQr = 0;

  comunidades: Comunidad[] = [];

  usuarioId?: number;
  administradorId?: number;

  cargando = true;
  error = '';

  paginaActual = 1;
  tamanioPagina = 10;
  totalPaginas = 0;
  totalElementos = 0;

  qrComunidad: Comunidad | null = null;
  qrUrlTemporal: string | null = null;
  cargandoQr = false;
  errorQr = '';

  ngOnInit(): void {
    const usuario = JSON.parse(
      localStorage.getItem('usuario') || 'null'
    );

    this.usuarioId = usuario?.usuarioId;
    this.administradorId = usuario?.administradorId;

    this.cargarComunidades();
  }

  ngOnDestroy(): void {
    this.componenteDestruido = true;
    this.secuenciaSolicitudQr++;
    this.liberarQrTemporal();
  }

  cargarComunidades(): void {
    this.cargando = true;
    this.error = '';

    this.comunidadService
      .getComunidades(
        this.paginaActual - 1,
        this.tamanioPagina,
        this.usuarioId
      )
      .subscribe({
        next: data => {
          this.comunidades = [
            ...(data.content ?? [])
          ];

          this.totalPaginas =
            data.totalPages ?? 0;

          this.totalElementos =
            data.totalElements ?? 0;

          this.cargando = false;
          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error cargando comunidades:',
            error
          );

          this.error =
            'No se pudieron cargar las comunidades.';

          this.cargando = false;
          this.actualizarVista();
        }
      });
  }

  cambiarPagina(pagina: number): void {
    if (
      pagina < 1
      || pagina > this.totalPaginas
    ) {
      return;
    }

    this.paginaActual = pagina;
    this.cargarComunidades();
  }

  paginas(): number[] {
    return Array.from(
      {
        length: this.totalPaginas
      },
      (_, indice) => indice + 1
    );
  }

  editarComunidad(
    id: number | undefined
  ): void {
    if (!id) {
      return;
    }

    this.router.navigate([
      '/comunidades/editar',
      id
    ]);
  }

  verVecinos(
    id: number | undefined
  ): void {
    if (!id) {
      return;
    }

    this.router.navigate([
      '/vecinos/comunidad',
      id
    ]);
  }

  verRecibos(
    id: number | undefined
  ): void {
    if (!id) {
      return;
    }

    this.router.navigate([
      '/recibos/comunidad',
      id
    ]);
  }

  verRemesas(
    id: number | undefined
  ): void {
    if (!id) {
      return;
    }

    this.router.navigate([
      '/remesas/comunidad',
      id
    ]);
  }

  abrirQrComunidad(
    comunidad: Comunidad
  ): void {
    if (!comunidad.id) {
      return;
    }

    this.secuenciaSolicitudQr++;

    const solicitudActual =
      this.secuenciaSolicitudQr;

    this.liberarQrTemporal();

    this.qrComunidad = comunidad;
    this.cargandoQr = true;
    this.errorQr = '';

    this.comunidadService
      .obtenerQrComunidad(
        comunidad.id,
        this.usuarioId,
        this.administradorId
      )
      .subscribe({
        next: contenidoQr => {
          if (
            this.componenteDestruido
            || solicitudActual
            !== this.secuenciaSolicitudQr
          ) {
            return;
          }

          if (
            !(contenidoQr instanceof Blob)
            || contenidoQr.size === 0
          ) {
            this.cargandoQr = false;
            this.errorQr =
              'El servidor devolvió un código QR vacío.';

            this.actualizarVista();
            return;
          }

          this.qrUrlTemporal =
            URL.createObjectURL(contenidoQr);

          this.cargandoQr = false;
          this.actualizarVista();
        },

        error: error => {
          if (
            this.componenteDestruido
            || solicitudActual
            !== this.secuenciaSolicitudQr
          ) {
            return;
          }

          console.error(
            'Error cargando el código QR:',
            error
          );

          this.cargandoQr = false;
          this.errorQr =
            error?.error?.message
            || 'No se pudo generar el código QR.';

          this.actualizarVista();
        }
      });
  }

  cerrarQr(): void {
    this.secuenciaSolicitudQr++;
    this.liberarQrTemporal();

    this.qrComunidad = null;
    this.cargandoQr = false;
    this.errorQr = '';

    this.actualizarVista();
  }

  descargarQr(): void {
    if (
      !this.qrUrlTemporal
      || !this.qrComunidad?.id
    ) {
      return;
    }

    const enlace =
      document.createElement('a');

    enlace.href = this.qrUrlTemporal;
    enlace.download =
      `qr-comunidad-${this.qrComunidad.id}.png`;

    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
  }

  imprimirQr(): void {
    if (
      !this.qrUrlTemporal
      || !this.qrComunidad?.id
    ) {
      return;
    }

    const comunidadActual =
      this.qrComunidad;

    const ventanaImpresion = window.open(
      '',
      '_blank',
      'width=760,height=900'
    );

    if (!ventanaImpresion) {
      this.errorQr =
        'El navegador ha bloqueado la ventana de impresión.';

      this.actualizarVista();
      return;
    }

    const nombreComunidad =
      this.escaparHtml(
        comunidadActual.nombre
        || `Comunidad ${comunidadActual.id}`
      );

    const qrUrl =
      this.escaparHtml(
        this.qrUrlTemporal
      );

    ventanaImpresion.document.open();

    ventanaImpresion.document.write(`
      <!DOCTYPE html>
      <html lang="es">
      <head>
        <meta charset="UTF-8">

        <meta
          name="viewport"
          content="width=device-width, initial-scale=1.0">

        <title>QR - ${nombreComunidad}</title>

        <style>
          * {
            box-sizing: border-box;
          }

          body {
            margin: 0;
            padding: 32px;
            font-family: Arial, Helvetica, sans-serif;
            color: #111827;
            background: #ffffff;
          }

          .hoja {
            max-width: 700px;
            margin: 0 auto;
            padding: 34px;
            text-align: center;
            border: 2px solid #123a63;
            border-radius: 16px;
          }

          h1 {
            margin: 0 0 10px;
            color: #123a63;
            font-size: 30px;
          }

          h2 {
            margin: 0 0 28px;
            font-size: 22px;
          }

          img {
            display: block;
            width: 400px;
            max-width: 100%;
            height: auto;
            margin: 0 auto 28px;
          }

          p {
            margin: 8px 0;
            color: #475467;
            font-size: 17px;
            line-height: 1.5;
          }

          .identificador {
            margin-top: 24px;
            color: #667085;
            font-size: 13px;
          }

          @media print {
            body {
              padding: 0;
            }

            .hoja {
              break-inside: avoid;
            }
          }
        </style>
      </head>

      <body>
        <main class="hoja">

          <h1>Gombeth Urban</h1>

          <h2>${nombreComunidad}</h2>

          <img
            id="qr-imprimir"
            src="${qrUrl}"
            alt="Código QR de ${nombreComunidad}">

          <p>
            Escanee este código QR con la cámara
            de su teléfono para comunicar una incidencia
            de la comunidad.
          </p>

          <p class="identificador">
            Identificador interno de comunidad:
            ${comunidadActual.id}
          </p>

        </main>
      </body>
      </html>
    `);

    ventanaImpresion.document.close();

    const ejecutarImpresion = (): void => {
      ventanaImpresion.focus();

      setTimeout(() => {
        ventanaImpresion.print();
      }, 250);
    };

    const imagenQr =
      ventanaImpresion.document.getElementById(
        'qr-imprimir'
      ) as HTMLImageElement | null;

    if (!imagenQr || imagenQr.complete) {
      ejecutarImpresion();
      return;
    }

    imagenQr.onload = ejecutarImpresion;
  }

  private liberarQrTemporal(): void {
    if (this.qrUrlTemporal) {
      URL.revokeObjectURL(
        this.qrUrlTemporal
      );
    }

    this.qrUrlTemporal = null;
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
      this.cdr.detectChanges();
    }
  }
}
