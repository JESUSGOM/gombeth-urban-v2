import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-navbar',
  imports: [],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit {

  username = 'Usuario';
  administradorNombre = 'Administrador';
  iniciales = 'US';

  ngOnInit(): void {
    const usuario = JSON.parse(
      localStorage.getItem('usuario') || 'null'
    );

    if (usuario) {
      this.username = usuario.username || 'Usuario';

      this.administradorNombre =
        usuario.administradorNombre ||
        `Administrador ID ${usuario.administradorId}`;

      this.iniciales = this.username
        .substring(0, 2)
        .toUpperCase();
    }
  }
}
