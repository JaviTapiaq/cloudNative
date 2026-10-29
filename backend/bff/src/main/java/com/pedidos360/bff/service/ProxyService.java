package com.pedidos360.bff.service;

import com.pedidos360.bff.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ProxyService {

    static final String HEADER_USUARIO_EMAIL = "X-Usuario-Email";
    static final String HEADER_USUARIO_NOMBRE = "X-Usuario-Nombre";

    @Value("${bff.services.clientes-url}")
    private String clientesUrl;

    @Value("${bff.services.productos-url}")
    private String productosUrl;

    @Value("${bff.services.pedidos-url}")
    private String pedidosUrl;

    @Value("${bff.services.mensajeria-url}")
    private String mensajeriaUrl;

    @Value("${bff.services.admin-url}")
    private String adminUrl;

    private final RestClient restClient;

    public ProxyService() {
        this.restClient = RestClient.builder().build();
    }

    /**
     * Reenvia la peticion al microservicio y devuelve su respuesta tal cual (codigo HTTP,
     * tipo de contenido y cuerpo), incluidos los errores 4xx/5xx de negocio.
     * La identidad del usuario sale del JWT validado, nunca de cabeceras del navegador.
     */
    public ResponseEntity<String> forward(HttpServletRequest request, String body, String service) {
        String baseUrl = getBaseUrl(service);
        String path = extractPath(request, service);
        String url = baseUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Object email = request.getAttribute(JwtAuthFilter.ATTR_USUARIO_EMAIL);
        Object nombre = request.getAttribute(JwtAuthFilter.ATTR_USUARIO_NOMBRE);
        if (email != null) {
            headers.set(HEADER_USUARIO_EMAIL, email.toString());
        }
        if (nombre != null) {
            // Codificado para admitir tildes y n en las cabeceras HTTP
            headers.set(HEADER_USUARIO_NOMBRE, URLEncoder.encode(nombre.toString(), StandardCharsets.UTF_8));
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        try {
            return restClient.method(method)
                    .uri(url)
                    .headers(h -> h.addAll(headers))
                    .body(body == null ? "" : body)
                    .exchange((req, res) -> {
                        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(res.getStatusCode());
                        MediaType contentType = res.getHeaders().getContentType();
                        if (contentType != null) {
                            respuesta.contentType(contentType);
                        }
                        return respuesta.body(StreamUtils.copyToString(res.getBody(), StandardCharsets.UTF_8));
                    });
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"title\":\"Servicio no disponible\",\"status\":502,\"detail\":\"No fue posible comunicarse con "
                            + service + "\"}");
        }
    }

    private String getBaseUrl(String service) {
        return switch (service) {
            case "clientes" -> clientesUrl;
            case "productos" -> productosUrl;
            case "pedidos" -> pedidosUrl;
            case "mensajeria" -> mensajeriaUrl;
            case "admin" -> adminUrl;
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
