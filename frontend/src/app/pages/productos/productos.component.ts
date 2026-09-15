import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductoService, Producto } from '../../services/producto.service';

@Component({
  selector: 'app-productos',
  imports: [FormsModule],
  templateUrl: './productos.component.html',
  styleUrl: './productos.component.css'
})
export class ProductosComponent implements OnInit {
  private productoService = inject(ProductoService);

  productos = signal<Producto[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  nuevoProducto: Producto = { nombre: '', descripcion: '', precio: 0, stock: 0, categoria: '' };
  editingId: number | null = null;

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos(): void {
    this.loading.set(true);
    this.productoService.getAll().subscribe({
      next: (data) => {
        this.productos.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar productos: ' + err.message);
        this.loading.set(false);
      }
    });
  }

  guardar(): void {
    if (this.editingId) {
      this.productoService.update(this.editingId, this.nuevoProducto).subscribe({
        next: () => {
          this.cargarProductos();
          this.limpiarForm();
        },
        error: (err) => this.error.set('Error al actualizar: ' + err.message)
      });
    } else {
      this.productoService.create(this.nuevoProducto).subscribe({
        next: () => {
          this.cargarProductos();
          this.limpiarForm();
        },
        error: (err) => this.error.set('Error al crear: ' + err.message)
      });
    }
  }

  editar(producto: Producto): void {
    this.editingId = producto.id!;
    this.nuevoProducto = { ...producto };
  }

  eliminar(id: number): void {
    this.productoService.delete(id).subscribe({
      next: () => this.cargarProductos(),
      error: (err) => this.error.set('Error al eliminar: ' + err.message)
    });
  }

  limpiarForm(): void {
    this.editingId = null;
    this.nuevoProducto = { nombre: '', descripcion: '', precio: 0, stock: 0, categoria: '' };
  }
}
