package com.pedidos360.kafkaadmin.dto;

import java.util.List;

/**
 * Lag de un consumer group: por cada particion, el offset confirmado, el ultimo offset del topico y
 * la diferencia (mensajes pendientes por procesar). Un lag creciente indica un consumidor lento o caido.
 */
public record ConsumerGroupLagResponse(
        String groupId,
        long lagTotal,
        List<ParticionLag> particiones
) {

    public record ParticionLag(String topico, int particion, long offsetActual, long offsetFinal, long lag) {
    }
}
