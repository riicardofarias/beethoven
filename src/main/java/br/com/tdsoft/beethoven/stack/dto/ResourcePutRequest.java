package br.com.tdsoft.beethoven.stack.dto;

import br.com.tdsoft.beethoven.stack.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.hibernate.validator.constraints.Length;

@Builder
@Jacksonized
public record ResourcePutRequest(
        Long id,
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

    public Resource apply(Resource resource) {
        resource.setName(name);
        resource.setResourceOrder(resourceOrder);
        resource.setRouting(routing);
        return resource;
    }
}
