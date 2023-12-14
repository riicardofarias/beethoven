package br.com.tdsoft.beethoven.job.rabbitmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("app.rabbitmq")
public class AppRabbitmqProps {
    private String exchange;
    private String queue;
    private String routing;
    private Integer minutesRetry;
    private Integer maxAttempt;
}
