package br.com.tdsoft.beethoven.job;

import br.com.tdsoft.beethoven.common.Entity;
import br.com.tdsoft.beethoven.stack.Stack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@Table("job")
public class Job implements Entity {
    @PersistenceCreator
    public Job(Long id, LocalDateTime initAt, LocalDateTime endAt, Status status, Long idStack) {
        this.id = id;
        this.initAt = initAt;
        this.endAt = endAt;
        this.status = status;
        this.idStack = idStack;
    }

    @Id
    private Long id;
    private LocalDateTime initAt;
    private LocalDateTime endAt;
    @Builder.Default
    private Status status = Status.RUNNING;
    private Long idStack;
    @Transient
    private Stack stack;
    @Transient
    private List<Step> steps;
}
