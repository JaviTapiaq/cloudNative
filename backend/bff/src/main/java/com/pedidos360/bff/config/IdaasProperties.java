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
