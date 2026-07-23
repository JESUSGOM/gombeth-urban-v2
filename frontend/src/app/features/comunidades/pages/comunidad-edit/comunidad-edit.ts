import {
  ChangeDetectorRef,
  Component,
  OnDestroy,
  OnInit,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { Comunidad } from '../../../../core/models/comunidad.model';
import { ComunidadService } from '../../../../core/services/comunidad';

@Component({
  selector: 'app-comunidad-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './comunidad-edit.html',
  styleUrl: './comunidad-edit.scss'
})
export class ComunidadEdit implements OnInit, OnDestroy {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private comunidadService = inject(ComunidadService);
  private cdr = inject(ChangeDetectorRef);

  private componenteDestruido = false;

  comunidad?: Comunidad;

  metodoReparto = 'COEFICIENTE';

  guardando = false;
  mensajeExito = '';
  mensajeError = '';

  cargandoQr = false;
  mensajeErrorQr = '';
  qrUrlTemporal: string | null = null;
  visorQrAbierto = false;

  ngOnInit(): void {
    const id = Number(
      this.route.snapshot.paramMap.get('id')
    );

    if (!Number.isInteger(id) || id <= 0) {
      this.mensajeError =
        'El identificador de la comunidad no es válido.';
      return;
    }


    this.comunidadService
      .getComunidad(
        id
      )
      .subscribe({
        next: data => {
          this.comunidad = {
            ...data
          };

          this.cargarConfiguracionReparto();
          this.cargarQrComunidad();
          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error cargando comunidad:',
            error
          );

          this.mensajeError =
            error?.error?.message
            || 'No se pudo cargar la comunidad.';

          this.actualizarVista();
        }
      });
  }

  ngOnDestroy(): void {
    this.componenteDestruido = true;
    this.liberarQrTemporal();
  }

  cargarConfiguracionReparto(): void {
    if (!this.comunidad?.id) {
      return;
    }

    this.comunidadService
      .getConfiguracionReparto(
        this.comunidad.id
      )
      .subscribe({
        next: configuracion => {
          this.metodoReparto =
            configuracion.metodoReparto
            || 'COEFICIENTE';

          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error cargando configuración de reparto:',
            error
          );

          this.metodoReparto = 'COEFICIENTE';
          this.actualizarVista();
        }
      });
  }

  cargarQrComunidad(): void {
    if (!this.comunidad?.id) {
      return;
    }

    const comunidadId = this.comunidad.id;

    this.liberarQrTemporal();

    this.cargandoQr = true;
    this.mensajeErrorQr = '';
    this.visorQrAbierto = false;

    this.comunidadService
      .obtenerQrComunidad(
        comunidadId
      )
      .subscribe({
        next: contenidoQr => {
          if (
            !(contenidoQr instanceof Blob)
            || contenidoQr.size === 0
          ) {
            this.cargandoQr = false;
            this.mensajeErrorQr =
              'El servidor devolvió un código QR vacío.';

            this.actualizarVista();
            return;
          }

          if (this.componenteDestruido) {
            return;
          }

          this.qrUrlTemporal =
            URL.createObjectURL(contenidoQr);

          this.cargandoQr = false;
          this.actualizarVista();
        },

        error: error => {
          console.error(
            'Error cargando el código QR:',
            error
          );

          this.cargandoQr = false;
          this.mensajeErrorQr =
            error?.error?.message
            || 'No se pudo generar el código QR de la comunidad.';

          this.actualizarVista();
        }
      });
  }

  abrirQr(): void {
    if (!this.qrUrlTemporal) {
      return;
    }

    this.visorQrAbierto = true;
  }

  cerrarQr(): void {
    this.visorQrAbierto = false;
  }

  descargarQr(): void {
    if (
      !this.qrUrlTemporal
      || !this.comunidad?.id
    ) {
      return;
    }

    const enlace = document.createElement('a');

    enlace.href = this.qrUrlTemporal;
    enlace.download =
      `qr-comunidad-${this.comunidad.id}.png`;

    document.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
  }

  imprimirQr(): void {
    if (
      !this.qrUrlTemporal
      || !this.comunidad?.id
    ) {
      return;
    }

    const ventanaImpresion = window.open(
      '',
      '_blank',
      'width=760,height=900'
    );

    if (!ventanaImpresion) {
      this.mensajeErrorQr =
        'El navegador ha bloqueado la ventana de impresión.';

      this.actualizarVista();
      return;
    }

    const nombreComunidad = this.escaparHtml(
      this.comunidad.nombre
      || `Comunidad ${this.comunidad.id}`
    );

    const qrUrl = this.escaparHtml(
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
            border: 2px solid #123a63;
            border-radius: 16px;
            text-align: center;
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
              border: 2px solid #123a63;
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
            Escanee este código QR con la cámara de su teléfono
            para comunicar una incidencia de la comunidad.
          </p>

          <p class="identificador">
            Identificador interno de comunidad:
            ${this.comunidad.id}
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

    if (!imagenQr) {
      ejecutarImpresion();
      return;
    }

    if (imagenQr.complete) {
      ejecutarImpresion();
      return;
    }

    imagenQr.onload = ejecutarImpresion;
  }

  volver(): void {
    this.router.navigate(['/comunidades']);
  }

  guardar(): void {
    if (!this.comunidad?.id) {
      return;
    }

    this.guardando = true;
    this.mensajeExito = '';
    this.mensajeError = '';

    this.comunidadService
      .actualizarComunidad(
        this.comunidad.id,
        this.comunidad
      )
      .subscribe({
        next: () => {
          this.guardarConfiguracionReparto();
        },

        error: error => {
          console.error(
            'Error al guardar comunidad:',
            error
          );

          this.guardando = false;
          this.mensajeError =
            'No se pudo guardar la comunidad.';

          this.actualizarVista();
        }
      });
  }

  guardarConfiguracionReparto(): void {
    if (!this.comunidad?.id) {
      this.guardando = false;
      this.mensajeError =
        'No se pudo guardar la configuración de reparto.';

      this.actualizarVista();
      return;
    }

    this.comunidadService
      .guardarConfiguracionReparto(
        this.comunidad.id,
        this.metodoReparto
      )
      .subscribe({
        next: () => {
          this.guardando = false;
          this.mensajeExito =
            'Comunidad guardada correctamente.';

          this.actualizarVista();

          setTimeout(() => {
            this.router.navigate(['/comunidades']);
          }, 3000);
        },

        error: error => {
          console.error(
            'Error guardando configuración de reparto:',
            error
          );

          this.guardando = false;
          this.mensajeError =
            'La comunidad se guardó, pero no se pudo guardar '
            + 'el método de reparto.';

          this.actualizarVista();
        }
      });
  }

  private liberarQrTemporal(): void {
    if (this.qrUrlTemporal) {
      URL.revokeObjectURL(
        this.qrUrlTemporal
      );
    }

    this.qrUrlTemporal = null;
    this.visorQrAbierto = false;
  }

  private escaparHtml(valor: string): string {
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
