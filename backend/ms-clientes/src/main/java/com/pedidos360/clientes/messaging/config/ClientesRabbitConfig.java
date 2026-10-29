package com.pedidos360.clientes.messaging.config;

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
 * Topologia RabbitMQ de ms-clientes (separada de la logica de negocio).
 *
 * <pre>
 *  [pedidos360.eventos.exchange] --pedido.creado--> pedidos360.clientes.pedidos.queue
 *  rechazo sin requeue --> [pedidos360.dlx] --pedidos360.clientes.pedidos.dlq--> pedidos360.clientes.pedidos.dlq
 * </pre>
 */
@Configuration
@EnableConfigurationProperties(ClientesMensajeriaProperties.class)
public class ClientesRabbitConfig {

    private final ClientesMensajeriaProperties props;

    public ClientesRabbitConfig(ClientesMensajeriaProperties props) {
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
    public Queue comprasQueue() {
        return QueueBuilder.durable(props.compras().queue())
                .deadLetterExchange(props.deadLetterExchange())
                .deadLetterRoutingKey(props.compras().dlq())
                .build();
    }

    @Bean
    public Queue comprasDlq() {
        return QueueBuilder.durable(props.compras().dlq()).build();
    }

    @Bean
    public Binding comprasBinding() {
        return BindingBuilder.bind(comprasQueue()).to(eventosExchange()).with(props.compras().routingKey());
    }

    @Bean
    public Binding comprasDlqBinding() {
        return BindingBuilder.bind(comprasDlq()).to(deadLetterExchange()).with(props.compras().dlq());
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
