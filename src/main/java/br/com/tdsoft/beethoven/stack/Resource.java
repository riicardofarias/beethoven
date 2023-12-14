package br.com.tdsoft.beethoven.stack;

import br.com.tdsoft.beethoven.common.Entity;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@Table("resource")
public class Resource implements Entity {
    @Id
    private Long id;
    private String name;
    private Integer resourceOrder;
    private String routing;
    @Builder.Default
    private boolean deleted = false;
    private Long idStack;
}
