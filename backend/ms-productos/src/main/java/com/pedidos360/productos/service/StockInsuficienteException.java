package com.pedidos360.productos.service;

/**
 * No hay unidades suficientes (o el producto no existe) para descontar el stock de un pedido.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String message) {
        super(message);
    }
}
