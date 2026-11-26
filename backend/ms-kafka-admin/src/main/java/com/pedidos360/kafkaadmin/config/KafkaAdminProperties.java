package com.pedidos360.kafkaadmin.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * Configuracion del servicio (prefijo "kafka.admin"): conexion a los brokers y topicos que se
 * garantizan al arrancar.
 */
@Validated
@ConfigurationProperties(prefix = "kafka.admin")
public record KafkaAdminProperties(
        @NotBlank String bootstrapServers,
        @Min(1000) int requestTimeoutMs,
        boolean bootstrapEnabled,
        @Valid List<TopicoInicial> bootstrapTopics
) {

    public KafkaAdminProperties {
        bootstrapTopics = bootstrapTopics == null ? List.of() : bootstrapTopics;
    }

    public record TopicoInicial(
            @NotBlank String nombre,
            @Min(1) int particiones,
            @Min(1) short factorReplicacion,
            Map<String, String> configuraciones
    ) {
        public TopicoInicial {
            configuraciones = configuraciones == null ? Map.of() : configuraciones;
        }
    }
}
