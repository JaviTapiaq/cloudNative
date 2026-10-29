package com.pedidos360.mensajeria.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infraestructura de mensajeria: conversion JSON, RabbitTemplate con publisher confirms
 * y fabrica de listeners con ACK manual. No contiene logica de negocio.
 */
@Configuration
public class RabbitInfrastructureConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitInfrastructureConfig.class);

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        // mandatory=true: si no hay cola enlazada para la routing key, el broker devuelve el mensaje
        template.setMandatory(true);
        template.setConfirmCallback((correlation, ack, cause) -> {
            String id = correlation != null ? correlation.getId() : "n/a";
            if (ack) {
                log.debug("Broker confirmo (ACK) la publicacion del mensaje {}", id);
            } else {
                log.error("Broker rechazo (NACK) la publicacion del mensaje {}: {}", id, cause);
            }
        });
        template.setReturnsCallback(returned -> log.error(
                "Mensaje sin ruta: exchange={} routingKey={} replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return template;
    }

    /**
     * RabbitAdmin declara en el broker los beans Queue/Exchange/Binding de RabbitTopologyConfig
     * y es la base del RabbitAdminService para la gestion dinamica via REST.
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * Fabrica usada por todos los @RabbitListener. Se fuerza AcknowledgeMode.MANUAL para que
     * cada consumidor decida explicitamente basicAck / basicNack / basicReject.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        // Los consumidores reciben el Message crudo y deserializan/validan ellos mismos
        // (AbstractManualAckConsumer). Asi un JSON invalido o un __TypeId__ desconocido
        // llega al consumidor y se rechaza explicitamente hacia la DLQ, en vez de fallar
        // dentro del contenedor antes de poder hacer ACK/NACK.
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
