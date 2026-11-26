package com.pedidos360.pedidos.messaging.kafka;

import com.pedidos360.pedidos.event.PedidoEvent;
import com.pedidos360.pedidos.messaging.kafka.config.PedidosKafkaProperties;
import com.pedidos360.pedidos.messaging.kafka.dto.AuditoriaEventoKafka;
import com.pedidos360.pedidos.messaging.kafka.dto.PedidoEventoKafka;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Traduce los eventos de dominio de pedidos a eventos Kafka. Convive con
 * PedidoEventRabbitPublisher: ambos escuchan el mismo PedidoEvent y son independientes
 * (RabbitMQ sigue encargandose de las tareas; Kafka del streaming para reporteria y auditoria).
 *
 * Un fallo de Kafka se registra pero NO se propaga: el pedido nunca falla por el streaming.
 * Por cada hecho se publican dos eventos: uno de negocio y uno de auditoria, ambos con key = pedidoId.
 */
@Component
@ConditionalOnProperty(prefix = "pedidos.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PedidoEventKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PedidosKafkaProperties props;

    public PedidoEventKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, PedidosKafkaProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    @EventListener
    public void onPedidoEvent(PedidoEvent evento) {
        String key = String.valueOf(evento.pedidoId());
        publicar(props.topicPedidos(), key, PedidoEventoKafka.desde(evento), evento);
        publicar(props.topicAuditoria(), key, AuditoriaEventoKafka.desde(evento, props.origen()), evento);
    }

    private void publicar(String topico, String key, Object mensaje, PedidoEvent evento) {
        try {
            kafkaTemplate.send(topico, key, mensaje).whenComplete((resultado, error) -> {
                if (error != null) {
                    log.error("Kafka: no se pudo publicar {} del pedido {} en {}: {}", evento.tipo(),
                            evento.pedidoId(), topico, error.getMessage());
                } else {
                    log.info("Kafka: {} del pedido {} publicado en {} (particion={}, offset={})", evento.tipo(),
                            evento.pedidoId(), topico, resultado.getRecordMetadata().partition(),
                            resultado.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            // send() puede bloquear hasta max.block.ms y lanzar si no obtiene metadata del broker
            log.error("Kafka: error al publicar {} del pedido {} en {}: {}. El pedido se proceso igualmente.",
                    evento.tipo(), evento.pedidoId(), topico, e.getMessage());
        }
    }
}
