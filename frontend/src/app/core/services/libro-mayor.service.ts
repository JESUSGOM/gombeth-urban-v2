import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LibroMayorLinea {
  movimientoId: number;
  fecha: string;
  concepto: string;
  numeroAsiento: string;
  debe: number;
  haber: number;
  saldo: number;
}

export interface LibroMayor {
  comunidadId: number;
  cuentaId: number;
  totalDebe: number;
  totalHaber: number;
  saldoFinal: number;
  lineas: LibroMayorLinea[];
}

@Injectable({
  providedIn: 'root'
})
export class LibroMayorService {

  private http = inject(HttpClient);

  private readonly api =
    'http://localhost:8080/api/mayor';

  obtenerMayor(
    comunidadId: number,
    cuentaId: number,
    ejercicio: number
  ): Observable<LibroMayor> {

    return this.http.get<LibroMayor>(
      `${this.api}?comunidadId=${comunidadId}&cuentaId=${cuentaId}&ejercicio=${ejercicio}`
    );
  }
}
