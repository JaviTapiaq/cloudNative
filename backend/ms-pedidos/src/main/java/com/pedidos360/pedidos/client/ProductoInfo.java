package com.pedidos360.pedidos.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Datos de un producto tal como los entrega ms-productos.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductoInfo(Long id, String nombre, BigDecimal precio, Integer stock) {
}
