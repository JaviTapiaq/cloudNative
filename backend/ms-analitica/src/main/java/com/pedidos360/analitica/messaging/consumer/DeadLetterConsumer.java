package com.pedidos360.analitica.messaging.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Monitor de los Dead Letter Topics: registra en logs cada mensaje que no pudo procesarse, con el
 * topico de origen, su ubicacion original y la causa del fallo. El mensaje permanece en el DLT
 * (retencion de 30 dias) para poder inspeccionarlo o reprocesarlo desde Kafka-UI.
 */
@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @KafkaListener(id = "dlt-monitor",
            topics = {"${analitica.kafka.topic-pedidos}.DLT", "${analitica.kafka.topic-auditoria}.DLT"},
            groupId = "${analitica.kafka.group-dlt}",
            containerFactory = "dltListenerContainerFactory")
    public void registrar(ConsumerRecord<String, String> registro) {
        log.error("DLT {}: mensaje descartado. key={} origen={}-{}@{} causa=\"{}\" payload={}",
                registro.topic(), registro.key(),
                cabecera(registro, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                cabecera(registro, KafkaHeaders.DLT_ORIGINAL_PARTITION),
                cabecera(registro, KafkaHeaders.DLT_ORIGINAL_OFFSET),
                cabecera(registro, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                registro.value());
    }

    private String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header header = registro.headers().lastHeader(nombre);
        if (header == null || header.value() == null) {
            return "?";
        }
        byte[] valor = header.value();
        // Particion (int) y offset (long) viajan como binario; el resto como texto
        if (KafkaHeaders.DLT_ORIGINAL_PARTITION.equals(nombre) && valor.length == Integer.BYTES) {
            return String.valueOf(java.nio.ByteBuffer.wrap(valor).getInt());
        }
        if (KafkaHeaders.DLT_ORIGINAL_OFFSET.equals(nombre) && valor.length == Long.BYTES) {
            return String.valueOf(java.nio.ByteBuffer.wrap(valor).getLong());
        }
        return new String(valor, StandardCharsets.UTF_8);
    }
}
