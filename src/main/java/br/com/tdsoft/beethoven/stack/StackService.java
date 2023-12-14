package br.com.tdsoft.beethoven.stack;

import br.com.tdsoft.beethoven.common.PageQuery;
import br.com.tdsoft.beethoven.stack.dto.ResourcePutRequest;
import br.com.tdsoft.beethoven.stack.dto.ResourceRequest;
import br.com.tdsoft.beethoven.stack.dto.StackPutRequest;
import br.com.tdsoft.beethoven.stack.dto.StackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;
import java.util.List;

import static br.com.tdsoft.beethoven.common.R2dbcEntityTemplateOps.merge;
import static br.com.tdsoft.beethoven.common.R2dbcEntityTemplateOps.mergeAll;
import static java.util.Objects.isNull;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

@Slf4j
@Service
@RequiredArgsConstructor
public class StackService {
    private final TransactionalOperator transactionalOperator;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public Mono<Stack> findWithResourcesById(Long id) {
        return applyResources(findById(id));
    }

    public Mono<Stack> findById(Long id) {
        return r2dbcEntityTemplate.select(Stack.class)
                .matching(query(
                        where("deleted").is(false)
                                .and("id").is(id)
                ))
                .one()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Stack not found")));
    }

    public Mono<Stack> findWithResourcesByExternalId(String identifier) {
        return applyResources(findByExternalId(identifier));
    }

    public Mono<Stack> findByExternalId(String identifier) {
        return r2dbcEntityTemplate.select(Stack.class)
                .matching(query(
                        where("deleted").is(false)
                                .and("external_id").is(identifier)
                ))
                .one()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Stack not found")));
    }

    public Mono<Page<Stack>> findAllStacks(PageQuery pageQuery) {
        var pageRequest = pageQuery.toPageRequest("name");
        var baseQuery = query(where("deleted").is(false));
        return Mono.zip(
                        r2dbcEntityTemplate.select(Stack.class)
                                .matching(baseQuery.sort(pageRequest.getSort())
                                        .limit(pageRequest.getPageSize())
                                        .offset(pageRequest.getOffset()))
                                .all()
                                .collectList(),
                        r2dbcEntityTemplate.count(baseQuery, Stack.class)
                )
                .subscribeOn(Schedulers.parallel())
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageRequest, tuple.getT2()));
    }

    public Flux<Resource> findAllByIdStack(Long id) {
        return r2dbcEntityTemplate.select(Resource.class)
                .matching(query(
                        where("deleted").is(false)
                                .and("id_stack").is(id)
                )).all();
    }

    public Mono<Resource> findResourceById(Long id) {
        return r2dbcEntityTemplate.select(Resource.class)
                .matching(query(
                        where("deleted").is(false)
                                .and("id").is(id)
                ))
                .one()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found")));
    }

    private Mono<Stack> applyResources(Mono<Stack> stackMono) {
        return stackMono.flatMap(stack ->
                findAllByIdStack(stack.getId())
                        .collectList()
                        .map(resources -> {
                            stack.setResources(resources);
                            return stack;
                        })
        );
    }

    public Mono<Stack> save(StackRequest stackRequest) {
        return findByExternalId(stackRequest.externalId())
                .onErrorResume(ResponseStatusException.class, ignore -> Mono.empty())
                .flatMap(stack -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "External id already exist")))
                .switchIfEmpty(
                        transactionalOperator.transactional(
                                merge(r2dbcEntityTemplate, stackRequest.toStack())
                                        .flatMap(stack -> mergeAll(r2dbcEntityTemplate, Flux.fromIterable(getResources(stackRequest.resources(), stack)))
                                                .collectList()
                                                .map(resources -> {
                                                    stack.setResources(resources);
                                                    return stack;
                                                })
                                        )
                        )
                ).cast(Stack.class);
    }

    public Mono<Stack> update(StackPutRequest stackPutRequest) {
        return findByExternalId(stackPutRequest.externalId())
                .onErrorResume(ResponseStatusException.class, ignore -> Mono.empty())
                .filter(stack -> !stack.getId().equals(stackPutRequest.id()))
                .flatMap(stack -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "External id already exist")))
                .switchIfEmpty(
                        findWithResourcesById(stackPutRequest.id())
                                .map(stackPutRequest::apply)
                                .flatMap(it ->
                                        transactionalOperator.transactional(
                                                merge(r2dbcEntityTemplate, it).flatMap(stack ->
                                                        mergeAll(r2dbcEntityTemplate, Flux.fromIterable(generateSources(stackPutRequest.resources(), stack)))
                                                                .collectSortedList(Comparator.comparing(Resource::getResourceOrder))
                                                                .map(resources -> {
                                                                    stack.setResources(resources);
                                                                    return stack;
                                                                })
                                                                .switchIfEmpty(Mono.just(stack))
                                                )
                                        )
                                )
                ).cast(Stack.class);
    }

    public Mono<Void> deleteStack(Long id) {
        return findById(id)
                .flatMap(stack -> {
                    stack.setDeleted(true);
                    return merge(r2dbcEntityTemplate, stack);
                }).then();
    }

    public Mono<Void> deleteResource(Long stackId, Long resourceId) {
        return findById(stackId)
                .flatMap(stack -> findResourceById(resourceId))
                .filter(resource -> resource.getIdStack().equals(stackId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found")))
                .flatMap(resource -> {
                    resource.setDeleted(true);
                    return merge(r2dbcEntityTemplate, resource);
                }).then();
    }

    private List<Resource> generateSources(List<ResourcePutRequest> resources, Stack stack) {
        if (isNull(resources)) {
            return List.of();
        }
        return resources.stream()
                .map(it -> updateSource(it, stack))
                .toList();
    }

    private Resource updateSource(ResourcePutRequest resourcePutRequest, Stack stack) {
        if (isNull(resourcePutRequest.id())) {
            var resource = resourcePutRequest.toResource();
            resource.setIdStack(stack.getId());
            return resource;
        }
        return stack.getResources()
                .stream()
                .filter(resource -> resource.getId().equals(resourcePutRequest.id()))
                .map(resourcePutRequest::apply)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource " + resourcePutRequest.id() + " not found"));
    }

    private List<Resource> getResources(List<ResourceRequest> resources, Stack stack) {
        return resources.stream()
                .map(ResourceRequest::toResource)
                .peek(resource -> resource.setIdStack(stack.getId()))
                .toList();
    }
}
