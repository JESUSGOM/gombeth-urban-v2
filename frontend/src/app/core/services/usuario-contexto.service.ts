import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';

export interface ComunidadUsuario {
  id: number;
  nombre: string;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioContextoService {

  private http = inject(HttpClient);

  private readonly api = '/api/usuario/mis-comunidades';

  private comunidadesCache: ComunidadUsuario[] = [];

  obtenerComunidades(usuarioId: number): Observable<ComunidadUsuario[]> {

    if (this.comunidadesCache.length > 0) {
      return of(this.comunidadesCache);
    }

    const key = `comunidades_usuario_${usuarioId}`;
    const guardadas = localStorage.getItem(key);

    if (guardadas) {
      this.comunidadesCache = JSON.parse(guardadas);
      return of(this.comunidadesCache as ComunidadUsuario[]);
    }

    return this.http
      .get<ComunidadUsuario[]>(this.api)
      .pipe(
        tap(data => {
          this.comunidadesCache = data;
          localStorage.setItem(key, JSON.stringify(data));
        })
      );
  }

  limpiarCache(): void {
    this.comunidadesCache = [];

    Object.keys(localStorage)
      .filter(k => k.startsWith('comunidades_usuario_'))
      .forEach(k => localStorage.removeItem(k));
  }
}
