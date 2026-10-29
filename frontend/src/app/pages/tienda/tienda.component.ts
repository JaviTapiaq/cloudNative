import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Producto, ProductoService } from '../../services/producto.service';
import { CarritoService } from '../../services/carrito.service';

const ICONOS: Record<string, string> = {
  Computacion: '💻',
  Accesorios: '🖱️',
  Audio: '🎧',
  Oficina: '🪑',
};

@Component({
  selector: 'app-tienda',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './tienda.component.html',
  styleUrl: './tienda.component.css'
})
export class TiendaComponent implements OnInit {
  private productoService = inject(ProductoService);
  carrito = inject(CarritoService);

  productos = signal<Producto[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  agregado = signal<string | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    this.productoService.getAll().subscribe({
      next: (data) => {
        this.productos.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('No fue posible cargar el catalogo: ' + (err.error?.detail ?? err.message));
        this.loading.set(false);
      }
    });
  }

  icono(producto: Producto): string {
    return ICONOS[producto.categoria ?? ''] ?? '📦';
  }

  /** Unidades que aun se pueden agregar considerando lo que ya esta en el carrito. */
  disponible(producto: Producto): number {
    return producto.stock - this.carrito.cantidadDe(producto.id!);
  }

  agregar(producto: Producto): void {
    this.carrito.agregar(producto);
    this.agregado.set(producto.nombre);
    setTimeout(() => this.agregado.set(null), 2000);
  }
}
