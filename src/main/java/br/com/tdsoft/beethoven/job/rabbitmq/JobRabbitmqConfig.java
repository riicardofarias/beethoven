package br.com.tdsoft.beethoven.job.rabbitmq;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import reactor.core.Disposable;
import reactor.rabbitmq.BindingSpecification;
import reactor.rabbitmq.ExchangeSpecification;
import reactor.rabbitmq.QueueSpecification;
import reactor.rabbitmq.Sender;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static reactor.rabbitmq.BindingSpecification.binding;
import static reactor.rabbitmq.QueueSpecification.queue;
import static reactor.rabbitmq.ResourcesSpecification.exchange;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.rabbitmq.enable")
class JobRabbitmqConfig {
    private final AppRabbitmqProps appRabbitmqProps;
    private final Sender sender;

    @PostConstruct
    Disposable init() {
        return sender.declare(beethovenExchange())
                .then(sender.declare(beethovenQueue()))
                .then(sender.declare(beethovenQueueDlx()))
                .then(sender.bind(beethovenBinding()))
                .subscribe();
    }

    private ExchangeSpecification beethovenExchange() {
        return exchange(appRabbitmqProps.getExchange())
                .durable(true)
                .type("topic");
    }

    private QueueSpecification beethovenQueue() {
        return queue(appRabbitmqProps.getQueue())
                .arguments(Map.of(
                        "x-dead-letter-exchange", "",
                        "x-dead-letter-routing-key", getBeethovenQueueDlxName()
                ))
                .durable(true);
    }

    private QueueSpecification beethovenQueueDlx() {
        return queue(getBeethovenQueueDlxName())
                .durable(true)
                .arguments(Map.of(
                        "x-dead-letter-exchange", "",
                        "x-dead-letter-routing-key", appRabbitmqProps.getQueue(),
                        "x-queue-mode", "lazy",
                        "x-message-ttl", TimeUnit.MINUTES.toMillis(appRabbitmqProps.getMinutesRetry())
                ));
    }

    private BindingSpecification beethovenBinding() {
        return binding(
                appRabbitmqProps.getExchange(),
                appRabbitmqProps.getRouting(),
                appRabbitmqProps.getQueue()
        );
    }

    private String getBeethovenQueueDlxName() {
        return appRabbitmqProps.getQueue() + ".dlx";
    }
}
