package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.Map;

/**
 * Modificacion de un topico existente. Todos los campos son opcionales:
 * <ul>
 *   <li>configuraciones: se establecen o actualizan (ej. retention.ms).</li>
 *   <li>eliminarConfiguraciones: vuelven al valor por defecto del cluster.</li>
 *   <li>particiones: solo puede aumentarse (Kafka no permite reducir particiones).</li>
 * </ul>
 */
public record ActualizarTopicoRequest(
        Map<String, String> configuraciones,
        List<String> eliminarConfiguraciones,
        @Min(1) @Max(100) Integer particiones
) {
}
