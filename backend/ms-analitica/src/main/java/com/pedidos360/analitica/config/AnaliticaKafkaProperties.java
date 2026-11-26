package com.pedidos360.analitica.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Topicos, consumer groups y politica de reintentos de ms-analitica (prefijo "analitica.kafka").
 * Los brokers se configuran en spring.kafka.bootstrap-servers.
 */
@Validated
@ConfigurationProperties(prefix = "analitica.kafka")
public record AnaliticaKafkaProperties(
        boolean autoStartup,
        @NotBlank String topicPedidos,
        @NotBlank String topicAuditoria,
        @NotBlank String groupReporteria,
        @NotBlank String groupAuditoria,
        @NotBlank String groupDlt,
        @NotBlank String groupAlertas,
        @Min(0) int umbralStockBajo,
        @Min(1) int concurrency,
        @Min(0) int maxReintentos,
        @Min(1) long backoffInicialMs,
        @Min(1) double backoffMultiplicador,
        @Min(1) long backoffMaximoMs
) {

    /** Sufijo de los Dead Letter Topics: "<topico>.DLT". */
    public static final String SUFIJO_DLT = ".DLT";

    public String dltDe(String topico) {
        return topico + SUFIJO_DLT;
    }
}
