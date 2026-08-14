import {
  ChangeDetectorRef,
  Component,
  OnInit,
  inject
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { Vecino } from '../../../../core/models/vecino.model';
import { VecinoDocumento } from '../../../../core/models/vecino-documento.model';

import { VecinoService } from '../../../../core/services/vecino';
import {
  VecinoDocumentoService
} from '../../../../core/services/vecino-documento.service';

@Component({
  selector: 'app-vecino-edit',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './vecino-edit.html',
  styleUrl: './vecino-edit.scss',
})
export class VecinoEdit implements OnInit {

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private vecinoService = inject(VecinoService);

  private documentoService =
    inject(VecinoDocumentoService);

  private cdr = inject(ChangeDetectorRef);

  vecino?: Vecino;

  documentos: VecinoDocumento[] = [];

  esNuevo = false;
  guardando = false;
  subiendoDocumento = false;
  descargandoMandato = false;
  cargandoDocumentos = false;
  eliminandoDocumentoId?: number;

  mensaje = '';
  error = '';
  errorDocumentos = '';

  archivoSeleccionado?: File;

  ngOnInit(): void {
    const id =
      this.route.snapshot.paramMap.get('id');

    const comunidadId =
      this.route.snapshot.paramMap.get(
        'comunidadId'
      );

    if (id) {
      this.cargarVecino(Number(id));
      return;
    }

    if (comunidadId) {
      this.esNuevo = true;

      this.vecino = {
        id: 0,
        comunidadId: Number(comunidadId),
        nombre: '',
        vivienda: '',
        nif: '',
        iban: '',
        bic: '',
        email: '',
        telefono1: '',
        telefono2: '',
        telefono3: '',
        direccion: '',
        poblacion: '',
        provincia: '',
        codigoPostal: '',
        paisCod: 'ES',
        referenciaMandato: '',
        fechaMandato: null,
        direccionNotificacion: '',
        rutaMandatoFirmado: '',
        coeficiente: 0,
        domiciliado: true,
        activo: true,
        notas: ''
      };

      this.cdr.detectChanges();
    }
  }

  cargarVecino(id: number): void {
    this.vecinoService
      .getVecino(id)
      .subscribe({
        next: (data) => {
          this.vecino = {
            ...data
          };

          this.cargarDocumentos(id);
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(
            'Error cargando propietario:',
            err
          );

          this.error =
            'No se pudo cargar el propietario.';

          this.cdr.detectChanges();
        }
      });
  }

  cargarDocumentos(
    vecinoId?: number
  ): void {
    const id =
      vecinoId ?? this.vecino?.id;

    if (!id) {
      this.documentos = [];
      return;
    }

    this.cargandoDocumentos = true;
    this.errorDocumentos = '';

    this.documentoService
      .listarPorVecino(id)
      .subscribe({
        next: (documentos) => {
          this.documentos = [
            ...documentos
          ];

          this.cargandoDocumentos = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(
            'Error cargando documentos:',
            err
          );

          this.documentos = [];

          this.errorDocumentos =
            'No se pudieron cargar los documentos del propietario.';

          this.cargandoDocumentos = false;
          this.cdr.detectChanges();
        }
      });
  }

  guardar(): void {
    if (!this.vecino) {
      return;
    }

    this.guardando = true;
    this.mensaje = '';
    this.error = '';

    if (this.esNuevo) {
      this.crear();
      return;
    }

    this.actualizar();
  }

  crear(): void {
    if (!this.vecino) {
      return;
    }

    /*
     * El backend no permite recibir un ID al crear un propietario.
     *
     * El modelo de Angular mantiene temporalmente id: 0 para no romper
     * el tipado actual de Vecino, pero ese campo se elimina expresamente
     * del cuerpo JSON antes de realizar el POST.
     */
    const {
      id: _idNoEnviar,
      ...datosNuevoPropietario
    } = this.vecino;

    this.vecinoService
      .crearVecino(
        datosNuevoPropietario as Vecino
      )
      .subscribe({
        next: (data) => {
          this.mensaje =
            'Propietario creado correctamente.';

          this.guardando = false;

          this.router.navigate([
            '/vecinos/comunidad',
            data.comunidadId
          ]);
        },
        error: (err) => {
          console.error(
            'Error creando propietario:',
            err
          );

          this.error =
            err?.error?.detail
            || err?.error?.message
            || (
              typeof err?.error === 'string'
                ? err.error
                : ''
            )
            || 'No se pudo crear el propietario.';

          this.guardando = false;
          this.cdr.detectChanges();
        }
      });
  }

  actualizar(): void {
    if (
      !this.vecino
      || !this.vecino.id
    ) {
      return;
    }

    this.vecinoService
      .actualizarVecino(
        this.vecino.id,
        this.vecino
      )
      .subscribe({
        next: (data) => {
          this.vecino = {
            ...data
          };

          this.mensaje =
            'Propietario guardado correctamente.';

          this.guardando = false;
          this.cdr.detectChanges();

          setTimeout(() => {
            this.router.navigate([
              '/vecinos/comunidad',
              data.comunidadId
            ]);
          }, 800);
        },
        error: (err) => {
          console.error(
            'Error guardando propietario:',
            err
          );

          this.error =
            'No se pudo guardar el propietario.';

          this.guardando = false;
          this.cdr.detectChanges();
        }
      });
  }

  volver(): void {
    if (this.vecino?.comunidadId) {
      this.router.navigate([
        '/vecinos/comunidad',
        this.vecino.comunidadId
      ]);

      return;
    }

    this.router.navigate([
      '/comunidades'
    ]);
  }

  seleccionarDocumento(
    event: Event
  ): void {
    const input =
      event.target as HTMLInputElement;

    this.mensaje = '';
    this.error = '';
    this.errorDocumentos = '';

    if (
      !input.files
      || input.files.length === 0
    ) {
      this.archivoSeleccionado =
        undefined;

      return;
    }

    const archivo = input.files[0];

    const tiposPermitidos = [
      'application/pdf',
      'image/jpeg',
      'image/png'
    ];

    if (
      archivo.type
      && !tiposPermitidos.includes(
        archivo.type.toLowerCase()
      )
    ) {
      this.archivoSeleccionado =
        undefined;

      input.value = '';

      this.errorDocumentos =
        'Solo se permiten documentos PDF, JPG o PNG.';

      this.cdr.detectChanges();
      return;
    }

    const tamanioMaximo =
      10 * 1024 * 1024;

    if (archivo.size > tamanioMaximo) {
      this.archivoSeleccionado =
        undefined;

      input.value = '';

      this.errorDocumentos =
        'El documento no puede superar los 10 MB.';

      this.cdr.detectChanges();
      return;
    }

    this.archivoSeleccionado =
      archivo;
  }

  subirDocumento(): void {
    if (
      !this.vecino
      || !this.vecino.id
    ) {
      this.errorDocumentos =
        'Primero debe guardar el propietario.';

      this.cdr.detectChanges();
      return;
    }

    if (!this.archivoSeleccionado) {
      this.errorDocumentos =
        'Seleccione un documento PDF, JPG o PNG.';

      this.cdr.detectChanges();
      return;
    }

    this.subiendoDocumento = true;
    this.mensaje = '';
    this.error = '';
    this.errorDocumentos = '';

    const vecinoId =
      this.vecino.id;

    this.documentoService
      .subirMandatoFirmado(
        vecinoId,
        this.archivoSeleccionado
      )
      .subscribe({
        next: (documento) => {
          if (this.vecino) {
            this.vecino.rutaMandatoFirmado =
              `BD:${documento.id}`;
          }

          this.archivoSeleccionado =
            undefined;

          this.mensaje =
            'Mandato firmado subido correctamente.';

          this.subiendoDocumento = false;

          this.cargarDocumentos(
            vecinoId
          );

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(
            'Error subiendo documento:',
            err
          );

          this.errorDocumentos =
            'No se pudo subir el mandato firmado.';

          this.subiendoDocumento = false;
          this.cdr.detectChanges();
        }
      });
  }


  descargarMandatoSepa(): void {
    if (
      !this.vecino
      || !this.vecino.id
    ) {
      this.errorDocumentos =
        'Primero debe guardar el propietario.';

      this.cdr.detectChanges();
      return;
    }

    this.descargandoMandato = true;
    this.mensaje = '';
    this.error = '';
    this.errorDocumentos = '';

    const vecinoId = this.vecino.id;

    this.vecinoService
      .descargarMandatoPdf(vecinoId)
      .subscribe({
        next: (blob) => {
          const url =
            window.URL.createObjectURL(blob);

          const enlace =
            document.createElement('a');

          enlace.href = url;
          enlace.download =
            `mandato_sepa_${vecinoId}.pdf`;

          document.body.appendChild(enlace);
          enlace.click();
          enlace.remove();

          window.URL.revokeObjectURL(url);

          this.mensaje =
            'Mandato SEPA generado correctamente.';

          this.descargandoMandato = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(
            'Error generando mandato SEPA:',
            err
          );

          this.errorDocumentos =
            'No se pudo generar el mandato SEPA.';

          this.descargandoMandato = false;
          this.cdr.detectChanges();
        }
      });
  }

  get estadoMandato(): string {
    if (!this.vecino?.domiciliado) {
      return 'NO_DOMICILIADO';
    }

    if (
      this.vecino.rutaMandatoFirmado
      && this.vecino.rutaMandatoFirmado
        .startsWith('BD:')
    ) {
      return 'FIRMADO';
    }

    if (
      this.vecino.referenciaMandato
      && this.vecino.fechaMandato
    ) {
      return 'DATOS_INFORMADOS';
    }

    return 'PENDIENTE';
  }

  get textoEstadoMandato(): string {
    switch (this.estadoMandato) {
      case 'FIRMADO':
        return 'Mandato firmado almacenado';

      case 'DATOS_INFORMADOS':
        return 'Datos del mandato informados, sin documento firmado';

      case 'NO_DOMICILIADO':
        return 'Propietario no domiciliado';

      default:
        return 'Mandato pendiente';
    }
  }

  visualizarDocumento(
    documento: VecinoDocumento
  ): void {
    const url =
      this.documentoService
        .obtenerUrlVisualizacion(
          documento.id
        );

    window.open(
      url,
      '_blank',
      'noopener,noreferrer'
    );
  }

  descargarDocumento(
    documento: VecinoDocumento
  ): void {
    const url =
      this.documentoService
        .obtenerUrlDescarga(
          documento.id
        );

    window.open(
      url,
      '_blank',
      'noopener,noreferrer'
    );
  }

  eliminarDocumento(
    documento: VecinoDocumento
  ): void {
    if (
      this.eliminandoDocumentoId
      || !this.vecino?.id
    ) {
      return;
    }

    const confirmado = confirm(
      '¿Desea eliminar el documento '
      + `"${documento.nombreArchivo}"?`
    );

    if (!confirmado) {
      return;
    }

    this.eliminandoDocumentoId =
      documento.id;

    this.mensaje = '';
    this.errorDocumentos = '';

    this.documentoService
      .eliminar(documento.id)
      .subscribe({
        next: () => {
          if (
            this.vecino?.rutaMandatoFirmado
            === `BD:${documento.id}`
          ) {
            this.vecino.rutaMandatoFirmado =
              '';
          }

          this.mensaje =
            'Documento eliminado correctamente.';

          this.eliminandoDocumentoId =
            undefined;

          this.cargarDocumentos(
            this.vecino?.id
          );

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(
            'Error eliminando documento:',
            err
          );

          this.errorDocumentos =
            'No se pudo eliminar el documento.';

          this.eliminandoDocumentoId =
            undefined;

          this.cdr.detectChanges();
        }
      });
  }

  formatearTamanio(
    tamanio: number
  ): string {
    if (
      !Number.isFinite(tamanio)
      || tamanio <= 0
    ) {
      return '0 bytes';
    }

    if (tamanio < 1024) {
      return `${tamanio} bytes`;
    }

    if (tamanio < 1024 * 1024) {
      return (
        tamanio / 1024
      ).toFixed(1) + ' KB';
    }

    return (
      tamanio / (1024 * 1024)
    ).toFixed(2) + ' MB';
  }
}
