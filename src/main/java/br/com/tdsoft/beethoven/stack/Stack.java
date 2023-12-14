package br.com.tdsoft.beethoven.stack;

import br.com.tdsoft.beethoven.common.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

@Data
@Builder
@AllArgsConstructor
@Table("stack")
public class Stack implements Entity {
    @PersistenceCreator
    public Stack(Long id, String name, String externalId, String description, boolean deleted) {
        this.id = id;
        this.externalId = externalId;
        this.name = name;
        this.description = description;
        this.deleted = deleted;
    }

    @Id
    private Long id;
    private String externalId;
    private String name;
    private String description;
    @Builder.Default
    private boolean deleted = false;
    @Transient
    private List<Resource> resources;

    public boolean hasSources() {
        return nonNull(resources) && !resources.isEmpty();
    }

    public Optional<Resource> getNextResource(Resource resource) {
        return getNextResources(resource)
                .findFirst();
    }

    public Stream<Resource> getNextResources(Resource resource) {
        if (!hasSources()) {
            return Stream.empty();
        }
        var index = resources.indexOf(resource);
        if (index < 0) {
            throw new RuntimeException("Resource is not present in stack");
        }
        return resources.stream()
                .skip((long) index + 1);
    }
}
