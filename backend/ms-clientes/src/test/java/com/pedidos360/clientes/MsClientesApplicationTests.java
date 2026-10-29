package com.pedidos360.clientes;

import com.pedidos360.clientes.model.Cliente;
import com.pedidos360.clientes.repository.ClienteRepository;
import com.pedidos360.clientes.service.RegistroCompraService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Arranque del contexto sin broker (listeners detenidos) y registro idempotente de compras.
 */
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class MsClientesApplicationTests {

    @Autowired
    private RegistroCompraService registroCompraService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void registraClienteNuevoYNoDuplicaCompras() {
        registroCompraService.registrar(1L, "Ana.Perez@Duocuc.cl", "Ana Perez", new BigDecimal("1000"), LocalDateTime.now());
        registroCompraService.registrar(1L, "ana.perez@duocuc.cl", "Ana Perez", new BigDecimal("1000"), LocalDateTime.now());
        registroCompraService.registrar(2L, "ana.perez@duocuc.cl", "Ana Perez", new BigDecimal("500"), LocalDateTime.now());

        Cliente cliente = clienteRepository.findByEmail("ana.perez@duocuc.cl").orElseThrow();
        assertThat(cliente.getNombre()).isEqualTo("Ana");
        assertThat(cliente.getApellido()).isEqualTo("Perez");
        assertThat(cliente.getCantidadPedidos()).isEqualTo(2);
        assertThat(cliente.getTotalComprado()).isEqualByComparingTo("1500");
    }
}
