import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Producto {
  id?: number;
  nombre: string;
  descripcion?: string;
  precio: number;
  stock: number;
  categoria?: string;
}

@Injectable({
  providedIn: 'root',
})
export class ProductoService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/productos`;
  // private baseUrl = 'https://n9mfpgvsah.execute-api.us-east-1.amazonaws.com/api/productos'; // AWS (ya no se utiliza)

  getAll(): Observable<Producto[]> {
    return this.http.get<Producto[]>(this.baseUrl);
  }

  getById(id: number): Observable<Producto> {
    return this.http.get<Producto>(`${this.baseUrl}/${id}`);
  }

  getByCategoria(categoria: string): Observable<Producto[]> {
    return this.http.get<Producto[]>(`${this.baseUrl}/categoria/${categoria}`);
  }

  create(producto: Producto): Observable<Producto> {
    return this.http.post<Producto>(this.baseUrl, producto);
  }

  update(id: number, producto: Producto): Observable<Producto> {
    return this.http.put<Producto>(`${this.baseUrl}/${id}`, producto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
