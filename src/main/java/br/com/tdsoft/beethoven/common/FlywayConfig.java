package br.com.tdsoft.beethoven.common;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!h2")
class FlywayConfig {
    @Bean(initMethod = "migrate")
    Flyway flyway(R2dbcProperties r2dbcProperties) {
        return new Flyway(
                Flyway.configure()
                        .baselineOnMigrate(true)
                        .validateOnMigrate(false)
                        .dataSource(
                                r2dbcProperties.getUrl().replace("r2dbc", "jdbc"),
                                r2dbcProperties.getUsername(),
                                r2dbcProperties.getPassword()
                        )
        );
    }
}
