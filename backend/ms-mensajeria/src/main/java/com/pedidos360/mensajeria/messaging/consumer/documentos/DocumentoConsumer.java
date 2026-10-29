package com.pedidos360.mensajeria.messaging.consumer.documentos;

import com.pedidos360.mensajeria.messaging.consumer.support.AbstractManualAckConsumer;
import com.pedidos360.mensajeria.messaging.consumer.support.ConsumerSupport;
import com.pedidos360.mensajeria.messaging.model.DocumentoMessage;
import com.pedidos360.mensajeria.service.documentos.DocumentoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class DocumentoConsumer extends AbstractManualAckConsumer<DocumentoMessage> {

    private final DocumentoService documentoService;

    public DocumentoConsumer(ConsumerSupport support, DocumentoService documentoService) {
        super(support, DocumentoMessage.class);
        this.documentoService = documentoService;
    }

    @RabbitListener(id = "documentos", queues = "${mensajeria.rabbit.documentos.queue}")
    public void recibir(Message message, Channel channel) throws IOException {
        consumir(message, channel);
    }

    @Override
    protected String procesar(DocumentoMessage payload, MessageProperties properties) {
        return documentoService.generar(payload);
    }
}
