package br.com.tdsoft.beethoven.stack;

import br.com.tdsoft.beethoven.common.PageQuery;
import br.com.tdsoft.beethoven.stack.dto.StackPutRequest;
import br.com.tdsoft.beethoven.stack.dto.StackRequest;
import br.com.tdsoft.beethoven.stack.dto.StackResponse;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("stacks")
@RequiredArgsConstructor
@Timed("http_server_requests")
class StackController {
    private final StackService stackService;

    @Operation(summary = "Find stack by id")
    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<StackResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok()
                .body(stackService
                        .findWithResourcesById(id)
                        .map(StackResponse::from));
    }

    @Operation(summary = "List all stacks")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<Page<StackResponse>>> findAll(@Valid PageQuery pageQuery) {
        return ResponseEntity.ok()
                .body(
                        stackService.findAllStacks(pageQuery)
                                .map(it -> it.map(StackResponse::from))
                );
    }

    @Operation(summary = "Save new stack")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<StackResponse>> save(@Valid @RequestBody StackRequest body) {
        return ResponseEntity.ok()
                .body(stackService
                        .save(body)
                        .map(StackResponse::from));
    }

    @Operation(summary = "Update stack")
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<StackResponse>> update(@Valid @RequestBody StackPutRequest body) {
        return ResponseEntity.ok()
                .body(stackService
                        .update(body)
                        .map(StackResponse::from));
    }

    @Operation(summary = "Delete stack by id")
    @DeleteMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<Void>> deleteStack(@PathVariable Long id) {
        return new ResponseEntity<>(
                stackService.deleteStack(id),
                HttpStatus.NO_CONTENT
        );
    }

    @Operation(summary = "Delete resource by id")
    @DeleteMapping(value = "{stackId}/resources/{resourceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<Void>> deleteResource(@PathVariable Long stackId, @PathVariable Long resourceId) {
        return new ResponseEntity<>(
                stackService.deleteResource(stackId, resourceId),
                HttpStatus.NO_CONTENT
        );
    }
}
