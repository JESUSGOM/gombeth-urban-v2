/*
 * Zone.js debe cargarse antes de iniciar Angular.
 *
 * De este modo Angular detectará automáticamente
 * las operaciones asíncronas, incluidas las peticiones HTTP.
 */
import 'zone.js';

import { bootstrapApplication } from '@angular/platform-browser';

import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch(error => {
    console.error(
      'Error iniciando Gombeth Urban:',
      error
    );
  });
