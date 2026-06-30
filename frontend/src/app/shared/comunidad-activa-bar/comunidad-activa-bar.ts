import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import {
  UsuarioContextoService,
  ComunidadUsuario
} from '../../core/services/usuario-contexto.service';

import {
  ComunidadStateService,
  ComunidadSeleccionada
} from '../../core/state/comunidad-state.service';

@Component({
  selector: 'app-comunidad-activa-bar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './comunidad-activa-bar.html',
  styleUrl: './comunidad-activa-bar.scss'
})
export class ComunidadActivaBar implements OnInit {

  private usuarioContextoService = inject(UsuarioContextoService);
  private comunidadState = inject(ComunidadStateService);

  comunidades: ComunidadUsuario[] = [];
  comunidadId: number | null = null;

  cargando = false;

  ngOnInit(): void {

    this.comunidadState.init();

    const usuarioRaw = localStorage.getItem('usuario');

    if (!usuarioRaw) {
      console.error('No hay usuario en localStorage');
      return;
    }

    const usuario = JSON.parse(usuarioRaw);

    const usuarioId = usuario.usuarioId ?? usuario.id;

    if (!usuarioId) {
      console.error('UsuarioId no válido');
      return;
    }

    this.cargando = true;

    this.usuarioContextoService.obtenerComunidades(usuarioId)
      .subscribe({
        next: data => {

          console.log('Comunidades recibidas:', data);

          this.comunidades = [...data];

          const actual = this.comunidadState.getComunidad();

          if (actual) {
            this.comunidadId = actual.id;
          } else if (data.length > 0) {

            this.comunidadId = data[0].id;

            this.comunidadState.setComunidad({
              id: data[0].id,
              nombre: data[0].nombre
            });
          }

          this.cargando = false;
        },

        error: err => {
          console.error(err);
          this.cargando = false;
        }
      });
  }

  seleccionarComunidad(): void {

    const comunidad = this.comunidades.find(
      c => c.id === Number(this.comunidadId)
    );

    if (!comunidad) return;

    const seleccionada: ComunidadSeleccionada = {
      id: comunidad.id,
      nombre: comunidad.nombre
    };

    this.comunidadState.setComunidad(seleccionada);

    console.log('Comunidad cambiada:', seleccionada);

    // ❌ IMPORTANTE: NO HACER reload
    // window.location.reload();

    // 👉 opcional: emitir cambio sin recargar app
  }
}
