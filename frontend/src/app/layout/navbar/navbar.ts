import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ComunidadActivaBar } from '../../shared/comunidad-activa-bar/comunidad-activa-bar';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    ComunidadActivaBar
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar implements OnInit {

  username = 'Usuario';
  administradorNombre = 'Administrador';

  ngOnInit(): void {
    const usuario = JSON.parse(localStorage.getItem('usuario') || 'null');

    if (usuario) {
      this.username = usuario.username || 'Usuario';

      this.administradorNombre =
        usuario.administradorNombre ||
        `Administrador ID ${usuario.administradorId}`;
    }
  }
}
