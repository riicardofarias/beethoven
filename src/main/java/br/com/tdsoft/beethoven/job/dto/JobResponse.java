package br.com.tdsoft.beethoven.job.dto;

import br.com.tdsoft.beethoven.job.Job;
import br.com.tdsoft.beethoven.job.Status;
import br.com.tdsoft.beethoven.job.Step;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Objects.isNull;

@Builder
@Jacksonized
public record JobResponse(
        Long id,
        LocalDateTime initAt,
        LocalDateTime endAt,
        Status status,
        List<StepResponse> steps
) {
    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .initAt(job.getInitAt())
                .endAt(job.getEndAt())
                .status(job.getStatus())
                .steps(getSteps(job.getSteps()))
                .build();
    }

    private static List<StepResponse> getSteps(List<Step> steps) {
        if (isNull(steps)) {
            return List.of();
        }
        return steps.stream()
                .map(StepResponse::from)
                .toList();
    }
}
