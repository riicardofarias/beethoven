package br.com.tdsoft.beethoven.job.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class JobStepReceiver {
    String uuid;
    Object payload;
    String errorTracer;
    @Builder.Default
    boolean success = true;
}
