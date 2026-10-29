package com.pedidos360.pedidos.event;

import com.pedidos360.pedidos.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de dominio interno (Spring ApplicationEvent). No depende de RabbitMQ:
 * el servicio de pedidos solo avisa que algo ocurrio; quien lo publique hacia
 * el broker es una decision de infraestructura (ver messaging.PedidoEventRabbitPublisher).
 */
public record PedidoEvent(
        Tipo tipo,
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

    public enum Tipo {
        CREADO,
        ESTADO_ACTUALIZADO
    }

    public record Item(Long productoId, String nombreProducto, Integer cantidad, BigDecimal precioUnitario) {
    }

    public static PedidoEvent creado(Pedido pedido) {
        List<Item> items = pedido.getDetalles().stream()
                .map(d -> new Item(d.getProductoId(), d.getNombreProducto(), d.getCantidad(), d.getPrecioUnitario()))
                .toList();
        int unidades = items.stream().mapToInt(Item::cantidad).sum();
        return new PedidoEvent(Tipo.CREADO, pedido.getId(), pedido.getClienteId(), pedido.getClienteEmail(),
                pedido.getClienteNombre(), pedido.getEstado(), pedido.getTotal(), unidades, items,
                pedido.getFechaCreacion());
    }

    public static PedidoEvent estadoActualizado(Pedido pedido) {
        return new PedidoEvent(Tipo.ESTADO_ACTUALIZADO, pedido.getId(), pedido.getClienteId(), pedido.getClienteEmail(),
                pedido.getClienteNombre(), pedido.getEstado(), pedido.getTotal(), null, List.of(), LocalDateTime.now());
    }
}
