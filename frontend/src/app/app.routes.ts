import { Routes } from '@angular/router';

import { MainLayout } from './layout/main-layout/main-layout';

import { Dashboard } from './features/dashboard/pages/dashboard/dashboard';
import { ComunidadesList } from './features/comunidades/pages/comunidades-list/comunidades-list';
import { Login } from './features/auth/pages/login/login';

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
    children: [
      {
        path: 'dashboard',
        component: Dashboard
      },
      {
        path: 'comunidades',
        component: ComunidadesList
      }
    ]
  },

  {
    path: '**',
    redirectTo: 'dashboard'
  }

];
