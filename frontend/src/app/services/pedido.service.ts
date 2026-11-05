import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DetallePedido {
  productoId: number;
  cantidad: number;
  // El backend toma el precio y el nombre vigentes desde ms-productos
  precioUnitario?: number;
  nombreProducto?: string;
}

export interface Pedido {
  id?: number;
  clienteId?: number;
  clienteEmail?: string;
  clienteNombre?: string;
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
  private baseUrl = `${environment.apiUrl}/pedidos`;
  // private baseUrl = 'https://n9mfpgvsah.execute-api.us-east-1.amazonaws.com/api/pedidos'; // AWS (ya no se utiliza)

  getAll(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(this.baseUrl);
  }

  getById(id: number): Observable<Pedido> {
    return this.http.get<Pedido>(`${this.baseUrl}/${id}`);
  }

  getByCliente(clienteId: number): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.baseUrl}/cliente/${clienteId}`);
  }

  /** Pedidos del usuario autenticado (el BFF lo identifica a partir del JWT). */
  getMios(): Observable<Pedido[]> {
    return this.http.get<Pedido[]>(`${this.baseUrl}/mios`);
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
