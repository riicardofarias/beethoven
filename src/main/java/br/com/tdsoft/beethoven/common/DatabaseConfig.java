package br.com.tdsoft.beethoven.common;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;

import java.util.List;

@Configuration
class DatabaseConfig {
    @Bean
    R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
        return R2dbcCustomConversions.of(
                DialectResolver.getDialect(connectionFactory),
                List.of(new ByteBooleanConverter())
        );
    }
}
