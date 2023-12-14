package br.com.tdsoft.beethoven.job;

import br.com.tdsoft.beethoven.common.Entity;
import br.com.tdsoft.beethoven.stack.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@Table("step")
public class Step implements Entity {
    @PersistenceCreator
    public Step(Long id,
                String uuid,
                String payload,
                String errorTracer,
                LocalDateTime initAt,
                LocalDateTime endAt,
                Status status,
                Long idJob,
                Long idResource) {
        this.id = id;
        this.uuid = uuid;
        this.payload = payload;
        this.errorTracer = errorTracer;
        this.initAt = initAt;
        this.endAt = endAt;
        this.status = status;
        this.idJob = idJob;
        this.idResource = idResource;
    }

    @Id
    private Long id;
    private String uuid;
    private String payload;
    private String errorTracer;
    private LocalDateTime initAt;
    private LocalDateTime endAt;
    @Builder.Default
    private Status status = Status.RUNNING;
    private Long idJob;
    private Long idResource;
    @Transient
    private Job job;
    @Transient
    private Resource resource;

    public boolean isFinalized() {
        return status.equals(Status.COMPLETED);
    }

    public boolean canReprocess() {
        return !status.equals(Status.COMPLETED);
    }
}
