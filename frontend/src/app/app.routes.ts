import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'home', canActivate: [authGuard], loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
  { path: 'clientes', canActivate: [authGuard], loadComponent: () => import('./pages/clientes/clientes.component').then(m => m.ClientesComponent) },
  { path: 'productos', canActivate: [authGuard], loadComponent: () => import('./pages/productos/productos.component').then(m => m.ProductosComponent) },
  { path: 'pedidos', canActivate: [authGuard], loadComponent: () => import('./pages/pedidos/pedidos.component').then(m => m.PedidosComponent) },
  { path: '**', redirectTo: '/home' },
];
