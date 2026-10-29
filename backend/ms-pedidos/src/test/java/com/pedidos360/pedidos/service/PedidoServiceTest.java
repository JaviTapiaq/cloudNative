package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.client.ProductoClient;
import com.pedidos360.pedidos.client.ProductoInfo;
import com.pedidos360.pedidos.event.PedidoEvent;
import com.pedidos360.pedidos.exception.PedidoInvalidoException;
import com.pedidos360.pedidos.exception.StockInsuficienteException;
import com.pedidos360.pedidos.model.DetallePedido;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedidoServiceTest {

    private final PedidoRepository repository = mock(PedidoRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ProductoClient productoClient = mock(ProductoClient.class);
    private final PedidoService service = new PedidoService(repository, eventPublisher, productoClient);

    @Test
    void usaPrecioVigenteYPublicaEvento() {
        when(productoClient.obtener(1L)).thenReturn(new ProductoInfo(1L, "Mouse", new BigDecimal("10000"), 5));
        when(repository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        // El navegador envia un precio manipulado de $1: debe ignorarse
        Pedido guardado = service.save(pedido(new DetallePedido(1L, 2, BigDecimal.ONE)));

        assertThat(guardado.getTotal()).isEqualByComparingTo("20000");
        assertThat(guardado.getDetalles().get(0).getNombreProducto()).isEqualTo("Mouse");
        ArgumentCaptor<PedidoEvent> evento = ArgumentCaptor.forClass(PedidoEvent.class);
        verify(eventPublisher).publishEvent(evento.capture());
        assertThat(evento.getValue().items()).hasSize(1);
    }

    @Test
    void rechazaStockInsuficienteSinGuardar() {
        when(productoClient.obtener(1L)).thenReturn(new ProductoInfo(1L, "Silla", new BigDecimal("100"), 1));

        // Dos lineas del mismo producto suman 3 unidades, mas que el stock disponible
        assertThatThrownBy(() -> service.save(pedido(new DetallePedido(1L, 2, null), new DetallePedido(1L, 1, null))))
                .isInstanceOf(StockInsuficienteException.class);
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rechazaPedidoVacio() {
        assertThatThrownBy(() -> service.save(new Pedido()))
                .isInstanceOf(PedidoInvalidoException.class);
    }

    private static Pedido pedido(DetallePedido... detalles) {
        Pedido pedido = new Pedido();
        pedido.setDetalles(new java.util.ArrayList<>(List.of(detalles)));
        return pedido;
    }
}
