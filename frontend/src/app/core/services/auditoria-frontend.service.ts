import {
  DOCUMENT
} from '@angular/common';

import {
  HttpClient
} from '@angular/common/http';

import {
  Injectable,
  inject
} from '@angular/core';

import {
  NavigationEnd,
  Router
} from '@angular/router';

import {
  EMPTY,
  catchError,
  filter
} from 'rxjs';

import {
  AuthService
} from './auth.service';

import {
  ComunidadStateService
} from '../state/comunidad-state.service';

interface EventoFrontend {
  tipoEvento: string;
  ruta: string;
  comunidadId: number | null;
  comunidadNombre: string | null;
  elemento?: string | null;
  texto?: string | null;
  idElemento?: string | null;
  nombreElemento?: string | null;
  tipoElemento?: string | null;
  detalle?: string | null;
  fichero?: string | null;
  linea?: number | null;
  columna?: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class AuditoriaFrontendService {

  private readonly http =
    inject(HttpClient);

  private readonly router =
    inject(Router);

  private readonly authService =
    inject(AuthService);

  private readonly comunidadState =
    inject(ComunidadStateService);

  private readonly document =
    inject(DOCUMENT);

  private inicializado = false;

  inicializar(): void {

    if (this.inicializado) {
      return;
    }

    this.inicializado = true;

    this.router.events
      .pipe(
        filter(
          (evento): evento is NavigationEnd =>
            evento instanceof NavigationEnd
        )
      )
      .subscribe(evento => {

        this.enviar({
          tipoEvento: 'NAVEGACION',
          ruta: evento.urlAfterRedirects,
          ...this.obtenerComunidad()
        });
      });

    this.document.addEventListener(
      'click',
      event => this.registrarClick(event),
      true
    );

    this.document.addEventListener(
      'change',
      event => this.registrarCambio(event),
      true
    );

    this.document.addEventListener(
      'submit',
      event => this.registrarSubmit(event),
      true
    );

    window.addEventListener(
      'error',
      event => {

        this.enviar({
          tipoEvento: 'ERROR_JAVASCRIPT',
          ruta: this.router.url,
          ...this.obtenerComunidad(),
          detalle: this.limitar(
            event.message,
            1000
          ),
          fichero: this.limitar(
            event.filename,
            500
          ),
          linea: event.lineno || null,
          columna: event.colno || null
        });
      }
    );

    window.addEventListener(
      'unhandledrejection',
      event => {

        this.enviar({
          tipoEvento: 'PROMESA_RECHAZADA',
          ruta: this.router.url,
          ...this.obtenerComunidad(),
          detalle: this.limitar(
            this.descripcionError(
              event.reason
            ),
            2000
          )
        });
      }
    );
  }

  private registrarClick(
    event: Event
  ): void {

    const target =
      event.target;

    if (!(target instanceof Element)) {
      return;
    }

    const elemento =
      target.closest(
        [
          'button',
          'a',
          '[role="button"]',
          'input[type="button"]',
          'input[type="submit"]',
          'input[type="checkbox"]',
          'input[type="radio"]'
        ].join(',')
      );

    if (!(elemento instanceof HTMLElement)) {
      return;
    }

    this.enviar({
      tipoEvento: 'CLICK',
      ruta: this.router.url,
      ...this.obtenerComunidad(),
      elemento: elemento.tagName.toLowerCase(),
      texto: this.obtenerTextoElemento(
        elemento
      ),
      idElemento: elemento.id || null,
      nombreElemento:
        elemento.getAttribute('name'),
      tipoElemento:
        elemento.getAttribute('type')
    });
  }

  private registrarCambio(
    event: Event
  ): void {

    const target =
      event.target;

    if (!(target instanceof HTMLElement)) {
      return;
    }

    const tag =
      target.tagName.toLowerCase();

    if (
      tag !== 'input'
      && tag !== 'select'
      && tag !== 'textarea'
    ) {
      return;
    }

    /*
     * No se registra el valor introducido en el campo.
     * Los valores de negocio que llegan al servidor quedan
     * auditados en el cuerpo sanitizado de la petición HTTP.
     */
    this.enviar({
      tipoEvento: 'CAMBIO_CAMPO',
      ruta: this.router.url,
      ...this.obtenerComunidad(),
      elemento: tag,
      idElemento: target.id || null,
      nombreElemento:
        target.getAttribute('name'),
      tipoElemento:
        target.getAttribute('type')
    });
  }

  private registrarSubmit(
    event: Event
  ): void {

    const target =
      event.target;

    if (!(target instanceof HTMLFormElement)) {
      return;
    }

    this.enviar({
      tipoEvento: 'ENVIO_FORMULARIO',
      ruta: this.router.url,
      ...this.obtenerComunidad(),
      elemento: 'form',
      idElemento: target.id || null,
      nombreElemento:
        target.getAttribute('name')
    });
  }

  private enviar(
    evento: EventoFrontend
  ): void {

    /*
     * El endpoint de auditoría exige sesión autenticada.
     * Login, intentos fallidos y cambio de contraseña se
     * auditan directamente en el backend.
     */
    if (!this.authService.getUsuario()) {
      return;
    }

    this.http
      .post<void>(
        '/api/auditoria/frontend',
        evento
      )
      .pipe(
        catchError(() => EMPTY)
      )
      .subscribe();
  }

  private obtenerComunidad(): {
    comunidadId: number | null;
    comunidadNombre: string | null;
  } {

    const comunidad =
      this.comunidadState.getComunidad();

    return {
      comunidadId:
        comunidad?.id ?? null,
      comunidadNombre:
        comunidad?.nombre ?? null
    };
  }

  private obtenerTextoElemento(
    elemento: HTMLElement
  ): string | null {

    const ariaLabel =
      elemento.getAttribute(
        'aria-label'
      );

    if (ariaLabel) {
      return this.limitar(
        ariaLabel,
        300
      );
    }

    const title =
      elemento.getAttribute(
        'title'
      );

    if (title) {
      return this.limitar(
        title,
        300
      );
    }

    const texto =
      elemento.textContent
        ?.replace(/\s+/g, ' ')
        .trim();

    return texto
      ? this.limitar(texto, 300)
      : null;
  }

  private descripcionError(
    error: unknown
  ): string {

    if (error instanceof Error) {
      return `${error.name}: ${error.message}`;
    }

    if (typeof error === 'string') {
      return error;
    }

    try {
      return JSON.stringify(error);
    } catch {
      return String(error);
    }
  }

  private limitar(
    valor: string | null | undefined,
    maximo: number
  ): string | null {

    if (!valor) {
      return null;
    }

    const limpio =
      valor
        .replace(/[\r\n\t]+/g, ' ')
        .trim();

    if (limpio.length <= maximo) {
      return limpio;
    }

    return `${limpio.substring(0, maximo)}…`;
  }
}
