package com.pedidos360.bff.filter;

import com.pedidos360.bff.service.JwtValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtValidationService jwtValidationService;

    public JwtAuthFilter(JwtValidationService jwtValidationService) {
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Falta token de autenticacion\"}");
            response.setContentType("application/json");
            return;
        }

        String token = authHeader.substring(7);
        JwtValidationService.ValidationResult result = jwtValidationService.validate(token);

        if (!result.isValid()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"" + result.getError() + "\"}");
            response.setContentType("application/json");
            return;
        }

        request.setAttribute("jwt.subject", result.getSubject());
        request.setAttribute("jwt.roles", result.getRoles());

        filterChain.doFilter(request, response);
    }
}
