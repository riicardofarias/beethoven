package br.com.tdsoft.beethoven.job;

import br.com.tdsoft.beethoven.common.PageQuery;
import br.com.tdsoft.beethoven.job.dto.JobRequest;
import br.com.tdsoft.beethoven.job.dto.JobStepMetaData;
import br.com.tdsoft.beethoven.job.dto.JobStepReceiver;
import br.com.tdsoft.beethoven.job.dto.JobStepSender;
import br.com.tdsoft.beethoven.job.rabbitmq.AppRabbitmqProps;
import br.com.tdsoft.beethoven.stack.Resource;
import br.com.tdsoft.beethoven.stack.Stack;
import br.com.tdsoft.beethoven.stack.StackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static br.com.tdsoft.beethoven.common.R2dbcEntityTemplateOps.merge;
import static java.util.Objects.isNull;
import static org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT;
import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.empty;
import static org.springframework.data.relational.core.query.Query.query;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {
    private final AppRabbitmqProps appRabbitmqProps;
    private final TransactionalOperator transactionalOperator;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final StackService stackService;
    private final ObjectMapper objectMapper;
    private final Sender sender;

    public Mono<Job> startNewJob(JobRequest jobRequest) {
        return stackService.findWithResourcesByExternalId(jobRequest.identifier())
                .filter(Stack::hasSources)
                .flatMap(stack -> transactionalOperator.transactional(
                        startNewJob(stack, stack.getResources().get(0), jobRequest.payload())
                ))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stack not contains resources")));
    }

    private Mono<Job> startNewJob(Stack stack, Resource resource, Object payload) {
        return merge(r2dbcEntityTemplate, factoryJob(stack))
                .flatMap(job -> startNewStep(job, resource, payload));
    }

    private Mono<Job> startNewStep(Job job, Resource resource, Object payload) {
        return Mono.fromSupplier(() -> factoryStep(job, resource, payload))
                .flatMap(step -> merge(r2dbcEntityTemplate, step))
                .flatMap(
                        step -> convertAndSend(resource, step, payload)
                                .then(Mono.fromSupplier(() -> {
                                    job.setSteps(List.of(step));
                                    return job;
                                }))
                );
    }

    private Mono<Void> convertAndSend(Resource resource, Step step, Object payload) {
        var jobStepSender = JobStepSender.builder()
                .uuid(step.getUuid())
                .payload(payload)
                .metaData(JobStepMetaData.from(appRabbitmqProps))
                .build();
        return sender.send(
                Mono.fromSupplier(
                        () -> from(writeValueAsBytes(jobStepSender), resource.getRouting())
                )
        );
    }

    private OutboundMessage from(byte[] bytes, String routing) {
        return new OutboundMessage(
                appRabbitmqProps.getExchange(),
                routing,
                getBasicProperties(),
                bytes
        );
    }

    private AMQP.BasicProperties getBasicProperties() {
        return new AMQP.BasicProperties().builder()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .deliveryMode(MessageDeliveryMode.toInt(PERSISTENT))
                .build();
    }

    public Mono<Void> jobPipeline(JobStepReceiver jobStepReceiver) {
        return findStepByUuid(jobStepReceiver.getUuid())
                .flatMap(step -> {
                    if (step.isFinalized()) {
                        return Mono.error(new RuntimeException("Step already finished"));
                    }
                    return transactionalOperator.transactional(
                            failedJobOrCompleteJobOrStartNestStep(updateStep(jobStepReceiver, step), jobStepReceiver.getPayload())
                    );
                });
    }

    private Mono<Step> updateStep(JobStepReceiver jobStepReceiver, Step step) {
        step.setEndAt(LocalDateTime.now());
        if (jobStepReceiver.isSuccess()) {
            step.setStatus(Status.COMPLETED);
            step.setErrorTracer(null);
        }
        if (!jobStepReceiver.isSuccess()) {
            step.setStatus(Status.FAILED);
            step.setErrorTracer(jobStepReceiver.getErrorTracer());
        }
        return merge(r2dbcEntityTemplate, step);
    }

    public Mono<Void> failedJobOrCompleteJobOrStartNestStep(Mono<Step> stepMono, Object payload) {
        return stepMono.flatMap(step -> {
                    if (step.getStatus().equals(Status.FAILED)) {
                        return findJobById(step.getIdJob())
                                .flatMap(job -> {
                                    job.setStatus(Status.FAILED);
                                    job.setEndAt(LocalDateTime.now());
                                    return merge(r2dbcEntityTemplate, job);
                                }).then();
                    }
                    return Mono.just(step);
                })
                .cast(Step.class)
                .flatMap(step -> completeJobOrStartNestStep(step, payload))
                .then();
    }

    public Mono<Void> completeJobOrStartNestStep(Step step, Object payload) {
        return findJobById(step.getIdJob())
                .flatMap(job ->
                        stackService.findWithResourcesById(job.getIdStack())
                                .flatMap(stack ->
                                        stackService.findResourceById(step.getIdResource())
                                                .flatMap(resource -> {
                                                    var nextResource = stack.getNextResource(resource);
                                                    if (nextResource.isEmpty()) {
                                                        job.setStatus(Status.COMPLETED);
                                                        job.setEndAt(LocalDateTime.now());
                                                        return merge(r2dbcEntityTemplate, job);
                                                    }
                                                    return startNewStep(job, nextResource.get(), payload);
                                                })
                                )
                ).then();
    }

    public Mono<Step> findStepByUuid(String uuid) {
        return r2dbcEntityTemplate.select(Step.class)
                .matching(query(
                        where("uuid").is(uuid)
                ))
                .one()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found")));
    }

    public Mono<Job> findJobById(Long id) {
        return r2dbcEntityTemplate.select(Job.class)
                .matching(query(
                        where("id").is(id)
                ))
                .one()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found")));
    }

    public Mono<Job> findJobWithStepsById(Long id) {
        return applySteps(findJobById(id));
    }

    private Mono<Job> applySteps(Mono<Job> jobMono) {
        return jobMono.flatMap(job ->
                findAllStepsByIdJob(job.getId())
                        .collectList()
                        .map(steps -> {
                            job.setSteps(steps);
                            return job;
                        })
        );
    }

    public Mono<Page<Job>> findAllJobs(PageQuery pageQuery) {
        var pageRequest = pageQuery.toPageRequest();
        return Mono.zip(
                        r2dbcEntityTemplate.select(Job.class)
                                .matching(empty().sort(pageRequest.getSort())
                                        .limit(pageRequest.getPageSize())
                                        .offset(pageRequest.getOffset()))
                                .all()
                                .collectList(),
                        r2dbcEntityTemplate.count(empty(), Job.class)
                )
                .subscribeOn(Schedulers.parallel())
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageRequest, tuple.getT2()));
    }

    public Flux<Step> findAllStepsByIdJob(Long idJob) {
        return r2dbcEntityTemplate.select(Step.class)
                .matching(query(
                        where("id_job").is(idJob)
                ))
                .all();
    }

    public Mono<Step> findStepById(Long id) {
        return r2dbcEntityTemplate.select(Step.class)
                .matching(query(
                        where("id").is(id)
                ))
                .one()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found")));
    }

    public Mono<Void> reprocess(Long jobId, Long stepId) {
        return findJobById(jobId)
                .flatMap(job -> findStepById(stepId))
                .filter(step -> step.getIdJob().equals(jobId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Step not found")))
                .flatMap(step -> {
                    if (!step.canReprocess()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Step already COMPLETED"));
                    }
                    return stackService.findResourceById(step.getIdResource())
                            .flatMap(resource -> convertAndSend(resource, step, tryParsePayload(step.getPayload())));
                });
    }

    private Job factoryJob(Stack stack) {
        return Job.builder()
                .initAt(LocalDateTime.now())
                .idStack(stack.getId())
                .build();
    }

    private Step factoryStep(Job job, Resource resource, Object payload) {
        return Step.builder()
                .uuid(UUID.randomUUID().toString())
                .payload(tryParsePayload(payload))
                .initAt(LocalDateTime.now())
                .idJob(job.getId())
                .idResource(resource.getId())
                .build();
    }

    @SneakyThrows
    private String tryParsePayload(Object object) {
        if (isNull(object)) {
            return null;
        }
        return objectMapper.writeValueAsString(object);
    }

    @SneakyThrows
    private Object tryParsePayload(String json) {
        if (isNull(json)) {
            return null;
        }
        return objectMapper.readValue(json, Object.class);
    }

    @SneakyThrows
    private byte[] writeValueAsBytes(Object object) {
        return objectMapper.writeValueAsBytes(object);
    }
}
