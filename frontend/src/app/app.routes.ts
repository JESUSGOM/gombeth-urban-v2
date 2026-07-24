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
import { RemesasList } from './features/remesas/pages/remesas-list/remesas-list';
import { RemesaDetalle } from './features/remesas/pages/remesa-detalle/remesa-detalle';
import { MovimientosList } from './features/tesoreria/pages/movimientos-list/movimientos-list';

import { ConceptosList } from './features/conceptos/pages/conceptos-list/conceptos-list';
import { ConceptosEdit } from './features/conceptos/pages/conceptos-edit/conceptos-edit';

import { DiarioListComponent } from './features/tesoreria/pages/diario-list/diario-list';

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
        path: 'comunidades/nueva',
        component: ComunidadEdit
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
        path: 'recibos',
        component: RecibosList
      },

      {
        path: 'recibos/comunidad/:id',
        component: RecibosList
      },

      {
        path: 'remesas',
        component: RemesasList
      },

      {
        path: 'remesas/:id',
        component: RemesaDetalle
      },

      /*
       * Ruta utilizada por el botón RM
       * del listado de comunidades.
       */
      {
        path: 'remesas/comunidad/:id',
        component: RemesasList
      },

      {
        path: 'tesoreria/movimientos',
        component: MovimientosList
      },

      {
        path: 'tesoreria/diario',
        component: DiarioListComponent
      },

      {
        path: 'conceptos',
        component: ConceptosList
      },

      {
        path: 'conceptos/comunidad/:id',
        component: ConceptosList
      },

      {
        path: 'conceptos/comunidad/:id/nuevo',
        component: ConceptosEdit
      },

      {
        path: 'conceptos/comunidad/:id/editar/:conceptoId',
        component: ConceptosEdit
      },

      {
        path: 'tesoreria/mayor',
        loadComponent: () =>
          import('./features/tesoreria/pages/mayor/libro-mayor')
            .then(m => m.LibroMayorComponent)
      },

      {
        path: 'tesoreria/balance',
        loadComponent: () =>
          import('./features/tesoreria/pages/balance/balance')
            .then(m => m.BalanceComponent)
      },

      {
        path: 'configuracion/cuentas-presentador',
        loadComponent: () =>
          import(
            './features/configuracion/pages/cuentas-presentador/cuentas-presentador'
          ).then(m => m.CuentasPresentador)
      },

      {
        path: 'incidencias',
        loadComponent: () =>
          import('./features/incidencias/pages/incidencias-list/incidencias-list')
            .then(m => m.IncidenciasList)
      },

      {
        path: 'incidencias/nueva',
        loadComponent: () =>
          import(
            './features/incidencias/pages/incidencias-edit/incidencias-edit'
            ).then(m => m.IncidenciasEdit)
      },

      {
        path: 'incidencias/:id',
        loadComponent: () =>
          import(
            './features/incidencias/pages/incidencias-edit/incidencias-edit'
            ).then(m => m.IncidenciasEdit)
      }

    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }

];
