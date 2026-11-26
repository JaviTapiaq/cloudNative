package com.pedidos360.kafkaadmin.dto;

/**
 * Resumen de un consumer group: estado (STABLE, EMPTY, PREPARING_REBALANCE...), miembros conectados
 * y broker coordinador.
 */
public record ConsumerGroupResponse(
        String groupId,
        String estado,
        int miembros,
        String asignador,
        Integer coordinador
) {
}
