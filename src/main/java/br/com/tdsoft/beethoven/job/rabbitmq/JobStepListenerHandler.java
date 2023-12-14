package br.com.tdsoft.beethoven.job.rabbitmq;

import br.com.tdsoft.beethoven.common.MaxAttemptException;
import br.com.tdsoft.beethoven.job.JobService;
import br.com.tdsoft.beethoven.job.dto.JobStepReceiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.AcknowledgableDelivery;
import reactor.rabbitmq.ConsumeOptions;
import reactor.rabbitmq.ExceptionHandlers;
import reactor.rabbitmq.Receiver;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.enable")
class JobStepListenerHandler {
    private final AppRabbitmqProps appRabbitmqProps;
    private final Receiver receiver;
    private final JobService jobService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    Disposable beethovenHandlerQueueListener() {
        return receiver.consumeManualAck(appRabbitmqProps.getQueue(), consumeOptions())
                .retryWhen(Retry.backoff(6, Duration.ofSeconds(5)))
                .flatMap(this::consumerHandler)
                .subscribe();
    }

    private Mono<?> consumerHandler(AcknowledgableDelivery ad) {
        return reprocessStrategy(ad)
                .then(jobPipeline(ad))
                .doOnSuccess(it -> ad.ack())
                .onErrorResume(MaxAttemptException.class, throwable -> {
                    log.error("MaxAttemptException", throwable);
                    return Mono.fromRunnable(ad::ack);
                })
                .onErrorResume(throwable -> {
                    log.error("Exception", throwable);
                    return Mono.fromRunnable(() -> ad.nack(false));
                });
    }

    private Mono<Void> jobPipeline(AcknowledgableDelivery ad) {
        return Mono.fromSupplier(() -> readValue(ad.getBody()))
                .doOnNext(jobStepReceiver -> log.info("Process new step: {}", jobStepReceiver.getUuid()))
                .flatMap(jobService::jobPipeline);
    }

    private Mono<Void> reprocessStrategy(AcknowledgableDelivery ad) {
        return Mono.fromSupplier(() ->
                        ofNullable(ad.getProperties().getHeaders())
                                .flatMap(it -> ofNullable(it.get("x-death")))
                                .flatMap(headers -> ((List<Map<String, Object>>) headers).stream().findFirst())
                                .map(headers -> (Long) headers.get("count"))
                                .orElse(null)
                )
                .doOnNext(xDeathCount -> log.info("xDeathCount: {}", xDeathCount))
                .filter(xDeathCount -> xDeathCount >= appRabbitmqProps.getMaxAttempt())
                .flatMap(xDeathCount -> Mono.error(new MaxAttemptException(xDeathCount.intValue())))
                .then();
    }

    @SneakyThrows
    private JobStepReceiver readValue(byte[] body) {
        return objectMapper.readValue(body, JobStepReceiver.class);
    }

    private ConsumeOptions consumeOptions() {
        return new ConsumeOptions().exceptionHandler(
                new ExceptionHandlers.RetryAcknowledgmentExceptionHandler(
                        Duration.ofSeconds(20),
                        Duration.ofMillis(500),
                        ExceptionHandlers.CONNECTION_RECOVERY_PREDICATE
                )
        ).qos(1);
    }
}
