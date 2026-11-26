package com.pedidos360.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

/**
 * Descripcion de un topico. "configuraciones" incluye las sobrescrituras del topico y las claves
 * relevantes (retention.ms, cleanup.policy, min.insync.replicas). "detalleParticiones" solo se
 * completa al consultar un topico individual.
 */
public record TopicoResponse(
        String nombre,
        boolean interno,
        int particiones,
        int factorReplicacion,
        Map<String, String> configuraciones,
        List<ParticionInfo> detalleParticiones
) {

    public record ParticionInfo(int particion, int lider, List<Integer> replicas, List<Integer> isr) {
    }
}
