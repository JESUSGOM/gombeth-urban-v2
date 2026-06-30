import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ComunidadUsuario {
  id: number;
  nombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioContextoService {

  private http = inject(HttpClient);

  private readonly api =
    'http://localhost:8080/api/usuario/mis-comunidades';

  obtenerComunidades(
    usuarioId: number
  ): Observable<ComunidadUsuario[]> {
    return this.http.get<ComunidadUsuario[]>(
      `${this.api}?usuarioId=${usuarioId}`
    );
  }
}
