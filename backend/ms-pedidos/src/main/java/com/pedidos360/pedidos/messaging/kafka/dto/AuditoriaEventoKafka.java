package com.pedidos360.pedidos.messaging.kafka.dto;

import com.pedidos360.pedidos.event.PedidoEvent;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de auditoria publicado en el topico de auditoria: quien hizo que, sobre que entidad y cuando.
 *
 * <ul>
 *   <li>accion: PEDIDO_CREADO | PEDIDO_ESTADO_ACTUALIZADO.</li>
 *   <li>entidad / entidadId: sobre que recurso se actuo (PEDIDO, id).</li>
 *   <li>actor: usuario asociado a la accion (correo del cliente del pedido).</li>
 *   <li>detalle: descripcion legible del cambio.</li>
 *   <li>origen: microservicio que genero el registro.</li>
 * </ul>
 */
public record AuditoriaEventoKafka(
        String eventId,
        String accion,
        String entidad,
        String entidadId,
        String actor,
        String detalle,
        String origen,
        LocalDateTime ocurridoEn
) {

    public static AuditoriaEventoKafka desde(PedidoEvent evento, String origen) {
        boolean creado = evento.tipo() == PedidoEvent.Tipo.CREADO;
        String accion = creado ? "PEDIDO_CREADO" : "PEDIDO_ESTADO_ACTUALIZADO";
        String detalle = creado
                ? "Pedido creado con " + evento.cantidadItems() + " unidades por un total de " + evento.total()
                : "Estado del pedido actualizado a " + evento.estado();
        String actor = evento.clienteEmail() != null ? evento.clienteEmail() : "cliente-" + evento.clienteId();
        return new AuditoriaEventoKafka(UUID.randomUUID().toString(), accion, "PEDIDO",
                String.valueOf(evento.pedidoId()), actor, detalle, origen, evento.fecha());
    }
}
