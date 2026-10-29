package com.pedidos360.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "bff.idaas")
public class IdaasProperties {

    private String issuer = "";
    private String audience = "";
    private String jwksUri = "";
    private List<String> validRoles = new ArrayList<>();
    // Exigir roles en JwtAuthFilter (requiere roles de aplicacion asignados en Azure)
    private boolean enforceRoles = false;
    private String adminRole = "Admin";
    private List<String> adminPaths = new ArrayList<>(List.of("/api/admin/", "/api/mensajeria/"));

    public boolean isEnforceRoles() {
        return enforceRoles;
    }

    public void setEnforceRoles(boolean enforceRoles) {
        this.enforceRoles = enforceRoles;
    }

    public String getAdminRole() {
        return adminRole;
    }

    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    public List<String> getAdminPaths() {
        return adminPaths;
    }

    public void setAdminPaths(List<String> adminPaths) {
        this.adminPaths = adminPaths;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public List<String> getValidRoles() {
        return validRoles;
    }

    public void setValidRoles(List<String> validRoles) {
        this.validRoles = validRoles;
    }
}
