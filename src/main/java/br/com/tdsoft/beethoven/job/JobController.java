package br.com.tdsoft.beethoven.job;

import br.com.tdsoft.beethoven.common.PageQuery;
import br.com.tdsoft.beethoven.job.dto.JobRequest;
import br.com.tdsoft.beethoven.job.dto.JobResponse;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("jobs")
@RequiredArgsConstructor
@Timed("http_server_requests")
class JobController {
    private final JobService jobService;

    @Operation(summary = "Find job by id")
    @GetMapping(value = "{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<JobResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok()
                .body(jobService
                        .findJobWithStepsById(id)
                        .map(JobResponse::from));
    }

    @Operation(summary = "List all jobs")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<Page<JobResponse>>> findAll(@Valid PageQuery pageQuery) {
        return ResponseEntity.ok()
                .body(
                        jobService.findAllJobs(pageQuery)
                                .map(it -> it.map(JobResponse::from))
                );
    }

    @Operation(summary = "Save new job")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<JobResponse>> save(@Valid @RequestBody JobRequest body) {
        return ResponseEntity.ok()
                .body(jobService
                        .startNewJob(body)
                        .map(JobResponse::from));
    }

    @Operation(summary = "Reprocess step")
    @PostMapping(value = "{jobId}/steps/{stepId}/reprocess", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Mono<Void>> reprocess(@PathVariable Long jobId, @PathVariable Long stepId) {
        return new ResponseEntity<>(
                jobService.reprocess(jobId, stepId),
                HttpStatus.NO_CONTENT
        );
    }
}
