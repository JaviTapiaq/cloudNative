import { Injectable, computed, effect, signal } from '@angular/core';
import { Producto } from './producto.service';

export interface ItemCarrito {
  productoId: number;
  nombre: string;
  precio: number;
  stock: number;
  cantidad: number;
}

const STORAGE_KEY = 'pedidos360.carrito';

/**
 * Estado del carrito en el navegador. Se guarda en localStorage para no perderlo al recargar.
 * Los precios aqui son solo referenciales: el backend recalcula el total con los precios vigentes.
 */
@Injectable({
  providedIn: 'root',
})
export class CarritoService {
  readonly items = signal<ItemCarrito[]>(this.cargar());
  readonly cantidadTotal = computed(() => this.items().reduce((suma, i) => suma + i.cantidad, 0));
  readonly total = computed(() => this.items().reduce((suma, i) => suma + i.precio * i.cantidad, 0));

  constructor() {
    effect(() => this.guardar(this.items()));
  }

  cantidadDe(productoId: number): number {
    return this.items().find(i => i.productoId === productoId)?.cantidad ?? 0;
  }

  agregar(producto: Producto): void {
    if (producto.id == null) {
      return;
    }
    const existente = this.items().find(i => i.productoId === producto.id);
    if (existente) {
      this.actualizar(producto.id, existente.cantidad + 1);
      return;
    }
    if (producto.stock < 1) {
      return;
    }
    this.items.update(items => [...items, {
      productoId: producto.id!,
      nombre: producto.nombre,
      precio: producto.precio,
      stock: producto.stock,
      cantidad: 1,
    }]);
  }

  actualizar(productoId: number, cantidad: number): void {
    if (cantidad <= 0) {
      this.quitar(productoId);
      return;
    }
    this.items.update(items => items.map(i =>
      i.productoId === productoId ? { ...i, cantidad: Math.min(cantidad, i.stock) } : i));
  }

  quitar(productoId: number): void {
    this.items.update(items => items.filter(i => i.productoId !== productoId));
  }

  vaciar(): void {
    this.items.set([]);
  }

  private cargar(): ItemCarrito[] {
    try {
      const guardado = localStorage.getItem(STORAGE_KEY);
      return guardado ? JSON.parse(guardado) as ItemCarrito[] : [];
    } catch {
      return [];
    }
  }

  private guardar(items: ItemCarrito[]): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      // Sin almacenamiento disponible: el carrito vive solo en memoria
    }
  }
}
