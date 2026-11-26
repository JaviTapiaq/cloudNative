package com.pedidos360.analitica.messaging.consumer;

import com.pedidos360.analitica.messaging.model.AuditoriaEventoMessage;
import com.pedidos360.analitica.service.AuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor del topico de auditoria (consumer group "auditoria-group"). Es un group distinto al de
 * reporteria: cada flujo avanza con su propio offset y una falla en uno no afecta al otro.
 */
@Component
public class AuditoriaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaConsumer.class);

    private final AuditoriaService auditoriaService;
    private final PayloadValidator validator;

    public AuditoriaConsumer(AuditoriaService auditoriaService, PayloadValidator validator) {
        this.auditoriaService = auditoriaService;
        this.validator = validator;
    }

    @KafkaListener(id = "auditoria-eventos",
            topics = "${analitica.kafka.topic-auditoria}",
            groupId = "${analitica.kafka.group-auditoria}",
            containerFactory = "kafkaListenerContainerFactory")
    public void recibir(@Payload AuditoriaEventoMessage evento,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int particion,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        validator.validar(evento, "Registro de auditoria");
        boolean registrado = auditoriaService.registrar(evento);
        log.info("Auditoria: {} sobre {} {} {} (particion={}, offset={})", evento.accion(), evento.entidad(),
                evento.entidadId(), registrado ? "registrada" : "duplicada, se omite", particion, offset);
    }
}
