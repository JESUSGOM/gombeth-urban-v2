import { Routes } from '@angular/router';

import { MainLayout } from './layout/main-layout/main-layout';

import { Dashboard } from './features/dashboard/pages/dashboard/dashboard';
import { ComunidadesList } from './features/comunidades/pages/comunidades-list/comunidades-list';
import { Login } from './features/auth/pages/login/login';
import { ComunidadEdit } from './features/comunidades/pages/comunidad-edit/comunidad-edit';
import { VecinosList } from './features/vecinos/pages/vecinos-list/vecinos-list';
import { VecinoEdit } from './features/vecinos/pages/vecino-edit/vecino-edit';
import { PresupuestosList } from './features/presupuestos/pages/presupuestos-list/presupuestos-list';
import { RecibosList } from './features/recibos/pages/recibos-list/recibos-list';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [

  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },

  {
    path: 'login',
    component: Login
  },

  {
    path: '',
    component: MainLayout,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        component: Dashboard
      },
      {
        path: 'comunidades',
        component: ComunidadesList
      },
      {
        path: 'comunidades/editar/:id',
        component: ComunidadEdit
      },
      {
        path: 'vecinos',
        component: VecinosList
      },
      {
        path: 'vecinos/comunidad/:id',
        component: VecinosList
      },
      {
        path: 'vecinos/editar/:id',
        component: VecinoEdit
      },
      {
        path: 'vecinos/nuevo/comunidad/:comunidadId',
        component: VecinoEdit
      },
      {
        path: 'presupuestos',
        component: PresupuestosList
      },
      {
        path: 'recibos/comunidad/:id',
        component: RecibosList
      }
    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }

];
