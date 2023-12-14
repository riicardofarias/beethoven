package br.com.tdsoft.beethoven.common;

import static java.util.Objects.nonNull;

public interface Entity {
    Long getId();

    default boolean hasId() {
        return nonNull(getId());
    }
}
