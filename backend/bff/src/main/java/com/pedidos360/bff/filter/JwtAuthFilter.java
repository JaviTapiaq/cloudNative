package com.pedidos360.bff.filter;

import com.pedidos360.bff.config.IdaasProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Segundo filtro JWT del BFF. Se ejecuta despues de BearerTokenAuthenticationFilter
 * (Spring Security), que ya valido la firma, el emisor, la audiencia y la expiracion.
 *
 * <ol>
 *   <li>Valida los roles del token (bff.idaas.valid-roles) y el rol de administrador
 *       para las rutas de mensajeria. Se activa con bff.idaas.enforce-roles=true.</li>
 *   <li>Extrae la identidad del usuario (correo y nombre) y la deja disponible para que
 *       ProxyService la reenvie a los microservicios.</li>
 *   <li>Registra en el log quien hace cada peticion.</li>
 * </ol>
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USUARIO_EMAIL = "bff.usuario.email";
    public static final String ATTR_USUARIO_NOMBRE = "bff.usuario.nombre";

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final List<String> CLAIMS_CORREO = List.of("email", "preferred_username", "upn", "unique_name");

    private final IdaasProperties properties;

    public JwtAuthFilter(IdaasProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            // Sin token valido: Spring Security respondera 401 en el siguiente paso
            filterChain.doFilter(request, response);
            return;
        }

        Jwt jwt = jwtAuth.getToken();
        List<String> roles = roles(jwt);
        String usuario = correo(jwt);

        if (properties.isEnforceRoles()) {
            if (roles.stream().noneMatch(properties.getValidRoles()::contains)) {
                rechazar(response, usuario, "El usuario no tiene un rol permitido " + properties.getValidRoles());
                return;
            }
            if (esRutaAdministrativa(request) && !roles.contains(properties.getAdminRole())) {
                rechazar(response, usuario, "Se requiere el rol " + properties.getAdminRole());
                return;
            }
        }

        request.setAttribute(ATTR_USUARIO_EMAIL, usuario);
        request.setAttribute(ATTR_USUARIO_NOMBRE, jwt.getClaimAsString("name"));
        log.info("{} {} | usuario={} roles={}", request.getMethod(), request.getRequestURI(), usuario, roles);

        filterChain.doFilter(request, response);
    }

    private List<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null ? roles : List.of();
    }

    /**
     * Correo del usuario segun los claims disponibles (varian entre tokens v1/v2 y
     * cuentas invitadas). "live.com#" es el prefijo que Azure agrega a cuentas externas.
     */
    static String correo(Jwt jwt) {
        return CLAIMS_CORREO.stream()
                .map(jwt::getClaimAsString)
                .filter(valor -> valor != null && !valor.isBlank())
                .map(valor -> valor.contains("#") ? valor.substring(valor.lastIndexOf('#') + 1) : valor)
                .map(String::toLowerCase)
                .findFirst()
                .orElse(jwt.getSubject());
    }

    private boolean esRutaAdministrativa(HttpServletRequest request) {
        return properties.getAdminPaths().stream().anyMatch(request.getRequestURI()::startsWith);
    }

    private void rechazar(HttpServletResponse response, String usuario, String motivo) throws IOException {
        log.warn("Acceso denegado a {}: {}", usuario, motivo);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"title\":\"Acceso denegado\",\"status\":403,\"detail\":\"" + motivo + "\"}");
    }
}
