package com.pedidos360.mensajeria.admin.dto;

public record ColaResponse(
        String nombre,
        long mensajes,
        int consumidores,
        boolean protegida
) {
}
