package com.pedidos360.pedidos.messaging;

import com.pedidos360.pedidos.event.PedidoEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato JSON del evento publicado en RabbitMQ. Cada consumidor (ms-mensajeria,
 * ms-productos, ms-clientes) tiene su propia copia con solo los campos que usa:
 * los servicios no comparten clases, solo el formato del mensaje.
 */
public record PedidoEventoMessage(
        String tipoEvento,
        Long pedidoId,
        Long clienteId,
        String clienteEmail,
        String clienteNombre,
        String estado,
        BigDecimal total,
        Integer cantidadItems,
        List<Item> items,
        LocalDateTime fecha
) {

    public record Item(Long productoId, String nombreProducto, Integer cantidad, BigDecimal precioUnitario) {
    }

    public static PedidoEventoMessage desde(PedidoEvent evento) {
        List<Item> items = evento.items().stream()
                .map(i -> new Item(i.productoId(), i.nombreProducto(), i.cantidad(), i.precioUnitario()))
                .toList();
        return new PedidoEventoMessage(evento.tipo().name(), evento.pedidoId(), evento.clienteId(),
                evento.clienteEmail(), evento.clienteNombre(), evento.estado(), evento.total(),
                evento.cantidadItems(), items, evento.fecha());
    }
}
