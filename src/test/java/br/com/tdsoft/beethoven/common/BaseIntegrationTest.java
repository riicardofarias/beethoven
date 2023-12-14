package br.com.tdsoft.beethoven.common;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@ActiveProfiles("h2")
@SpringBootTest(webEnvironment = RANDOM_PORT)
public abstract class BaseIntegrationTest {
    private static boolean SCHEMA_CREATED = false;
    @Autowired
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @BeforeAll
    static void init(@Autowired R2dbcEntityTemplate r2dbcEntityTemplate) {
        if (!SCHEMA_CREATED) {
            executeScript(r2dbcEntityTemplate, "/db/migration/V1_1_0__init.sql");
            SCHEMA_CREATED = true;
        }
    }

    protected void executeScripts(String... paths) {
        for (var path : paths)
            executeScript(path);
    }

    protected void executeScript(String path) {
        executeScript(r2dbcEntityTemplate, path);
    }

    private static void executeScript(R2dbcEntityTemplate r2dbcEntityTemplate, String path) {
        log.info("Running script: {}", path);
        r2dbcEntityTemplate.getDatabaseClient()
                .sql(getStringScript(path))
                .then()
                .block();
    }

    @SneakyThrows
    private static String getStringScript(String path) {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes());
    }
}
