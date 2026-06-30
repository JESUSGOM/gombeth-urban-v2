import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { UsuarioContextoService } from '../../core/services/usuario-contexto.service';
import { ComunidadStateService } from '../../core/state/comunidad-state.service';

@Component({
  selector: 'app-comunidad-activa-bar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './comunidad-activa-bar.html',
  styleUrl: './comunidad-activa-bar.scss'
})
export class ComunidadActivaBar implements OnInit {

  private usuarioService = inject(UsuarioContextoService);
  private comunidadState = inject(ComunidadStateService);

  comunidades: any[] = [];
  comunidadId: number | null = null;

  ngOnInit(): void {

    this.comunidadState.init();

    const usuario = JSON.parse(localStorage.getItem('usuario') || 'null');

    if (!usuario) return;

    this.usuarioService.obtenerComunidades(usuario.usuarioId)
      .subscribe(data => {

        this.comunidades = data;

        const actual = this.comunidadState.getComunidad();

        if (actual) {
          this.comunidadId = actual.id;
        } else if (data.length > 0) {
          this.comunidadId = data[0].id;
          this.seleccionar();
        }
      });
  }

  seleccionar(): void {

    const comunidad = this.comunidades.find(c => c.id == this.comunidadId);

    if (!comunidad) return;

    this.comunidadState.setComunidad({
      id: comunidad.id,
      nombre: comunidad.nombre
    });
  }
}
