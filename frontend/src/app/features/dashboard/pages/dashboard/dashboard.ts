import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  imports: [],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {

  private router = inject(Router);
  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);

  username = 'Usuario';

  totalComunidades = 0;
  totalVecinos = 0;
  totalIncidencias = 0;

  ngOnInit(): void {
    const usuario = JSON.parse(
      localStorage.getItem('usuario') || 'null'
    );

    if (usuario) {
      this.username = usuario.username;
    }

    const usuarioId = usuario?.usuarioId;

    this.http
      .get<any>(`http://localhost:8080/api/dashboard?usuarioId=${usuarioId}`)
      .subscribe({
        next: (data) => {
          console.log('DASHBOARD:', data);

          this.totalComunidades = data.totalComunidades ?? 0;
          this.totalVecinos = data.totalPropietarios ?? 0;
          this.totalIncidencias = data.totalIncidencias ?? 0;

          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error cargando dashboard:', err);
        }
      });
  }

  irAComunidades(): void {
    this.router.navigate(['/comunidades']);
  }
}
