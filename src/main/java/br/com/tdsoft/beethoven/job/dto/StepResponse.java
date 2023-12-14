package br.com.tdsoft.beethoven.job.dto;

import br.com.tdsoft.beethoven.common.JacksonConfig;
import br.com.tdsoft.beethoven.job.Status;
import br.com.tdsoft.beethoven.job.Step;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

import static java.util.Objects.isNull;

@Builder
@Jacksonized
public record StepResponse(
        Long id,
        String uuid,
        Object payload,
        String errorTracer,
        LocalDateTime initAt,
        LocalDateTime endAt,
        Status status,
        Long idJob,
        Long idResource
) {
    public static StepResponse from(Step step) {
        return StepResponse.builder()
                .id(step.getId())
                .uuid(step.getUuid())
                .payload(getPayload(step.getPayload()))
                .errorTracer(step.getErrorTracer())
                .initAt(step.getInitAt())
                .endAt(step.getEndAt())
                .status(step.getStatus())
                .idJob(step.getIdJob())
                .idResource(step.getIdResource())
                .build();
    }

    @SneakyThrows
    private static Object getPayload(String json) {
        if (isNull(json)) {
            return null;
        }
        return JacksonConfig.getObjectMapper()
                .readValue(json, Object.class);
    }
}
