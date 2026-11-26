package com.pedidos360.kafkaadmin;

import com.pedidos360.kafkaadmin.config.KafkaAdminProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// Sin brokers en el entorno de pruebas: se desactiva el bootstrap de topicos
@SpringBootTest(properties = "kafka.admin.bootstrap-enabled=false")
class MsKafkaAdminApplicationTests {

    @Autowired
    private KafkaAdminProperties props;

    @Test
    void contextLoadsYLosTopicosInicialesSeLeenDelYml() {
        assertThat(props.bootstrapTopics()).extracting(KafkaAdminProperties.TopicoInicial::nombre)
                .contains("pedidos360.pedidos.eventos", "pedidos360.auditoria.eventos");
        KafkaAdminProperties.TopicoInicial pedidos = props.bootstrapTopics().get(0);
        assertThat(pedidos.particiones()).isEqualTo(3);
        assertThat(pedidos.factorReplicacion()).isEqualTo((short) 3);
        assertThat(pedidos.configuraciones()).containsEntry("retention.ms", "604800000")
                .containsEntry("cleanup.policy", "delete");
    }
}
