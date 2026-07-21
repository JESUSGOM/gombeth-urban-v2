import { Injectable } from '@angular/core';
import {
  BehaviorSubject,
  distinctUntilChanged
} from 'rxjs';

export interface ComunidadSeleccionada {
  id: number;
  nombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class ComunidadStateService {

  private comunidadSubject =
    new BehaviorSubject<ComunidadSeleccionada | null>(null);

  readonly comunidad$ = this.comunidadSubject
    .asObservable()
    .pipe(
      distinctUntilChanged((anterior, actual) =>
        anterior?.id === actual?.id &&
        anterior?.nombre === actual?.nombre
      )
    );

  /**
   * Recupera la comunidad guardada.
   *
   * Puede ser llamado desde varios componentes sin provocar
   * emisiones repetidas de la misma comunidad.
   */
  init(): void {
    const data = localStorage.getItem('comunidadActiva');

    if (!data) {
      if (this.comunidadSubject.value !== null) {
        this.comunidadSubject.next(null);
      }

      return;
    }

    try {
      const comunidad =
        JSON.parse(data) as ComunidadSeleccionada;

      if (
        !comunidad ||
        !Number.isInteger(Number(comunidad.id)) ||
        Number(comunidad.id) <= 0 ||
        !comunidad.nombre
      ) {
        localStorage.removeItem('comunidadActiva');
        this.comunidadSubject.next(null);
        return;
      }

      const comunidadNormalizada: ComunidadSeleccionada = {
        id: Number(comunidad.id),
        nombre: comunidad.nombre
      };

      const actual = this.comunidadSubject.value;

      const esLaMisma =
        actual?.id === comunidadNormalizada.id &&
        actual?.nombre === comunidadNormalizada.nombre;

      if (!esLaMisma) {
        this.comunidadSubject.next(comunidadNormalizada);
      }

    } catch (error) {
      console.error(
        'No se pudo recuperar la comunidad activa:',
        error
      );

      localStorage.removeItem('comunidadActiva');
      this.comunidadSubject.next(null);
    }
  }

  /**
   * Cambia la comunidad seleccionada.
   *
   * Si se intenta seleccionar otra vez la misma comunidad,
   * no se vuelve a emitir innecesariamente.
   */
  setComunidad(comunidad: ComunidadSeleccionada): void {
    const comunidadNormalizada: ComunidadSeleccionada = {
      id: Number(comunidad.id),
      nombre: comunidad.nombre
    };

    localStorage.setItem(
      'comunidadActiva',
      JSON.stringify(comunidadNormalizada)
    );

    const actual = this.comunidadSubject.value;

    const esLaMisma =
      actual?.id === comunidadNormalizada.id &&
      actual?.nombre === comunidadNormalizada.nombre;

    if (esLaMisma) {
      return;
    }

    this.comunidadSubject.next(comunidadNormalizada);
  }

  getComunidad(): ComunidadSeleccionada | null {
    return this.comunidadSubject.value;
  }

  limpiar(): void {
    localStorage.removeItem('comunidadActiva');
    this.comunidadSubject.next(null);
  }
}
