package cl.sapo.licitaciones.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuration for RestClient to consume Mercado Publico API.
 */
@Configuration
public class RestClientConfig {

    @Value("${mercadopublico.api.base-url}")
    private String baseUrl;

    @Bean
    public RestClient mercadoPublicoRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
