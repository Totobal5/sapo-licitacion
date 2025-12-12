package cl.sapo.licitaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Mercado Publico API.
 */
@Configuration
@ConfigurationProperties(prefix = "mercadopublico.api")
public class MercadoPublicoProperties {

    private String baseUrl;
    private String ticket;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }
}
