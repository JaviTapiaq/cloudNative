package com.pedidos360.bff.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProxyService {

    @Value("${bff.services.clientes-url}")
    private String clientesUrl;

    @Value("${bff.services.productos-url}")
    private String productosUrl;

    @Value("${bff.services.pedidos-url}")
    private String pedidosUrl;

    private final RestClient restClient;

    public ProxyService() {
        this.restClient = RestClient.builder().build();
    }

    public ResponseEntity<String> forward(HttpServletRequest request, String body, String service) {
        String baseUrl = getBaseUrl(service);
        String path = extractPath(request, service);
        String url = baseUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        try {
            return restClient.method(method)
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(body == null ? "" : body)
                    .retrieve()
                    .toEntity(String.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\": \"Error comunicandose con " + service + ": " + e.getMessage() + "\"}");
        }
    }

    private String getBaseUrl(String service) {
        return switch (service) {
            case "clientes" -> clientesUrl;
            case "productos" -> productosUrl;
            case "pedidos" -> pedidosUrl;
            default -> throw new IllegalArgumentException("Servicio desconocido: " + service);
        };
    }

    private String extractPath(HttpServletRequest request, String service) {
        String uri = request.getRequestURI();
        String prefix = "/api/" + service;
        String path = uri.substring(uri.indexOf(prefix) + prefix.length());
        String query = request.getQueryString();
        if (query != null) {
            path += "?" + query;
        }
        return path;
    }
}
