import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection
} from '@angular/core';

import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),

    /*
     * Activa la detección automática tradicional de Angular.
     *
     * Las respuestas HTTP, promesas, temporizadores y eventos
     * actualizarán automáticamente las pantallas sin necesidad
     * de cambiar el combo, hacer clic o forzar manualmente
     * ChangeDetectorRef en cada componente.
     */
    provideZoneChangeDetection({
      eventCoalescing: false,
      runCoalescing: false
    }),

    provideRouter(routes),
    provideHttpClient()
  ]
};
