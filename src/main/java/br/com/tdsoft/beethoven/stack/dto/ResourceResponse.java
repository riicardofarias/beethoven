package br.com.tdsoft.beethoven.stack.dto;

import br.com.tdsoft.beethoven.stack.Resource;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record ResourceResponse(
        Long id,
        String name,
        Integer resourceOrder,
        String routing,
        Long idStack
) {
    public static ResourceResponse from(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .resourceOrder(resource.getResourceOrder())
                .routing(resource.getRouting())
                .idStack(resource.getIdStack())
                .build();
    }
}
