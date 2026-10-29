package com.pedidos360.mensajeria.messaging.consumer.tickets;

import com.pedidos360.mensajeria.messaging.consumer.support.AbstractManualAckConsumer;
import com.pedidos360.mensajeria.messaging.consumer.support.ConsumerSupport;
import com.pedidos360.mensajeria.messaging.model.TicketMessage;
import com.pedidos360.mensajeria.service.tickets.TicketService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TicketConsumer extends AbstractManualAckConsumer<TicketMessage> {

    private final TicketService ticketService;

    public TicketConsumer(ConsumerSupport support, TicketService ticketService) {
        super(support, TicketMessage.class);
        this.ticketService = ticketService;
    }

    @RabbitListener(id = "tickets", queues = "${mensajeria.rabbit.tickets.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        consumir(message, channel);
    }

    @Override
    protected String procesar(TicketMessage payload, MessageProperties properties) {
        return ticketService.generar(payload);
    }
}
