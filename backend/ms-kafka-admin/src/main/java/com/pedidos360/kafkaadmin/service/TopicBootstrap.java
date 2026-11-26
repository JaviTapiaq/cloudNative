package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import com.pedidos360.kafkaadmin.config.KafkaAdminProperties.TopicoInicial;
import com.pedidos360.kafkaadmin.dto.ActualizarTopicoRequest;
import com.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import com.pedidos360.kafkaadmin.dto.TopicoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Garantiza al arrancar los topicos declarados en kafka.admin.bootstrap-topics: crea los que faltan y
 * alinea las configuraciones de los existentes (infraestructura declarativa, replicable e idempotente).
 *
 * Corre en segundo plano con reintentos: si Kafka aun no esta disponible el servicio arranca igual y
 * el bootstrap se completa cuando los brokers responden. Nunca elimina topicos ni reduce particiones.
 */
@Component
@ConditionalOnProperty(prefix = "kafka.admin", name = "bootstrap-enabled", havingValue = "true")
public class TopicBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TopicBootstrap.class);
    private static final int INTENTOS = 30;
    private static final long ESPERA_MS = 5000;

    private final KafkaAdminService service;
    private final KafkaAdminProperties props;

    public TopicBootstrap(KafkaAdminService service, KafkaAdminProperties props) {
        this.service = service;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.bootstrapTopics().isEmpty()) {
            return;
        }
        Thread hilo = new Thread(this::ejecutarConReintentos, "kafka-topic-bootstrap");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void ejecutarConReintentos() {
        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                asegurarTopicos();
                log.info("Bootstrap de topicos Kafka completado ({} topicos)", props.bootstrapTopics().size());
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Bootstrap de topicos Kafka: intento {}/{} fallido: {}", intento, INTENTOS, e.getMessage());
                try {
                    Thread.sleep(ESPERA_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.error("Bootstrap de topicos Kafka: se agotaron los reintentos. Cree los topicos con POST /api/kafka-admin/topicos");
    }

    /** Visible para pruebas. */
    void asegurarTopicos() throws InterruptedException {
        Set<String> existentes = service.nombresDeTopicos();
        for (TopicoInicial t : props.bootstrapTopics()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            if (!existentes.contains(t.nombre())) {
                service.crearTopico(new CrearTopicoRequest(t.nombre(), t.particiones(),
                        (int) t.factorReplicacion(), t.configuraciones()));
            } else {
                alinear(t);
            }
        }
    }

    private void alinear(TopicoInicial t) {
        TopicoResponse actual = service.obtenerTopico(t.nombre());
        Map<String, String> diferencias = new HashMap<>();
        t.configuraciones().forEach((clave, valor) -> {
            if (!valor.equals(actual.configuraciones().get(clave))) {
                diferencias.put(clave, valor);
            }
        });
        Integer particiones = t.particiones() > actual.particiones() ? t.particiones() : null;
        if (!diferencias.isEmpty() || particiones != null) {
            log.info("Topico {}: alineando configuracion {} particiones={}", t.nombre(), diferencias, particiones);
            service.actualizarTopico(t.nombre(), new ActualizarTopicoRequest(diferencias, null, particiones));
        }
    }
}
