package com.pedidos360.kafkaadmin.config;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente administrativo de Kafka (AdminClient). Es el unico punto de conexion a los brokers;
 * se crea sin conectar, por lo que el servicio arranca aunque Kafka aun no este disponible.
 */
@Configuration
@EnableConfigurationProperties(KafkaAdminProperties.class)
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public Admin kafkaAdmin(KafkaAdminProperties props) {
        Map<String, Object> config = new HashMap<>();
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        config.put(AdminClientConfig.CLIENT_ID_CONFIG, "ms-kafka-admin");
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, props.requestTimeoutMs());
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, props.requestTimeoutMs());
        config.put(AdminClientConfig.RETRIES_CONFIG, 3);
        return Admin.create(config);
    }
}
