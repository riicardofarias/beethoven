package br.com.tdsoft.beethoven.job.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record JobRequest(
        @NotNull
        String identifier,
        Object payload
) {
}
