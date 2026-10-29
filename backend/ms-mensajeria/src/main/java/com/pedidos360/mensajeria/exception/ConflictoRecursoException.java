package com.pedidos360.mensajeria.exception;

/**
 * El recurso ya existe o su estado actual impide la operacion (por ejemplo, cola con mensajes).
 */
public class ConflictoRecursoException extends RuntimeException {

    public ConflictoRecursoException(String message) {
        super(message);
    }
}
