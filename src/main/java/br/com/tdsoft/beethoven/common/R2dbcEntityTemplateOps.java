package br.com.tdsoft.beethoven.common;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class R2dbcEntityTemplateOps {
    private R2dbcEntityTemplateOps() {
    }

    public static <T extends Entity> Mono<T> merge(R2dbcEntityTemplate r2dbcEntityTemplate, T entity) {
        return entity.hasId() ? r2dbcEntityTemplate.update(entity) : r2dbcEntityTemplate.insert(entity);
    }

    public static <T extends Entity> Flux<T> mergeAll(R2dbcEntityTemplate r2dbcEntityTemplate, Flux<T> entityFlux) {
        return entityFlux.flatMap(entity -> merge(r2dbcEntityTemplate, entity));
    }
}
