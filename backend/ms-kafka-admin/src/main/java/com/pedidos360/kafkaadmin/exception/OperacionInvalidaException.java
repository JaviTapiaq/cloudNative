package com.pedidos360.kafkaadmin.exception;

/** La operacion solicitada no es valida para el estado actual (ej. reducir particiones). */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String message) {
        super(message);
    }
}
