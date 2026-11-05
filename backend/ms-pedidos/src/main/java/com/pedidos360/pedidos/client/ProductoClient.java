package com.pedidos360.pedidos.client;

import com.pedidos360.pedidos.exception.ProductoNoEncontradoException;
import com.pedidos360.pedidos.exception.ServicioNoDisponibleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Consulta sincrona a ms-productos para obtener el precio y stock vigentes.
 *
 * Es sincrona a proposito: el precio forma parte de la transaccion y sin el no se puede
 * crear el pedido. Las tareas posteriores (stock, cliente, notificaciones) van por RabbitMQ.
 */
@Component
public class ProductoClient {

    private final RestClient restClient;

    public ProductoClient(@Value("${pedidos.productos.url}") String productosUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(productosUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public ProductoInfo obtener(Long productoId) {
        try {
            ProductoInfo producto = restClient.get()
                    .uri("/{id}", productoId)
                    .retrieve()
                    .body(ProductoInfo.class);
            if (producto == null) {
                throw new ProductoNoEncontradoException(productoId);
            }
            return producto;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ProductoNoEncontradoException(productoId);
            }
            throw new ServicioNoDisponibleException("ms-productos respondio " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            throw new ServicioNoDisponibleException("ms-productos no esta disponible");
        } catch (RestClientException e) {
            throw new ServicioNoDisponibleException("Error consultando ms-productos: " + e.getMessage());
        }
    }
}
