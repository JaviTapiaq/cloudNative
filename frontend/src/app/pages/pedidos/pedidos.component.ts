import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { PedidoService, Pedido } from '../../services/pedido.service';

@Component({
  selector: 'app-pedidos',
  imports: [FormsModule, DatePipe],
  templateUrl: './pedidos.component.html',
  styleUrl: './pedidos.component.css'
})
export class PedidosComponent implements OnInit {
  private pedidoService = inject(PedidoService);

  pedidos = signal<Pedido[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.cargarPedidos();
  }

  cargarPedidos(): void {
    this.loading.set(true);
    this.pedidoService.getAll().subscribe({
      next: (data) => {
        this.pedidos.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar pedidos: ' + err.message);
        this.loading.set(false);
      }
    });
  }

  cambiarEstado(id: number, estado: string): void {
    this.pedidoService.updateEstado(id, estado).subscribe({
      next: () => this.cargarPedidos(),
      error: (err) => this.error.set('Error al actualizar estado: ' + err.message)
    });
  }

  eliminar(id: number): void {
    this.pedidoService.delete(id).subscribe({
      next: () => this.cargarPedidos(),
      error: (err) => this.error.set('Error al eliminar: ' + err.message)
    });
  }
}
