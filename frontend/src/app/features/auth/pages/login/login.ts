import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {

  private http = inject(HttpClient);
  private router = inject(Router);

  username = '';
  password = '';
  error = '';
  cargando = false;

  login(): void {
    this.error = '';
    this.cargando = true;

    this.http.post<any>('http://localhost:8080/api/auth/login', {
      username: this.username,
      password: this.password
    }).subscribe({
      next: (resp) => {
        this.cargando = false;

        if (!resp.ok) {
          this.error = resp.mensaje || 'Login incorrecto';
          return;
        }

        localStorage.setItem('usuario', JSON.stringify({
          usuarioId: resp.usuarioId,
          username: resp.username,
          administradorId: resp.administradorId,
          administradorNombre: resp.administradorNombre
        }));

        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error('Error login:', err);
        this.cargando = false;
        this.error = 'No se pudo conectar con el servidor.';
      }
    });
  }
}
