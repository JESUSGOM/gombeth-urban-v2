import { Component, OnInit, inject } from '@angular/core';
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

  username = 'Usuario';

  totalComunidades = 0;
  totalVecinos = 0;
  totalIncidencias = 0;

  ngOnInit(): void {

    const usuario = JSON.parse(
      localStorage.getItem('usuario') || 'null'
    );

    if (!usuario) {
      return;
    }

    this.username = usuario.username;

    this.http
      .get<any>('/api/dashboard')
      .subscribe({

        next: data => {

          this.totalComunidades = data.totalComunidades ?? 0;
          this.totalVecinos = data.totalPropietarios ?? 0;
          this.totalIncidencias = data.totalIncidencias ?? 0;

        },

        error: err => console.error(err)

      });

  }

  irAComunidades(): void {
    this.router.navigate(['/comunidades']);
  }

}
