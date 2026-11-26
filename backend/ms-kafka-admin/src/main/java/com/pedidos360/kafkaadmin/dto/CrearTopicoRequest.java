package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * Solicitud para crear un topico. Si no se indican particiones ni factor de replicacion se usan
 * los del cluster (3 y 3). "configuraciones" admite cualquier configuracion de topico de Kafka
 * (retention.ms, cleanup.policy, min.insync.replicas, segment.bytes...); el broker valida los valores.
 */
public record CrearTopicoRequest(
        @NotBlank
        @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}", message = "solo letras, numeros, '.', '_' y '-' (maximo 249)")
        String nombre,
        @Min(1) @Max(100) Integer particiones,
        @Min(1) @Max(10) Integer factorReplicacion,
        Map<String, String> configuraciones
) {
    public static final int PARTICIONES_POR_DEFECTO = 3;
    public static final int REPLICACION_POR_DEFECTO = 3;

    public int particionesOUsual() {
        return particiones != null ? particiones : PARTICIONES_POR_DEFECTO;
    }

    public short replicacionOUsual() {
        return (short) (factorReplicacion != null ? factorReplicacion : REPLICACION_POR_DEFECTO);
    }
}
