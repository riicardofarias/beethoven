package br.com.tdsoft.beethoven.job.dto;

import br.com.tdsoft.beethoven.job.rabbitmq.AppRabbitmqProps;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record JobStepMetaData(
        String exchange,
        String routing
) {
    public static JobStepMetaData from(AppRabbitmqProps appRabbitmqProps) {
        return new JobStepMetaData(
                appRabbitmqProps.getExchange(),
                appRabbitmqProps.getRouting()
        );
    }
}
