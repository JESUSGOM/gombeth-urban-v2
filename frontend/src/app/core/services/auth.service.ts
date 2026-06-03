import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

export interface UsuarioLogin {
  usuarioId: number;
  username: string;
  administradorId: number;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private http = inject(HttpClient);
  private router = inject(Router);

  private apiUrl = 'http://localhost:8080/api/auth/login';

  login(username: string, password: string) {
    return this.http.post<any>(this.apiUrl, {
      username,
      password
    });
  }

  guardarUsuario(resp: any): void {
    localStorage.setItem('usuario', JSON.stringify({
      usuarioId: resp.usuarioId,
      username: resp.username,
      administradorId: resp.administradorId
    }));
  }

  getUsuario(): UsuarioLogin | null {
    const raw = localStorage.getItem('usuario');

    if (!raw) {
      return null;
    }

    return JSON.parse(raw);
  }

  estaLogueado(): boolean {
    return this.getUsuario() !== null;
  }

  logout(): void {
    localStorage.removeItem('usuario');
    this.router.navigate(['/login']);
  }
}
