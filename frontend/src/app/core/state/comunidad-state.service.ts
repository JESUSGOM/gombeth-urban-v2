import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

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

  comunidad$ = this.comunidadSubject.asObservable();

  init(): void {
    const data = localStorage.getItem('comunidadActiva');

    if (data) {
      this.comunidadSubject.next(JSON.parse(data));
    }
  }

  setComunidad(comunidad: ComunidadSeleccionada): void {
    this.comunidadSubject.next(comunidad);
    localStorage.setItem(
      'comunidadActiva',
      JSON.stringify(comunidad)
    );
  }

  getComunidad(): ComunidadSeleccionada | null {
    return this.comunidadSubject.value;
  }
}
