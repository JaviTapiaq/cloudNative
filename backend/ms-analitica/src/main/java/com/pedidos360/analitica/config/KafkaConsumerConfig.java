package com.pedidos360.analitica.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.analitica.messaging.consumer.MensajeInvalidoException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente Kafka (consumidor) de ms-analitica. Centraliza TODA la infraestructura de mensajeria:
 * los consumidores (@KafkaListener) solo contienen logica de entrada y delegan en los servicios.
 *
 * <ul>
 *   <li>Value como String + StringJsonMessageConverter: el JSON se convierte al DTO que declara cada
 *       listener, de modo que un JSON invalido falla de forma controlada (y va al DLT) en vez de
 *       romper el poll del consumidor.</li>
 *   <li>Commit manual por registro (AckMode.RECORD): un offset solo avanza si el mensaje se proceso.</li>
 *   <li>CooperativeStickyAssignor: los rebalanceos al escalar replicas no detienen todo el grupo.</li>
 *   <li>DefaultErrorHandler: N reintentos con backoff exponencial y luego "&lt;topico&gt;.DLT".
 *       Los errores permanentes (JSON/payload invalido) van al DLT sin reintentar.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(AnaliticaKafkaProperties.class)
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, String> analiticaConsumerFactory(KafkaProperties kafkaProperties) {
        // Parte de spring.kafka.* (bootstrap-servers, client-id...) y fija los parametros avanzados.
        // El group.id lo define cada @KafkaListener.
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Plantilla para publicar en los Dead Letter Topics. Value String: el DLT conserva el JSON
     * original tal como llego, sin reserializarlo.
     */
    @Bean
    public KafkaTemplate<String, String> dltKafkaTemplate(KafkaProperties kafkaProperties) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties(null));
        props.put("acks", "all");
        props.put("enable.idempotence", true);
        props.put("max.block.ms", 5000);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props, new StringSerializer(),
                new StringSerializer()));
    }

    @Bean
    public DefaultErrorHandler analiticaErrorHandler(KafkaTemplate<String, String> dltKafkaTemplate,
                                                     AnaliticaKafkaProperties props) {
        // Mismo numero de particion en el DLT no es posible (el DLT tiene 1): se envia siempre a la 0
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dltKafkaTemplate,
                (registro, error) -> new TopicPartition(props.dltDe(registro.topic()), 0));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(props.maxReintentos());
        backOff.setInitialInterval(props.backoffInicialMs());
        backOff.setMultiplier(props.backoffMultiplicador());
        backOff.setMaxInterval(props.backoffMaximoMs());

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // Un payload invalido no mejora al reintentar: directo al DLT
        handler.addNotRetryableExceptions(MensajeInvalidoException.class);
        return handler;
    }

    /** Factory de los consumidores de negocio: convierte el JSON al DTO del listener. */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> analiticaConsumerFactory,
            DefaultErrorHandler analiticaErrorHandler,
            AnaliticaKafkaProperties props,
            ObjectMapper objectMapper) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = nuevaFactory(
                analiticaConsumerFactory, analiticaErrorHandler, props);
        factory.setRecordMessageConverter(new StringJsonMessageConverter(objectMapper));
        return factory;
    }

    /** Factory del monitor de DLT: recibe el JSON crudo, sin convertir ni reintentar. */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltListenerContainerFactory(
            ConsumerFactory<String, String> analiticaConsumerFactory,
            AnaliticaKafkaProperties props) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = nuevaFactory(
                analiticaConsumerFactory, null, props);
        factory.setConcurrency(1);
        return factory;
    }

    private ConcurrentKafkaListenerContainerFactory<String, String> nuevaFactory(
            ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler errorHandler,
            AnaliticaKafkaProperties props) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(props.concurrency());
        factory.setAutoStartup(props.autoStartup());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        // Si el topico aun no existe, el listener espera en vez de impedir el arranque del servicio
        factory.getContainerProperties().setMissingTopicsFatal(false);
        if (errorHandler != null) {
            factory.setCommonErrorHandler(errorHandler);
        }
        return factory;
    }
}
