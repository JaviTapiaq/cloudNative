import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DetallePedido {
  productoId: number;
  cantidad: number;
  precioUnitario: number;
}

export interface Pedido {
  id?: number;
  clienteId: number;
  detalles: DetallePedido[];
  estado?: string;
  total?: number;
  fechaCreacion?: string;
}

@Injectable({
  providedIn: 'root',
})
export class PedidoService {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api/pedidos';

  getAll(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.baseUrl);
  }

  getById(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.baseUrl}/${id}`);
  }

  getByCliente(clienteId: number): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.baseUrl}/cliente/${clienteId}`);
  }

  create(pedido: Pedido): Observable<Pedido> {
    return this.http.post<Pedido>(this.baseUrl, pedido);
  }

  updateEstado(id: number, estado: string): Observable<Pedido> {
    return this.http.patch<Pedido>(`${this.baseUrl}/${id}/estado`, { estado });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
