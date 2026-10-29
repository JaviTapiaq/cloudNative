package com.pedidos360.productos;

import com.pedidos360.productos.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arranque del contexto sin broker (listeners detenidos) y carga del catalogo inicial.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class MsProductosApplicationTests {

    @Autowired
    private ProductoRepository repository;

    @Test
    void contextLoads() {
    }

    @Test
    void cargaCatalogoInicial() {
        assertThat(repository.count()).isEqualTo(6);
    }
}
