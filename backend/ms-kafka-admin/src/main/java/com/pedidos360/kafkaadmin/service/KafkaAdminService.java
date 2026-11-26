package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import com.pedidos360.kafkaadmin.dto.ActualizarTopicoRequest;
import com.pedidos360.kafkaadmin.dto.ClusterResponse;
import com.pedidos360.kafkaadmin.dto.ConsumerGroupLagResponse;
import com.pedidos360.kafkaadmin.dto.ConsumerGroupResponse;
import com.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import com.pedidos360.kafkaadmin.dto.TopicoResponse;
import com.pedidos360.kafkaadmin.exception.OperacionInvalidaException;
import com.pedidos360.kafkaadmin.exception.RecursoProtegidoException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Operaciones de administracion de Kafka sobre el AdminClient: topicos (crear, listar, describir,
 * modificar configuraciones y particiones, eliminar), consumer groups (listar, describir, lag,
 * eliminar) y estado del cluster.
 */
@Service
public class KafkaAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminService.class);

    /** Configuraciones que siempre se muestran, aunque tengan el valor por defecto del cluster. */
    private static final Set<String> CONFIGURACIONES_RELEVANTES =
            Set.of("retention.ms", "cleanup.policy", "min.insync.replicas");

    private final Admin admin;
    private final long timeoutMs;

    public KafkaAdminService(Admin admin, KafkaAdminProperties props) {
        this.admin = admin;
        // Margen sobre el timeout del cliente para que sea Kafka (y no este servicio) quien agote primero
        this.timeoutMs = props.requestTimeoutMs() * 3L;
    }

    // ------------------------------------------------------------------ Topicos

    public List<TopicoResponse> listarTopicos() {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        Map<String, TopicDescription> descripciones = esperar(admin.describeTopics(nombres).allTopicNames());
        Map<String, Config> configs = describirConfiguraciones(nombres);
        return descripciones.values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(d -> aRespuesta(d, configs.get(d.name()), false))
                .toList();
    }

    public TopicoResponse obtenerTopico(String nombre) {
        TopicDescription descripcion = esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
        return aRespuesta(descripcion, describirConfiguraciones(Set.of(nombre)).get(nombre), true);
    }

    public TopicoResponse crearTopico(CrearTopicoRequest request) {
        rechazarInterno(request.nombre());
        NewTopic topico = new NewTopic(request.nombre(), request.particionesOUsual(), request.replicacionOUsual());
        if (request.configuraciones() != null && !request.configuraciones().isEmpty()) {
            topico.configs(request.configuraciones());
        }
        esperar(admin.createTopics(List.of(topico)).all());
        log.info("Topico {} creado (particiones={}, replicas={}, configs={})", request.nombre(),
                request.particionesOUsual(), request.replicacionOUsual(), request.configuraciones());
        return obtenerTopico(request.nombre());
    }

    public TopicoResponse actualizarTopico(String nombre, ActualizarTopicoRequest request) {
        rechazarInterno(nombre);
        TopicoResponse actual = obtenerTopico(nombre);

        boolean hayConfigs = request.configuraciones() != null && !request.configuraciones().isEmpty();
        boolean hayBajas = request.eliminarConfiguraciones() != null && !request.eliminarConfiguraciones().isEmpty();
        boolean hayParticiones = request.particiones() != null;
        if (!hayConfigs && !hayBajas && !hayParticiones) {
            throw new OperacionInvalidaException("Indique al menos configuraciones, eliminarConfiguraciones o particiones");
        }
        if (hayParticiones && request.particiones() < actual.particiones()) {
            throw new OperacionInvalidaException("Kafka no permite reducir particiones (actual="
                    + actual.particiones() + ", solicitado=" + request.particiones() + ")");
        }

        if (hayConfigs || hayBajas) {
            List<AlterConfigOp> operaciones = new ArrayList<>();
            if (hayConfigs) {
                request.configuraciones().forEach((clave, valor) -> operaciones.add(
                        new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET)));
            }
            if (hayBajas) {
                request.eliminarConfiguraciones().forEach(clave -> operaciones.add(
                        new AlterConfigOp(new ConfigEntry(clave, null), AlterConfigOp.OpType.DELETE)));
            }
            esperar(admin.incrementalAlterConfigs(
                    Map.of(new ConfigResource(ConfigResource.Type.TOPIC, nombre), operaciones)).all());
            log.info("Topico {}: configuraciones actualizadas ({} operaciones)", nombre, operaciones.size());
        }
        if (hayParticiones && request.particiones() > actual.particiones()) {
            esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(request.particiones()))).all());
            log.info("Topico {}: particiones aumentadas de {} a {}", nombre, actual.particiones(),
                    request.particiones());
        }
        return obtenerTopico(nombre);
    }

    public void eliminarTopico(String nombre) {
        rechazarInterno(nombre);
        esperar(admin.deleteTopics(List.of(nombre)).all());
        log.info("Topico {} eliminado", nombre);
    }

    // ------------------------------------------------------------------ Consumer groups

    public List<ConsumerGroupResponse> listarConsumerGroups() {
        List<String> ids = esperar(admin.listConsumerGroups().all()).stream()
                .map(g -> g.groupId()).sorted().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return esperar(admin.describeConsumerGroups(ids).all()).values().stream()
                .sorted(Comparator.comparing(ConsumerGroupDescription::groupId))
                .map(this::aRespuesta)
                .toList();
    }

    public ConsumerGroupResponse obtenerConsumerGroup(String groupId) {
        return aRespuesta(describirGrupoExistente(groupId));
    }

    public ConsumerGroupLagResponse lagDeConsumerGroup(String groupId) {
        ConsumerGroupDescription grupo = describirGrupoExistente(groupId);
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());

        // Particiones a medir: las que tienen offset confirmado, las asignadas a miembros activos y TODAS las
        // particiones de esos topicos. Una particion que nunca confirmo un offset tambien acumula lag.
        Set<TopicPartition> particiones = new HashSet<>(confirmados.keySet());
        grupo.members().forEach(m -> particiones.addAll(m.assignment().topicPartitions()));
        Set<String> topicos = new HashSet<>();
        particiones.forEach(tp -> topicos.add(tp.topic()));
        if (topicos.isEmpty()) {
            return new ConsumerGroupLagResponse(groupId, 0, List.of());
        }
        esperar(admin.describeTopics(topicos).allTopicNames()).values().forEach(d ->
                d.partitions().forEach(p -> particiones.add(new TopicPartition(d.name(), p.partition()))));

        Map<TopicPartition, OffsetSpec> ultimos = new HashMap<>();
        Map<TopicPartition, OffsetSpec> primeros = new HashMap<>();
        for (TopicPartition tp : particiones) {
            ultimos.put(tp, OffsetSpec.latest());
            if (confirmados.get(tp) == null) {
                primeros.put(tp, OffsetSpec.earliest());
            }
        }
        Map<TopicPartition, ListOffsetsResultInfo> finales = esperar(admin.listOffsets(ultimos).all());
        // Sin offset confirmado, el consumidor arrancaria desde el primer mensaje disponible (reset=earliest)
        Map<TopicPartition, ListOffsetsResultInfo> iniciales = primeros.isEmpty() ? Map.of()
                : esperar(admin.listOffsets(primeros).all());

        // TreeMap por (topico, particion) para una salida estable
        Map<TopicPartition, ConsumerGroupLagResponse.ParticionLag> filas = new TreeMap<>(
                Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition));
        long total = 0;
        for (TopicPartition tp : particiones) {
            OffsetAndMetadata confirmado = confirmados.get(tp);
            long actual = confirmado != null ? confirmado.offset() : iniciales.get(tp).offset();
            long fin = finales.get(tp).offset();
            long lag = Math.max(0, fin - actual);
            total += lag;
            filas.put(tp, new ConsumerGroupLagResponse.ParticionLag(tp.topic(), tp.partition(), actual, fin, lag));
        }
        return new ConsumerGroupLagResponse(groupId, total, List.copyOf(filas.values()));
    }

    public void eliminarConsumerGroup(String groupId) {
        describirGrupoExistente(groupId);
        // Kafka rechaza con GroupNotEmptyException (409) si el grupo tiene miembros activos
        esperar(admin.deleteConsumerGroups(List.of(groupId)).all());
        log.info("Consumer group {} eliminado", groupId);
    }

    // ------------------------------------------------------------------ Cluster

    public ClusterResponse describirCluster() {
        DescribeClusterResult resultado = admin.describeCluster();
        String clusterId = esperar(resultado.clusterId());
        Node controlador = esperar(resultado.controller());
        Collection<Node> nodos = esperar(resultado.nodes());
        List<ClusterResponse.Broker> brokers = nodos.stream()
                .sorted(Comparator.comparingInt(Node::id))
                .map(n -> new ClusterResponse.Broker(n.id(), n.host(), n.port()))
                .toList();
        return new ClusterResponse(clusterId, controlador == null ? null : controlador.id(), brokers);
    }

    // ------------------------------------------------------------------ Soporte (tambien usado por el bootstrap)

    /** Nombres de los topicos existentes, incluyendo los internos. */
    public Set<String> nombresDeTopicos() {
        return esperar(admin.listTopics(new ListTopicsOptions().listInternal(true)).names());
    }

    private Map<String, Config> describirConfiguraciones(Collection<String> topicos) {
        List<ConfigResource> recursos = topicos.stream()
                .map(t -> new ConfigResource(ConfigResource.Type.TOPIC, t)).toList();
        Map<ConfigResource, Config> configs = esperar(admin.describeConfigs(recursos).all());
        Map<String, Config> porTopico = new HashMap<>();
        configs.forEach((recurso, config) -> porTopico.put(recurso.name(), config));
        return porTopico;
    }

    private ConsumerGroupDescription describirGrupoExistente(String groupId) {
        ConsumerGroupDescription descripcion = esperar(admin.describeConsumerGroups(List.of(groupId)).all())
                .get(groupId);
        // Un grupo inexistente se describe como DEAD en lugar de lanzar error
        if (descripcion == null || descripcion.state() == ConsumerGroupState.DEAD) {
            throw new GroupIdNotFoundException("El consumer group " + groupId + " no existe");
        }
        return descripcion;
    }

    private TopicoResponse aRespuesta(TopicDescription d, Config config, boolean conDetalle) {
        int factor = d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size();
        Map<String, String> relevantes = new TreeMap<>();
        if (config != null) {
            for (ConfigEntry entrada : config.entries()) {
                boolean sobrescrita = entrada.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG;
                if (sobrescrita || CONFIGURACIONES_RELEVANTES.contains(entrada.name())) {
                    relevantes.put(entrada.name(), entrada.value());
                }
            }
        }
        List<TopicoResponse.ParticionInfo> detalle = conDetalle
                ? d.partitions().stream()
                        .map(p -> new TopicoResponse.ParticionInfo(p.partition(),
                                p.leader() == null ? -1 : p.leader().id(),
                                p.replicas().stream().map(Node::id).toList(),
                                p.isr().stream().map(Node::id).toList()))
                        .toList()
                : List.of();
        return new TopicoResponse(d.name(), d.isInternal(), d.partitions().size(), factor, relevantes, detalle);
    }

    private ConsumerGroupResponse aRespuesta(ConsumerGroupDescription d) {
        return new ConsumerGroupResponse(d.groupId(), d.state().toString(), d.members().size(),
                d.partitionAssignor(), d.coordinator() == null ? null : d.coordinator().id());
    }

    private void rechazarInterno(String nombre) {
        if (nombre.startsWith("__")) {
            throw new RecursoProtegidoException("El topico " + nombre + " es interno de Kafka y no se puede modificar");
        }
    }

    /** Espera el resultado de Kafka propagando los errores de Kafka tal cual (los traduce el handler). */
    <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Operacion interrumpida", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw new KafkaException(e.getCause());
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Kafka no respondio en " + timeoutMs + " ms");
        }
    }
}
