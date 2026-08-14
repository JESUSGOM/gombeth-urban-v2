import {
  Component,
  inject
} from '@angular/core';

import {
  RouterOutlet
} from '@angular/router';

import {
  AuditoriaFrontendService
} from './core/services/auditoria-frontend.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {

  private readonly auditoriaFrontend =
    inject(AuditoriaFrontendService);

  constructor() {
    this.auditoriaFrontend.inicializar();
  }
}
