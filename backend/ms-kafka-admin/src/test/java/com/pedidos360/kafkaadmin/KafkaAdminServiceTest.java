package com.pedidos360.kafkaadmin;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import com.pedidos360.kafkaadmin.dto.ActualizarTopicoRequest;
import com.pedidos360.kafkaadmin.dto.ConsumerGroupLagResponse;
import com.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import com.pedidos360.kafkaadmin.dto.TopicoResponse;
import com.pedidos360.kafkaadmin.exception.OperacionInvalidaException;
import com.pedidos360.kafkaadmin.exception.RecursoProtegidoException;
import com.pedidos360.kafkaadmin.service.KafkaAdminService;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
class KafkaAdminServiceTest {

    private Admin admin;
    private KafkaAdminService service;

    @BeforeEach
    void setUp() {
        admin = mock(Admin.class);
        service = new KafkaAdminService(admin, new KafkaAdminProperties("localhost:9092", 1000, false, List.of()));
    }

    /** Topico de 3 particiones y 3 replicas. */
    private void topicoExistente(String nombre, int particiones) {
        List<Node> nodos = List.of(new Node(1, "kafka1", 29092), new Node(2, "kafka2", 29092),
                new Node(3, "kafka3", 29092));
        List<TopicPartitionInfo> infos = new java.util.ArrayList<>();
        for (int p = 0; p < particiones; p++) {
            infos.add(new TopicPartitionInfo(p, nodos.get(0), nodos, nodos));
        }
        DescribeTopicsResult describe = mock(DescribeTopicsResult.class);
        when(describe.allTopicNames()).thenReturn(KafkaFuture.completedFuture(
                Map.of(nombre, new TopicDescription(nombre, false, infos))));
        when(admin.describeTopics((Collection<String>) any())).thenReturn(describe);

        DescribeConfigsResult configs = mock(DescribeConfigsResult.class);
        Config config = new Config(List.of(
                new ConfigEntry("retention.ms", "604800000", ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG,
                        false, false, List.of(), ConfigEntry.ConfigType.LONG, null),
                new ConfigEntry("segment.bytes", "1073741824", ConfigEntry.ConfigSource.DEFAULT_CONFIG,
                        false, false, List.of(), ConfigEntry.ConfigType.LONG, null)));
        when(configs.all()).thenReturn(KafkaFuture.completedFuture(
                Map.of(new ConfigResource(ConfigResource.Type.TOPIC, nombre), config)));
        when(admin.describeConfigs(any())).thenReturn(configs);
    }

    @Test
    void creaTopicoConParticionesReplicasYConfiguraciones() {
        CreateTopicsResult creacion = mock(CreateTopicsResult.class);
        when(creacion.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(any())).thenReturn(creacion);
        topicoExistente("demo", 3);

        TopicoResponse respuesta = service.crearTopico(
                new CrearTopicoRequest("demo", 3, 3, Map.of("retention.ms", "604800000")));

        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(captor.capture());
        NewTopic enviado = captor.getValue().iterator().next();
        assertThat(enviado.name()).isEqualTo("demo");
        assertThat(enviado.numPartitions()).isEqualTo(3);
        assertThat(enviado.replicationFactor()).isEqualTo((short) 3);
        assertThat(enviado.configs()).containsEntry("retention.ms", "604800000");
        assertThat(respuesta.particiones()).isEqualTo(3);
        assertThat(respuesta.factorReplicacion()).isEqualTo(3);
    }

    @Test
    void usaTresParticionesYTresReplicasPorDefecto() {
        CreateTopicsResult creacion = mock(CreateTopicsResult.class);
        when(creacion.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(any())).thenReturn(creacion);
        topicoExistente("demo", 3);

        service.crearTopico(new CrearTopicoRequest("demo", null, null, null));

        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(captor.capture());
        assertThat(captor.getValue().iterator().next().numPartitions()).isEqualTo(3);
        assertThat(captor.getValue().iterator().next().replicationFactor()).isEqualTo((short) 3);
    }

    @Test
    void laDescripcionMuestraSoloConfiguracionesRelevantesOSobrescritas() {
        topicoExistente("demo", 3);

        TopicoResponse t = service.obtenerTopico("demo");

        assertThat(t.configuraciones()).containsEntry("retention.ms", "604800000")
                .doesNotContainKey("segment.bytes");
        assertThat(t.detalleParticiones()).hasSize(3);
        assertThat(t.detalleParticiones().get(0).isr()).containsExactly(1, 2, 3);
    }

    @Test
    void actualizaConfiguracionesConIncrementalAlter() {
        topicoExistente("demo", 3);
        AlterConfigsResult alter = mock(AlterConfigsResult.class);
        when(alter.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.incrementalAlterConfigs(any())).thenReturn(alter);

        service.actualizarTopico("demo", new ActualizarTopicoRequest(Map.of("retention.ms", "86400000"),
                List.of("cleanup.policy"), null));

        ArgumentCaptor<Map<ConfigResource, Collection<AlterConfigOp>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(admin).incrementalAlterConfigs(captor.capture());
        Collection<AlterConfigOp> ops = captor.getValue().values().iterator().next();
        assertThat(ops).extracting(AlterConfigOp::opType)
                .containsExactlyInAnyOrder(AlterConfigOp.OpType.SET, AlterConfigOp.OpType.DELETE);
        verify(admin, never()).createPartitions(any());
    }

    @Test
    void aumentaParticiones() {
        topicoExistente("demo", 3);
        CreatePartitionsResult resultado = mock(CreatePartitionsResult.class);
        when(resultado.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createPartitions(any())).thenReturn(resultado);

        service.actualizarTopico("demo", new ActualizarTopicoRequest(null, null, 6));

        ArgumentCaptor<Map<String, NewPartitions>> captor = ArgumentCaptor.forClass(Map.class);
        verify(admin).createPartitions(captor.capture());
        assertThat(captor.getValue().get("demo").totalCount()).isEqualTo(6);
    }

