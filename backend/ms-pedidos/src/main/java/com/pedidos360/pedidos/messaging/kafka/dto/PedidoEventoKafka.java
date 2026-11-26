package com.pedidos360.pedidos.messaging.kafka.dto;

import com.pedidos360.pedidos.event.PedidoEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Evento de negocio publicado en el topico de pedidos (alimenta la reporteria).
 *
 * <ul>
 *   <li>eventId: identificador unico; permite al consumidor ser idempotente ante reentregas.</li>
 *   <li>tipoEvento: CREADO | ESTADO_ACTUALIZADO.</li>
 *   <li>version: version del contrato, para evolucionar el formato sin romper consumidores.</li>
 *   <li>pedidoId, clienteEmail, estado, total, cantidadItems, items: datos minimos para calcular
 *       ventas, pedidos por estado y productos mas vendidos sin consultar a ms-pedidos.</li>
 *   <li>fechaEvento: instante en que ocurrio el hecho (no cuando se proceso).</li>
 * </ul>
 */
public record PedidoEventoKafka(
        String eventId,
        String tipoEvento,
        int version,
        Long pedidoId,
        Long clienteId,
        String clienteEmail,
        String clienteNombre,
        String estado,
        BigDecimal total,
        Integer cantidadItems,
        List<Item> items,
        LocalDateTime fechaEvento
) {

    public static final int VERSION_ACTUAL = 1;

    /** stockRestante: stock que queda del producto tras el pedido; lo usa el grupo de alertas de stock bajo. */
    public record Item(Long productoId, String nombreProducto, Integer cantidad, BigDecimal precioUnitario,
                       Integer stockRestante) {
    }

    public static PedidoEventoKafka desde(PedidoEvent evento) {
        List<Item> items = evento.items().stream()
                .map(i -> new Item(i.productoId(), i.nombreProducto(), i.cantidad(), i.precioUnitario(),
                        i.stockRestante()))
                .toList();
        return new PedidoEventoKafka(UUID.randomUUID().toString(), evento.tipo().name(), VERSION_ACTUAL,
                evento.pedidoId(), evento.clienteId(), evento.clienteEmail(), evento.clienteNombre(),
                evento.estado(), evento.total(), evento.cantidadItems(), items, evento.fecha());
    }
}
