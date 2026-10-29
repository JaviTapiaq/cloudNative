import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, forkJoin, interval, startWith, switchMap } from 'rxjs';
import { ColaInfo, MensajeriaService, RegistroProcesamiento, TipoFallo } from '../../services/mensajeria.service';

const INTERVALO_MS = 3000;

/**
 * Panel para demostrar la mensajeria: mensajes pendientes por cola, mensajes en cada DLQ
 * y bitacora de ACK / reintentos / rechazos. Se actualiza solo cada 3 segundos.
 */
@Component({
  selector: 'app-monitor',
  imports: [DatePipe],
  templateUrl: './monitor.component.html',
  styleUrl: './monitor.component.css'
})
export class MonitorComponent implements OnInit {
  private mensajeriaService = inject(MensajeriaService);
  private destroyRef = inject(DestroyRef);

  colas = signal<ColaInfo[]>([]);
  registro = signal<RegistroProcesamiento[]>([]);
  error = signal<string | null>(null);
  aviso = signal<string | null>(null);
  actualizado = signal<Date | null>(null);

  colasPrincipales = computed(() => this.colas().filter(c => !this.esDlq(c)));
  dlqs = computed(() => this.colas().filter(c => this.esDlq(c)));
  totalEnDlq = computed(() => this.dlqs().reduce((suma, c) => suma + c.mensajes, 0));

  ngOnInit(): void {
    interval(INTERVALO_MS).pipe(
      startWith(0),
      // El error se maneja por consulta para que el sondeo siga si ms-mensajeria se reinicia
      switchMap(() => forkJoin({
        colas: this.mensajeriaService.getColas(),
        registro: this.mensajeriaService.getRegistro(),
      }).pipe(catchError((err) => {
        this.error.set(err.status === 403
          ? 'Acceso denegado: el monitor requiere el rol Admin.'
          : 'No fue posible consultar ms-mensajeria: ' + (err.error?.detail ?? err.message));
        return EMPTY;
      }))),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(({ colas, registro }) => {
      this.colas.set(colas);
      this.registro.set(registro);
      this.actualizado.set(new Date());
      this.error.set(null);
    });
  }

  simularFallo(tipo: TipoFallo): void {
    this.mensajeriaService.simularFalloNotificacion(tipo).subscribe({
      next: () => this.avisar(tipo === 'TRANSITORIO'
        ? 'Fallo transitorio publicado: se reintentara una vez y luego ira a la DLQ.'
        : 'Fallo permanente publicado: ira directo a la DLQ.'),
      error: (err) => this.error.set(err.error?.detail ?? err.message),
    });
  }

  publicarInvalido(): void {
    this.mensajeriaService.publicarMensajeInvalido().subscribe({
      next: () => this.avisar('Mensaje invalido publicado: el consumidor lo rechazara hacia la DLQ.'),
      error: (err) => this.error.set(err.error?.detail ?? err.message),
    });
  }

  vaciar(cola: ColaInfo): void {
    this.mensajeriaService.purgarCola(cola.nombre).subscribe({
      next: (r) => this.avisar(`${r.mensajesEliminados} mensaje(s) eliminados de ${this.nombreCorto(cola.nombre)}.`),
      error: (err) => this.error.set(err.error?.detail ?? err.message),
    });
  }

  limpiarRegistro(): void {
    this.mensajeriaService.limpiarRegistro().subscribe(() => this.registro.set([]));
  }

  nombreCorto(nombre: string): string {
    return nombre.replace('pedidos360.', '');
  }

  private esDlq(cola: ColaInfo): boolean {
    return cola.nombre.endsWith('.dlq');
  }

  private avisar(mensaje: string): void {
    this.aviso.set(mensaje);
    setTimeout(() => this.aviso.set(null), 4000);
  }
}