    @Test
    void noPermiteReducirParticiones() {
        topicoExistente("demo", 3);

        assertThatThrownBy(() -> service.actualizarTopico("demo", new ActualizarTopicoRequest(null, null, 2)))
                .isInstanceOf(OperacionInvalidaException.class);
        verify(admin, never()).createPartitions(any());
    }

    @Test
    void noPermiteTocarTopicosInternos() {
        assertThatThrownBy(() -> service.eliminarTopico("__consumer_offsets"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.crearTopico(new CrearTopicoRequest("__x", 1, 1, null)))
                .isInstanceOf(RecursoProtegidoException.class);
    }

    /** Un grupo estable que ya confirmo offsets en el topico "pedidos", que tiene {@code particiones} particiones. */
    private void grupoConOffsets(Map<TopicPartition, OffsetAndMetadata> confirmados, int particiones) {
        ConsumerGroupDescription grupo = new ConsumerGroupDescription("reporteria-group", false, List.of(),
                "cooperative-sticky", ConsumerGroupState.STABLE, new Node(1, "kafka1", 29092));
        DescribeConsumerGroupsResult describe = mock(DescribeConsumerGroupsResult.class);
        when(describe.all()).thenReturn(KafkaFuture.completedFuture(Map.of("reporteria-group", grupo)));
        when(admin.describeConsumerGroups(any(Collection.class))).thenReturn(describe);

        ListConsumerGroupOffsetsResult offsets = mock(ListConsumerGroupOffsetsResult.class);
        when(offsets.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(confirmados));
        when(admin.listConsumerGroupOffsets("reporteria-group")).thenReturn(offsets);

        Node nodo = new Node(1, "kafka1", 29092);
        List<TopicPartitionInfo> infos = new java.util.ArrayList<>();
        for (int p = 0; p < particiones; p++) {
            infos.add(new TopicPartitionInfo(p, nodo, List.of(nodo), List.of(nodo)));
        }
        DescribeTopicsResult topicos = mock(DescribeTopicsResult.class);
        when(topicos.allTopicNames()).thenReturn(KafkaFuture.completedFuture(
                Map.of("pedidos", new TopicDescription("pedidos", false, infos))));
        when(admin.describeTopics((Collection<String>) any())).thenReturn(topicos);
    }

    /** listOffsets responde segun la especificacion pedida: ultimo offset (latest) o primero (earliest). */
    private void offsetsDelLog(Map<Integer, Long> ultimos, long primero) {
        when(admin.listOffsets(any(Map.class))).thenAnswer(inv -> {
            Map<TopicPartition, OffsetSpec> pedido = inv.getArgument(0);
            boolean earliest = pedido.values().iterator().next().getClass() == OffsetSpec.earliest().getClass();
            Map<TopicPartition, ListOffsetsResultInfo> resultado = new java.util.HashMap<>();
            pedido.keySet().forEach(tp -> resultado.put(tp, new ListOffsetsResultInfo(
                    earliest ? primero : ultimos.get(tp.partition()), 0, java.util.Optional.empty())));
            ListOffsetsResult r = mock(ListOffsetsResult.class);
            when(r.all()).thenReturn(KafkaFuture.completedFuture(resultado));
            return r;
        });
    }

    @Test
    void calculaElLagPorParticionYTotal() {
        grupoConOffsets(Map.of(new TopicPartition("pedidos", 0), new OffsetAndMetadata(10),
                new TopicPartition("pedidos", 1), new OffsetAndMetadata(4)), 2);
        offsetsDelLog(Map.of(0, 15L, 1, 4L), 0);

        ConsumerGroupLagResponse lag = service.lagDeConsumerGroup("reporteria-group");

        assertThat(lag.lagTotal()).isEqualTo(5);
        assertThat(lag.particiones()).extracting(ConsumerGroupLagResponse.ParticionLag::lag)
                .containsExactly(5L, 0L);
    }

    @Test
    void unaParticionSinOffsetConfirmadoTambienAcumulaLag() {
        // Regresion: solo la particion 0 confirmo offsets; las particiones 1 y 2 tienen mensajes sin leer
        grupoConOffsets(Map.of(new TopicPartition("pedidos", 0), new OffsetAndMetadata(5)), 3);
        offsetsDelLog(Map.of(0, 5L, 1, 2L, 2, 1L), 0);

        ConsumerGroupLagResponse lag = service.lagDeConsumerGroup("reporteria-group");

        assertThat(lag.particiones()).hasSize(3);
        assertThat(lag.lagTotal()).isEqualTo(3);
        assertThat(lag.particiones()).extracting(ConsumerGroupLagResponse.ParticionLag::offsetActual)
                .containsExactly(5L, 0L, 0L);
    }

    @Test
    void ungrupoInexistenteSeReportaComoNoEncontrado() {
        ConsumerGroupDescription muerto = new ConsumerGroupDescription("fantasma", false, List.of(), "",
                ConsumerGroupState.DEAD, null);
        DescribeConsumerGroupsResult describe = mock(DescribeConsumerGroupsResult.class);
        when(describe.all()).thenReturn(KafkaFuture.completedFuture(Map.of("fantasma", muerto)));
        when(admin.describeConsumerGroups(any(Collection.class))).thenReturn(describe);

        assertThatThrownBy(() -> service.obtenerConsumerGroup("fantasma"))
                .isInstanceOf(GroupIdNotFoundException.class);
    }
}
