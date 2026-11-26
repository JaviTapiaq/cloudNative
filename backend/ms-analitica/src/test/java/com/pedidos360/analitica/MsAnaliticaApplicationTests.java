package com.pedidos360.analitica;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Sin brokers en el entorno de pruebas: los listeners no se inician
@SpringBootTest(properties = "analitica.kafka.auto-startup=false")
class MsAnaliticaApplicationTests {

    @Test
    void contextLoads() {
    }
}
