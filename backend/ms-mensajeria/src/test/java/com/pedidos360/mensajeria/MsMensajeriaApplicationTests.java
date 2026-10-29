package com.pedidos360.mensajeria;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto (topologia, listeners, API) arranque. Los listeners no se
 * inician para no requerir un broker RabbitMQ durante el build.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class MsMensajeriaApplicationTests {

    @Test
    void contextLoads() {
    }
}
