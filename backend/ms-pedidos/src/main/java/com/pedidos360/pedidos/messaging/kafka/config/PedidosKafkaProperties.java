package com.pedidos360.pedidos.messaging.kafka.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Parametros Kafka propios de ms-pedidos (prefijo "pedidos.kafka").
 * Los brokers se configuran en spring.kafka.bootstrap-servers.
 */
@Validated
@ConfigurationProperties(prefix = "pedidos.kafka")
public record PedidosKafkaProperties(
        boolean enabled,
        @NotBlank String topicPedidos,
        @NotBlank String topicAuditoria,
        @NotBlank String origen
) {
}
