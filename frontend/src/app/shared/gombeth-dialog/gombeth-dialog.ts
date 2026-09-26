import {
  Component,
  HostListener,
  inject
} from '@angular/core';

import {
  GombethDialogService
} from './gombeth-dialog.service';

@Component({
  selector: 'app-gombeth-dialog',
  templateUrl: './gombeth-dialog.html',
  styleUrl: './gombeth-dialog.scss'
})
export class GombethDialogComponent {

  private readonly dialogService =
    inject(GombethDialogService);

  readonly estado =
    this.dialogService.estado;

  aceptar(): void {
    this.dialogService.resolverDialogo(true);
  }

  cancelar(): void {
    this.dialogService.resolverDialogo(false);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {

    const dialogo = this.estado();

    if (!dialogo) {
      return;
    }

    if (dialogo.mostrarCancelar) {
      this.cancelar();
    }
  }
}
