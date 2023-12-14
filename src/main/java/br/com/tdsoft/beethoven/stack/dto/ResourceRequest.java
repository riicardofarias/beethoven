package br.com.tdsoft.beethoven.stack.dto;

import br.com.tdsoft.beethoven.stack.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.hibernate.validator.constraints.Length;

@Builder
@Jacksonized
public record ResourceRequest(
        @NotEmpty
        @Length(max = 255)
        String name,
        @NotNull
        Integer resourceOrder,
        @NotEmpty
        @Length(max = 255)
        String routing
) {
    public Resource toResource() {
        return Resource.builder()
                .name(name)
                .resourceOrder(resourceOrder)
                .routing(routing)
                .build();
    }
}
