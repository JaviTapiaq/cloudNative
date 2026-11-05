package com.pedidos360.pedidos.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de mensajeria de ms-pedidos, separada de la logica de negocio.
 * Solo declara el exchange donde publica (idempotente); las colas y DLQs
 * pertenecen al consumidor (ms-mensajeria).
 */
@Configuration
@EnableConfigurationProperties(PedidosMensajeriaProperties.class)
public class PedidosRabbitConfig {

    @Bean
    public TopicExchange pedidosEventosExchange(PedidosMensajeriaProperties props) {
        return new TopicExchange(props.exchange(), true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        // Cada mensaje lleva un messageId unico para trazarlo en ms-mensajeria
        converter.setCreateMessageIds(true);
        return converter;
    }
}
