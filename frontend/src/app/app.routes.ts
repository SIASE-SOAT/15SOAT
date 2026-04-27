import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'acompanhar',
    loadComponent: () =>
      import('./features/ordens/acompanhar/acompanhar.component').then(m => m.AcompanharComponent),
  },
  {
    path: 'acompanhar/:numero',
    loadComponent: () =>
      import('./features/ordens/acompanhar/acompanhar.component').then(m => m.AcompanharComponent),
  },
  {
    path: 'mecanico',
    loadComponent: () =>
      import('./features/mecanico/mecanico.component').then(m => m.MecanicoComponent),
  },
  {
    path: 'mecanico/ordens/:id',
    data: { backLink: '/mecanico' },
    loadComponent: () =>
      import('./features/ordens/detalhe/ordem-detalhe.component').then(m => m.OrdemDetalheComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'clientes',
        loadComponent: () =>
          import('./features/clientes/lista/clientes-lista.component').then(m => m.ClientesListaComponent),
      },
      {
        path: 'veiculos',
        loadComponent: () =>
          import('./features/veiculos/lista/veiculos-lista.component').then(m => m.VeiculosListaComponent),
      },
      {
        path: 'pecas',
        loadComponent: () =>
          import('./features/pecas/lista/pecas-lista.component').then(m => m.PecasListaComponent),
      },
      {
        path: 'servicos',
        loadComponent: () =>
          import('./features/servicos/lista/servicos-lista.component').then(m => m.ServicosListaComponent),
      },
      {
        path: 'ordens',
        loadComponent: () =>
          import('./features/ordens/lista/ordens-lista.component').then(m => m.OrdensListaComponent),
      },
      {
        path: 'ordens/:id',
        loadComponent: () =>
          import('./features/ordens/detalhe/ordem-detalhe.component').then(m => m.OrdemDetalheComponent),
      },
      {
        path: 'pedidos-compra',
        loadComponent: () =>
          import('./features/pedidos-compra/lista/pedidos-compra-lista.component').then(m => m.PedidosCompraListaComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
