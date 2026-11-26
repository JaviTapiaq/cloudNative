package com.pedidos360.kafkaadmin.exception;

/** Recurso interno de Kafka (ej. __consumer_offsets) que no debe modificarse desde esta API. */
public class RecursoProtegidoException extends RuntimeException {

    public RecursoProtegidoException(String message) {
        super(message);
    }
}
