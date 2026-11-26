package com.pedidos360.analitica.messaging.consumer;

import com.pedidos360.analitica.messaging.model.PedidoEventoMessage;
import com.pedidos360.analitica.service.AlertaStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Tercer caso de uso: alertas de stock bajo (consumer group "alertas-group").
 *
 * Lee el MISMO topico de pedidos que reporteria-group, pero como grupo distinto: recibe su propia copia de
 * cada evento y lleva su propio offset. Se agrego sin tocar ni a ms-pedidos ni al consumidor de reporteria,
 * que es justamente la ventaja de Kafka frente a una cola (donde el mensaje lo consume un solo lector).
 */
@Component
public class AlertasStockConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertasStockConsumer.class);

    private final AlertaStockService alertaService;
    private final PayloadValidator validator;

    public AlertasStockConsumer(AlertaStockService alertaService, PayloadValidator validator) {
        this.alertaService = alertaService;
        this.validator = validator;
    }

    @KafkaListener(id = "alertas-stock",
            topics = "${analitica.kafka.topic-pedidos}",
            groupId = "${analitica.kafka.group-alertas}",
            containerFactory = "kafkaListenerContainerFactory")
    public void recibir(@Payload PedidoEventoMessage evento,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        validator.validar(evento, "Evento de pedido");
        int alertas = alertaService.evaluar(evento);
        log.debug("Alertas: {} del pedido {} -> {} alertas nuevas (particion={}, offset={})", evento.tipoEvento(),
                evento.pedidoId(), alertas, particion, offset);
    }
}
