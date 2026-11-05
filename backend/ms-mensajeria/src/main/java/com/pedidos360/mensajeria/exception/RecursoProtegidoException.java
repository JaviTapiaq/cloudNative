package com.pedidos360.mensajeria.exception;

/**
 * Se intento eliminar un recurso de la topologia base del sistema.
 */
public class RecursoProtegidoException extends RuntimeException {

    public RecursoProtegidoException(String message) {
        super(message);
    }
}
