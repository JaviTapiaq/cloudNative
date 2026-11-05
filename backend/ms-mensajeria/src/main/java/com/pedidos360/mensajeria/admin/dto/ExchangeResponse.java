package com.pedidos360.mensajeria.admin.dto;

public record ExchangeResponse(
        String nombre,
        TipoExchange tipo,
        boolean durable,
        boolean autoDelete
) {
}
