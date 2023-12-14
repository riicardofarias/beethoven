package br.com.tdsoft.beethoven.stack.dto;

import br.com.tdsoft.beethoven.stack.Stack;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.With;
import lombok.extern.jackson.Jacksonized;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@With
@Builder
@Jacksonized
public record StackPutRequest(
        @NotNull
        Long id,
        String externalId,
        @NotEmpty
        @Length(max = 255)
        String name,
        @Length(max = 255)
        String description,
        @Valid
        List<ResourcePutRequest> resources
) {
    public Stack apply(Stack stack) {
        stack.setName(name);
        stack.setExternalId(externalId);
        stack.setDescription(description);
        return stack;
    }
}
