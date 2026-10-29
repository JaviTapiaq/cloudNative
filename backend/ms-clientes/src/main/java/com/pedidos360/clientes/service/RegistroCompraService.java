package com.pedidos360.clientes.service;

import com.pedidos360.clientes.model.Cliente;
import com.pedidos360.clientes.model.CompraCliente;
import com.pedidos360.clientes.repository.ClienteRepository;
import com.pedidos360.clientes.repository.CompraClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Logica de negocio: registra una compra en el historial del cliente, creandolo si es
 * su primera compra. No conoce RabbitMQ.
 */
@Service
public class RegistroCompraService {

    private static final Logger log = LoggerFactory.getLogger(RegistroCompraService.class);

    private final ClienteRepository clienteRepository;
    private final CompraClienteRepository compraRepository;

    public RegistroCompraService(ClienteRepository clienteRepository, CompraClienteRepository compraRepository) {
        this.clienteRepository = clienteRepository;
        this.compraRepository = compraRepository;
    }

    @Transactional
    public String registrar(Long pedidoId, String email, String nombreCompleto, BigDecimal total, LocalDateTime fecha) {
        if (compraRepository.existsByPedidoId(pedidoId)) {
            log.info("Pedido {} ya registrado; evento duplicado ignorado", pedidoId);
            return "Pedido " + pedidoId + " ya registrado (duplicado ignorado)";
        }

        String emailNormalizado = email.trim().toLowerCase();
        boolean nuevo = !clienteRepository.existsByEmail(emailNormalizado);
        Cliente cliente = clienteRepository.findByEmail(emailNormalizado)
                .orElseGet(() -> nuevoCliente(emailNormalizado, nombreCompleto));

        BigDecimal monto = total != null ? total : BigDecimal.ZERO;
        LocalDateTime fechaCompra = fecha != null ? fecha : LocalDateTime.now();
        cliente.setCantidadPedidos(valor(cliente.getCantidadPedidos()) + 1);
        cliente.setTotalComprado((cliente.getTotalComprado() != null ? cliente.getTotalComprado() : BigDecimal.ZERO).add(monto));
        cliente.setUltimaCompra(fechaCompra);
        cliente = clienteRepository.save(cliente);

        compraRepository.save(new CompraCliente(pedidoId, cliente.getId(), monto, fechaCompra));

        log.info("Compra del pedido {} registrada para cliente {} ({})", pedidoId, cliente.getId(), emailNormalizado);
        return (nuevo ? "Cliente nuevo " : "Cliente ") + emailNormalizado + ": pedido " + pedidoId
                + " registrado (" + cliente.getCantidadPedidos() + " pedido(s) en total)";
    }

    private Cliente nuevoCliente(String email, String nombreCompleto) {
        String nombre = nombreCompleto != null && !nombreCompleto.isBlank() ? nombreCompleto.trim() : email.split("@")[0];
        String[] partes = nombre.split("\\s+", 2);
        Cliente cliente = new Cliente();
        cliente.setNombre(partes[0]);
        cliente.setApellido(partes.length > 1 ? partes[1] : "-");
        cliente.setEmail(email);
        return cliente;
    }

    private int valor(Integer numero) {
        return numero != null ? numero : 0;
    }
}
