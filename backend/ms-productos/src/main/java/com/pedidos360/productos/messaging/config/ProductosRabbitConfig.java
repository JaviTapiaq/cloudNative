package com.pedidos360.productos.messaging.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topologia RabbitMQ de ms-productos (separada de la logica de negocio).
 *
 * <pre>
 *  [pedidos360.eventos.exchange] --pedido.creado--> pedidos360.productos.stock.queue
 *  rechazo sin requeue --> [pedidos360.dlx] --pedidos360.productos.stock.dlq--> pedidos360.productos.stock.dlq
 * </pre>
 *
 * Los exchanges se declaran tambien aqui (operacion idempotente) para que el servicio
 * pueda arrancar aunque los demas microservicios aun no lo hayan hecho.
 */
@Configuration
@EnableConfigurationProperties(ProductosMensajeriaProperties.class)
public class ProductosRabbitConfig {

    private final ProductosMensajeriaProperties props;

    public ProductosRabbitConfig(ProductosMensajeriaProperties props) {
        this.props = props;
    }

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(props.eventosExchange(), true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(props.deadLetterExchange(), true, false);
    }

    @Bean
    public Queue stockQueue() {
        return QueueBuilder.durable(props.stock().queue())
                .deadLetterExchange(props.deadLetterExchange())
                .deadLetterRoutingKey(props.stock().dlq())
                .build();
    }

    @Bean
    public Queue stockDlq() {
        return QueueBuilder.durable(props.stock().dlq()).build();
    }

    @Bean
    public Binding stockBinding() {
        return BindingBuilder.bind(stockQueue()).to(eventosExchange()).with(props.stock().routingKey());
    }

    @Bean
    public Binding stockDlqBinding() {
        return BindingBuilder.bind(stockDlq()).to(deadLetterExchange()).with(props.stock().dlq());
    }

    /**
     * ACK manual y Message crudo: el consumidor deserializa, valida y decide ACK/NACK.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
