import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ColaInfo {
  nombre: string;
  mensajes: number;
  consumidores: number;
  protegida: boolean;
}

export interface RegistroProcesamiento {
  fecha: string;
  messageId: string;
  cola: string;
  estado: 'PROCESADO' | 'REENCOLADO' | 'ENVIADO_A_DLQ' | 'RECIBIDO_EN_DLQ';
  detalle: string;
}

export type TipoFallo = 'TRANSITORIO' | 'PERMANENTE';

/**
 * Acceso a ms-mensajeria a traves del BFF: estado de las colas, bitacora de
 * procesamiento y publicacion de mensajes de prueba para demostrar reintentos y DLQ.
 */
@Injectable({
  providedIn: 'root',
})
export class MensajeriaService {
  private http = inject(HttpClient);
  private adminUrl = `${environment.apiUrl}/admin`;
  private mensajeriaUrl = `${environment.apiUrl}/mensajeria`;

  getColas(): Observable<ColaInfo[]> {
    return this.http.get<ColaInfo[]>(`${this.adminUrl}/queues`);
  }

  purgarCola(nombre: string): Observable<{ cola: string; mensajesEliminados: number }> {
    return this.http.delete<{ cola: string; mensajesEliminados: number }>(
      `${this.adminUrl}/queues/${encodeURIComponent(nombre)}/messages`);
  }

  getRegistro(): Observable<RegistroProcesamiento[]> {
    return this.http.get<RegistroProcesamiento[]>(`${this.mensajeriaUrl}/registro`);
  }

  limpiarRegistro(): Observable<void> {
    return this.http.delete<void>(`${this.mensajeriaUrl}/registro`);
  }

  /** Publica una notificacion que el consumidor hara fallar a proposito. */
  simularFalloNotificacion(tipo: TipoFallo): Observable<{ messageId: string }> {
    return this.http.post<{ messageId: string }>(`${this.mensajeriaUrl}/notificaciones`, {
      destinatario: 'demo@pedidos360.cl',
      canal: 'EMAIL',
      asunto: `Prueba de fallo ${tipo.toLowerCase()}`,
      cuerpo: 'Mensaje de demostracion del monitor',
      simularFallo: tipo,
    });
  }

  /** Publica una notificacion con datos invalidos: el consumidor la rechaza hacia la DLQ. */
  publicarMensajeInvalido(): Observable<{ messageId: string }> {
    return this.http.post<{ messageId: string }>(`${this.mensajeriaUrl}/notificaciones`, {
      destinatario: '',
      canal: 'FAX',
      asunto: 'Mensaje invalido',
      cuerpo: 'Sin destinatario y con un canal inexistente',
    });
  }
}
