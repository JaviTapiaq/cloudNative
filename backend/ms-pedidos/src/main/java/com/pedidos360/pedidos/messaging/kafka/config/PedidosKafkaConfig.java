package com.pedidos360.pedidos.messaging.kafka.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

/**
 * Cliente Kafka (productor) de ms-pedidos. La configuracion vive aqui y en spring.kafka.*
 * de application.yml; el publicador no conoce detalles de infraestructura.
 *
 * <ul>
 *   <li>Key: String (pedidoId) -> los eventos de un pedido van a la misma particion y se leen en orden.</li>
 *   <li>Value: DTO serializado a JSON, sin cabeceras de tipo Java (el contrato es el JSON, no la clase).</li>
 *   <li>acks=all + idempotencia: ningun evento se pierde ni se duplica aunque caiga un broker.</li>
 *   <li>Timeouts cortos: si Kafka esta caido el pedido NO se bloquea (ver PedidoEventKafkaPublisher).</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(PedidosKafkaProperties.class)
@ConditionalOnProperty(prefix = "pedidos.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PedidosKafkaConfig {

    @Bean
    public ProducerFactory<String, Object> pedidosProducerFactory(KafkaProperties kafkaProperties,
                                                                  ObjectMapper objectMapper) {
        // Parte de spring.kafka.* (bootstrap-servers, client-id...) y fija los parametros avanzados
        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        props.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 15000);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 3000);

        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);
        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pedidosProducerFactory) {
        return new KafkaTemplate<>(pedidosProducerFactory);
    }
}
