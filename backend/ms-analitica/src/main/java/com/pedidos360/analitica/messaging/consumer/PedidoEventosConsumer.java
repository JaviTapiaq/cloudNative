package com.pedidos360.analitica.messaging.consumer;

import com.pedidos360.analitica.messaging.model.PedidoEventoMessage;
import com.pedidos360.analitica.service.ReporteriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topico de pedidos para la reporteria (consumer group "reporteria-group").
 *
 * Solo valida y delega: el calculo de metricas vive en ReporteriaService. Si algo falla se lanza la
 * excepcion y el DefaultErrorHandler (KafkaConsumerConfig) reintenta o envia el mensaje al DLT.
 */
@Component
public class PedidoEventosConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventosConsumer.class);

    private final ReporteriaService reporteriaService;
    private final PayloadValidator validator;

    public PedidoEventosConsumer(ReporteriaService reporteriaService, PayloadValidator validator) {
        this.reporteriaService = reporteriaService;
        this.validator = validator;
    }

    @KafkaListener(id = "reporteria-pedidos",
            topics = "${analitica.kafka.topic-pedidos}",
            groupId = "${analitica.kafka.group-reporteria}",
            containerFactory = "kafkaListenerContainerFactory")
    public void recibir(@Payload PedidoEventoMessage evento,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        validator.validar(evento, "Evento de pedido");
        boolean procesado = reporteriaService.procesar(evento);
        log.info("Reporteria: {} del pedido {} {} (particion={}, offset={})", evento.tipoEvento(),
                evento.pedidoId(), procesado ? "procesado" : "ya procesado, se omite", particion, offset);
    }
}
