package br.com.tdsoft.beethoven.job.dto;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record JobStepSender(
        String uuid,
        Object payload,
        JobStepMetaData metaData
) {
}
