package com.pedidos360.bff.filter;

import com.pedidos360.bff.config.IdaasProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extraeIdentidadDelToken() throws Exception {
        autenticar(jwt("live.com#Ana.Perez@Outlook.com", List.of("User")));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/pedidos");
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthFilter(propiedades(false)).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(request.getAttribute(JwtAuthFilter.ATTR_USUARIO_EMAIL)).isEqualTo("ana.perez@outlook.com");
        assertThat(request.getAttribute(JwtAuthFilter.ATTR_USUARIO_NOMBRE)).isEqualTo("Ana Perez");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void conRolesExigidosRechazaUsuarioSinRol() throws Exception {
        autenticar(jwt("ana@duocuc.cl", List.of()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthFilter(propiedades(true)).doFilter(new MockHttpServletRequest("GET", "/api/productos"), response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void rutaDeMensajeriaRequiereRolAdmin() throws Exception {
        autenticar(jwt("ana@duocuc.cl", List.of("User")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JwtAuthFilter(propiedades(true)).doFilter(new MockHttpServletRequest("GET", "/api/admin/queues"),
                response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void sinTokenDelegaEnSpringSecurity() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        new JwtAuthFilter(propiedades(true)).doFilter(new MockHttpServletRequest("GET", "/api/productos"),
                new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    private static IdaasProperties propiedades(boolean exigirRoles) {
        IdaasProperties props = new IdaasProperties();
        props.setValidRoles(List.of("Admin", "User"));
        props.setEnforceRoles(exigirRoles);
        return props;
    }

    private static Jwt jwt(String uniqueName, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("sub-123")
                .claim("unique_name", uniqueName)
                .claim("name", "Ana Perez")
                .claim("roles", roles)
                .build();
    }

    private static void autenticar(Jwt jwt) {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
