package br.com.tdsoft.beethoven.stack.dto;

import br.com.tdsoft.beethoven.stack.Resource;
import br.com.tdsoft.beethoven.stack.Stack;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static java.util.Objects.isNull;

@Builder
@Jacksonized
public record StackResponse(
        Long id,
        String externalId,
        String name,
        String description,
        List<ResourceResponse> resources
) {
    public static StackResponse from(Stack stack) {
        return StackResponse.builder()
                .id(stack.getId())
                .externalId(stack.getExternalId())
                .name(stack.getName())
                .description(stack.getDescription())
                .resources(getResources(stack.getResources()))
                .build();
    }

    private static List<ResourceResponse> getResources(List<Resource> resources) {
        if (isNull(resources)) {
            return List.of();
        }
        return resources.stream()
                .map(ResourceResponse::from)
                .toList();
    }
}
