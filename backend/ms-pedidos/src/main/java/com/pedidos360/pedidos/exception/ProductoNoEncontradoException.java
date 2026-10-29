package com.pedidos360.pedidos.exception;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(Long productoId) {
        super("El producto " + productoId + " no existe");
    }
}
