package br.com.tdsoft.beethoven.common;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.*;
import reactor.util.retry.Retry;

@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.enable")
class RabbitMqConfig {
    @Bean
    ConnectionFactory reactiveConnectionFactory(RabbitProperties rabbitProperties) {
        var connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(rabbitProperties.getHost());
        connectionFactory.setPort(rabbitProperties.getPort());
        connectionFactory.setUsername(rabbitProperties.getUsername());
        connectionFactory.setPassword(rabbitProperties.getPassword());
        connectionFactory.setVirtualHost(rabbitProperties.getVirtualHost());
        connectionFactory.useNio();
        return connectionFactory;
    }

    @Bean
    Receiver receiver(ConnectionFactory reactiveConnectionFactory) {
        var options = new ReceiverOptions()
                .connectionFactory(reactiveConnectionFactory)
                .connectionSupplier(ConnectionFactory::newConnection)
                .connectionMonoConfigurator(cm -> cm.retryWhen(Retry.indefinitely()))
                .connectionSubscriptionScheduler(Schedulers.boundedElastic());
        return RabbitFlux.createReceiver(options);
    }

    @Bean
    Sender sender(ConnectionFactory reactiveConnectionFactory) {
        var senderOptions = new SenderOptions()
                .connectionFactory(reactiveConnectionFactory)
                .connectionSupplier(ConnectionFactory::newConnection)
                .connectionMonoConfigurator(Mono::retry)
                .connectionSubscriptionScheduler(Schedulers.boundedElastic());
        return RabbitFlux.createSender(senderOptions);
    }
}
