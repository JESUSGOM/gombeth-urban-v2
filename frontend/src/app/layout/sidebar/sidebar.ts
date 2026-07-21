import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { UsuarioContextoService } from '../../core/services/usuario-contexto.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar {

  constructor(
    private router: Router,
    private usuarioContextoService: UsuarioContextoService
  ) {}

  cerrarSesion(): void {

    this.usuarioContextoService.limpiarCache();

    localStorage.clear();
    sessionStorage.clear();

    this.router.navigate(['/login']);

  }

}
