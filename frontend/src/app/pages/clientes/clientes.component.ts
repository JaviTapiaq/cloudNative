import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ClienteService, Cliente } from '../../services/cliente.service';

@Component({
  selector: 'app-clientes',
  imports: [FormsModule],
  templateUrl: './clientes.component.html',
  styleUrl: './clientes.component.css'
})
export class ClientesComponent implements OnInit {
  private clienteService = inject(ClienteService);

  clientes = signal<Cliente[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  nuevoCliente: Cliente = { nombre: '', apellido: '', email: '', telefono: '', direccion: '' };
  editingId: number | null = null;

  ngOnInit(): void {
    this.cargarClientes();
  }

  cargarClientes(): void {
    this.loading.set(true);
    this.clienteService.getAll().subscribe({
      next: (data) => {
        this.clientes.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Error al cargar clientes: ' + err.message);
        this.loading.set(false);
      }
    });
  }

  guardar(): void {
    if (this.editingId) {
      this.clienteService.update(this.editingId, this.nuevoCliente).subscribe({
        next: () => {
          this.cargarClientes();
          this.limpiarForm();
        },
        error: (err) => this.error.set('Error al actualizar: ' + err.message)
      });
    } else {
      this.clienteService.create(this.nuevoCliente).subscribe({
        next: () => {
          this.cargarClientes();
          this.limpiarForm();
        },
        error: (err) => this.error.set('Error al crear: ' + err.message)
      });
    }
  }

  editar(cliente: Cliente): void {
    this.editingId = cliente.id!;
    this.nuevoCliente = { ...cliente };
  }

  eliminar(id: number): void {
    this.clienteService.delete(id).subscribe({
      next: () => this.cargarClientes(),
      error: (err) => this.error.set('Error al eliminar: ' + err.message)
    });
  }

  limpiarForm(): void {
    this.editingId = null;
    this.nuevoCliente = { nombre: '', apellido: '', email: '', telefono: '', direccion: '' };
  }
}
