package br.com.tdsoft.beethoven.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.reactive.server.WebTestClient;

@TestConfiguration
class WebTestClientConfig {
    @LocalServerPort
    private int port;
    @Value("${spring.webflux.base-path}")
    private String basePath;

    @Bean
    WebTestClient webTestClient() {
        return WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:" + port + basePath)
                .build();
    }
}
