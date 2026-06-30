import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ComunidadActiva {
  id: number;
  nombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class ComunidadActivaService {

  private comunidadSubject =
    new BehaviorSubject<ComunidadActiva>({
      id: 17,
      nombre: 'Comunidad 17'
    });

  comunidad$ = this.comunidadSubject.asObservable();

  get comunidadActual(): ComunidadActiva {
    return this.comunidadSubject.value;
  }

  setComunidad(
    comunidad: ComunidadActiva
  ): void {
    this.comunidadSubject.next(comunidad);
    localStorage.setItem(
      'comunidadActiva',
      JSON.stringify(comunidad)
    );
  }

  cargarDesdeStorage(): void {
    const data =
      localStorage.getItem('comunidadActiva');

    if (!data) {
      return;
    }

    try {
      const comunidad =
        JSON.parse(data) as ComunidadActiva;

      if (comunidad?.id && comunidad?.nombre) {
        this.comunidadSubject.next(comunidad);
      }

    } catch {
      localStorage.removeItem('comunidadActiva');
    }
  }
}
