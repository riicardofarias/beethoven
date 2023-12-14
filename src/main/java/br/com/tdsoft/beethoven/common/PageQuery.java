package br.com.tdsoft.beethoven.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;

import static jakarta.validation.constraints.Pattern.Flag.CASE_INSENSITIVE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

@Builder
@Jacksonized
public record PageQuery(
        @Min(1)
        Integer page,
        @Min(1)
        @Max(50)
        Integer size,
        @Pattern(regexp = "^asc$|^desc$", flags = CASE_INSENSITIVE)
        String direction,
        List<String> orderBy) {

    public PageRequest toPageRequest(String... allowedSortProperties) {
        if (isNull(orderBy) || isNull(allowedSortProperties)) {
            return unsorted();
        }
        var props = orderBy.stream()
                .filter(it -> Arrays.asList(allowedSortProperties).contains(it))
                .toArray(String[]::new);
        if (props.length == 0) {
            return unsorted();
        }
        return PageRequest.of(page() - 1, size(), Sort.by(getDirection(), props));
    }

    private PageRequest unsorted() {
        return PageRequest.of(page() - 1, size());
    }

    private Sort.Direction getDirection() {
        return ofNullable(direction)
                .flatMap(Sort.Direction::fromOptionalString)
                .orElse(Sort.Direction.ASC);
    }

    @Override
    public Integer page() {
        return nonNull(page) ? page : 1;
    }

    @Override
    public Integer size() {
        return nonNull(size) ? size : 50;
    }
}