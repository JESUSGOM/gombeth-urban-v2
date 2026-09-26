import { Injectable, signal } from '@angular/core';

export type GombethDialogTipo =
  | 'info'
  | 'success'
  | 'warning'
  | 'error'
  | 'confirm';

export interface GombethDialogEstado {
  mensaje: string;
  tipo: GombethDialogTipo;
  textoAceptar: string;
  textoCancelar: string;
  mostrarCancelar: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class GombethDialogService {

  readonly estado =
    signal<GombethDialogEstado | null>(null);

  private resolver:
    ((resultado: boolean) => void) | null = null;

  info(mensaje: string): Promise<void> {
    return this.mostrarAviso(mensaje, 'info');
  }

  success(mensaje: string): Promise<void> {
    return this.mostrarAviso(mensaje, 'success');
  }

  warning(mensaje: string): Promise<void> {
    return this.mostrarAviso(mensaje, 'warning');
  }

  error(mensaje: string): Promise<void> {
    return this.mostrarAviso(mensaje, 'error');
  }

  confirm(
    mensaje: string,
    textoAceptar = 'Aceptar',
    textoCancelar = 'Cancelar'
  ): Promise<boolean> {

    return this.abrir({
      mensaje,
      tipo: 'confirm',
      textoAceptar,
      textoCancelar,
      mostrarCancelar: true
    });
  }

  resolverDialogo(resultado: boolean): void {

    const resolverActual = this.resolver;

    this.resolver = null;
    this.estado.set(null);

    if (resolverActual) {
      resolverActual(resultado);
    }
  }

  private mostrarAviso(
    mensaje: string,
    tipo: Exclude<GombethDialogTipo, 'confirm'>
  ): Promise<void> {

    return this.abrir({
      mensaje,
      tipo,
      textoAceptar: 'Aceptar',
      textoCancelar: 'Cancelar',
      mostrarCancelar: false
    }).then(() => undefined);
  }

  private abrir(
    estado: GombethDialogEstado
  ): Promise<boolean> {

    if (this.resolver) {
      this.resolver(false);
      this.resolver = null;
    }

    return new Promise<boolean>(resolve => {
      this.resolver = resolve;
      this.estado.set(estado);
    });
  }
}
