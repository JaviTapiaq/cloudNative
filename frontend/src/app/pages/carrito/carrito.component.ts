import { Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CarritoService } from '../../services/carrito.service';
import { Pedido, PedidoService } from '../../services/pedido.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-carrito',
  imports: [CurrencyPipe, RouterLink],
  templateUrl: './carrito.component.html',
  styleUrl: './carrito.component.css'
})
export class CarritoComponent {
  private pedidoService = inject(PedidoService);
  private authService = inject(AuthService);
  carrito = inject(CarritoService);

  enviando = signal(false);
  error = signal<string | null>(null);
  pedidoCreado = signal<Pedido | null>(null);

  get usuario(): string | null {
    return this.authService.getUserName();
  }

  cambiarCantidad(productoId: number, cantidad: number): void {
    this.carrito.actualizar(productoId, cantidad);
  }

  /**
   * Crea el pedido. Solo se envian productos y cantidades: el backend valida el stock,
   * toma los precios vigentes e identifica al cliente a partir del token de Azure.
   * La respuesta llega de inmediato; las tareas asincronas siguen por RabbitMQ.
   */
  confirmar(): void {
    this.enviando.set(true);
    this.error.set(null);
    const pedido: Pedido = {
      detalles: this.carrito.items().map(i => ({ productoId: i.productoId, cantidad: i.cantidad })),
    };
    this.pedidoService.create(pedido).subscribe({
      next: (creado) => {
        this.pedidoCreado.set(creado);
        this.carrito.vaciar();
        this.enviando.set(false);
      },
      error: (err) => {
        this.error.set(err.error?.detail ?? 'No fue posible crear el pedido: ' + err.message);
        this.enviando.set(false);
      }
    });
  }
}
