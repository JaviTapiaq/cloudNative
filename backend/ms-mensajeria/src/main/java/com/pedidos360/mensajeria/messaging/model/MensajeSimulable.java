package com.pedidos360.mensajeria.messaging.model;

/**
 * Mensajes que permiten forzar un fallo para demostrar reintentos y DLQ.
 * Valores: PERMANENTE (va directo a la DLQ) o TRANSITORIO (se reencola una vez y luego va a la DLQ).
 */
public interface MensajeSimulable {

    String simularFallo();
}
