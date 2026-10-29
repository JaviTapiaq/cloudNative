import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'home', canActivate: [authGuard], loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent) },
  // Compra
  { path: 'tienda', canActivate: [authGuard], loadComponent: () => import('./pages/tienda/tienda.component').then(m => m.TiendaComponent) },
  { path: 'carrito', canActivate: [authGuard], loadComponent: () => import('./pages/carrito/carrito.component').then(m => m.CarritoComponent) },
  { path: 'mis-pedidos', canActivate: [authGuard], loadComponent: () => import('./pages/mis-pedidos/mis-pedidos.component').then(m => m.MisPedidosComponent) },
  // Administracion
  { path: 'clientes', canActivate: [authGuard], loadComponent: () => import('./pages/clientes/clientes.component').then(m => m.ClientesComponent) },
  { path: 'productos', canActivate: [authGuard], loadComponent: () => import('./pages/productos/productos.component').then(m => m.ProductosComponent) },
  { path: 'pedidos', canActivate: [authGuard], loadComponent: () => import('./pages/pedidos/pedidos.component').then(m => m.PedidosComponent) },
  { path: 'monitor', canActivate: [authGuard], loadComponent: () => import('./pages/monitor/monitor.component').then(m => m.MonitorComponent) },
  { path: '**', redirectTo: '/home' },
];
