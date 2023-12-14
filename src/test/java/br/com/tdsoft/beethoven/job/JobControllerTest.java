package br.com.tdsoft.beethoven.job;

import br.com.tdsoft.beethoven.common.BaseIntegrationTest;
import br.com.tdsoft.beethoven.job.dto.JobRequest;
import br.com.tdsoft.beethoven.job.dto.JobResponse;
import br.com.tdsoft.beethoven.job.dto.JobStepReceiver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Sender;

import static org.mockito.ArgumentMatchers.any;

class JobControllerTest extends BaseIntegrationTest {
    @Autowired
    private WebTestClient client;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JobService jobService;
    @MockBean
    private Sender sender;

    @BeforeEach
    void init() {
        Mockito.when(sender.send(any()))
                .thenReturn(Mono.empty());
    }

    @Test
    void findAllTest() {
        executeScripts("/h2/stack.sql", "/h2/job.sql");
        client.get()
                .uri("/jobs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.[0].id").hasJsonPath()
                .jsonPath("$.content.[0].initAt").hasJsonPath()
                .jsonPath("$.content.[0].endAt").hasJsonPath()
                .jsonPath("$.content.[0].status").hasJsonPath();
    }

    @Test
    void findByIdTest() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql", "/h2/job.sql", "/h2/step.sql");
        client.get()
                .uri("/jobs/100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.initAt").hasJsonPath()
                .jsonPath("$.endAt").hasJsonPath()
                .jsonPath("$.status").hasJsonPath()
                .jsonPath("$.steps.size()").isEqualTo(2)
                .jsonPath("$.steps.[0].id").hasJsonPath()
                .jsonPath("$.steps.[0].uuid").hasJsonPath()
                .jsonPath("$.steps.[0].payload").hasJsonPath()
                .jsonPath("$.steps.[0].initAt").hasJsonPath()
                .jsonPath("$.steps.[0].endAt").hasJsonPath()
                .jsonPath("$.steps.[0].status").hasJsonPath()
                .jsonPath("$.steps.[0].idJob").hasJsonPath()
                .jsonPath("$.steps.[0].idResource").hasJsonPath();
    }

    /**
     * Ciclo completo de execução da pipeline
     * 1. Executa a pipiline POST /jobs
     * 2. Com a resposta da etapa 1 gera um mock de retorno do component
     * 3. Verifica se a primeira etapa foi concluida e inicia a segunda etapa GET /jobs/{id}
     * 4. Com a resposta da etapa 3 gera um mock de retorno do component
     * 5. Verifica se a pipeline foi finalizada com sucesso  GET /jobs/{id}
     */
    @Test
    @SneakyThrows
    void saveTest() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql");
        var response = client.post()
                .uri("/jobs")
                .bodyValue(getJobRequest())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.initAt").hasJsonPath()
                .jsonPath("$.endAt").isEmpty()
                .jsonPath("$.status").isEqualTo(Status.RUNNING.name())
                .jsonPath("$.steps.size()").isEqualTo(1)
                .jsonPath("$.steps.[0].id").hasJsonPath()
                .jsonPath("$.steps.[0].uuid").hasJsonPath()
                .jsonPath("$.steps.[0].payload").hasJsonPath()
                .jsonPath("$.steps.[0].initAt").hasJsonPath()
                .jsonPath("$.steps.[0].endAt").isEmpty()
                .jsonPath("$.steps.[0].status").isEqualTo(Status.RUNNING.name())
                .jsonPath("$.steps.[0].idJob").hasJsonPath()
                .jsonPath("$.steps.[0].idResource").hasJsonPath()
                .returnResult();

        var jobResponse = objectMapper.readValue(response.getResponseBody(), JobResponse.class);
        mockOrchestratorHandler(
                JobStepReceiver.builder()
                        .uuid(jobResponse.steps().get(0).uuid())
                        .build()
        );
        response = client.get()
                .uri("/jobs/" + jobResponse.id())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.initAt").hasJsonPath()
                .jsonPath("$.endAt").hasJsonPath()
                .jsonPath("$.status").isEqualTo(Status.RUNNING.name())
                .jsonPath("$.steps.size()").isEqualTo(2)
                .jsonPath("$.steps.[0].id").hasJsonPath()
                .jsonPath("$.steps.[0].uuid").hasJsonPath()
                .jsonPath("$.steps.[0].payload").hasJsonPath()
                .jsonPath("$.steps.[0].initAt").hasJsonPath()
                .jsonPath("$.steps.[0].endAt").hasJsonPath()
                .jsonPath("$.steps.[0].status").isEqualTo(Status.COMPLETED.name())
                .jsonPath("$.steps.[1].status").isEqualTo(Status.RUNNING.name())
                .jsonPath("$.steps.[0].idJob").hasJsonPath()
                .jsonPath("$.steps.[0].idResource").hasJsonPath()
                .returnResult();
        jobResponse = objectMapper.readValue(response.getResponseBody(), JobResponse.class);
        mockOrchestratorHandler(
                JobStepReceiver.builder()
                        .uuid(jobResponse.steps().get(1).uuid())
                        .build()
        );
        client.get()
                .uri("/jobs/" + jobResponse.id())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.initAt").hasJsonPath()
                .jsonPath("$.endAt").hasJsonPath()
                .jsonPath("$.status").isEqualTo(Status.COMPLETED.name())
                .jsonPath("$.steps.size()").isEqualTo(2)
                .jsonPath("$.steps.[0].id").hasJsonPath()
                .jsonPath("$.steps.[0].uuid").hasJsonPath()
                .jsonPath("$.steps.[0].payload").hasJsonPath()
                .jsonPath("$.steps.[0].initAt").hasJsonPath()
                .jsonPath("$.steps.[0].endAt").hasJsonPath()
                .jsonPath("$.steps.[0].status").isEqualTo(Status.COMPLETED.name())
                .jsonPath("$.steps.[1].status").isEqualTo(Status.COMPLETED.name())
                .jsonPath("$.steps.[0].idJob").hasJsonPath()
                .jsonPath("$.steps.[0].idResource").hasJsonPath();
    }

    @Test
    @SneakyThrows
    void saveTest_failedInFirstStep() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql");
        var response = client.post()
                .uri("/jobs")
                .bodyValue(getJobRequest())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.initAt").hasJsonPath()
                .jsonPath("$.endAt").isEmpty()
                .jsonPath("$.status").isEqualTo(Status.RUNNING.name())
                .jsonPath("$.steps.size()").isEqualTo(1)
                .jsonPath("$.steps.[0].id").hasJsonPath()
                .jsonPath("$.steps.[0].uuid").hasJsonPath()
                .jsonPath("$.steps.[0].payload").hasJsonPath()
                .jsonPath("$.steps.[0].initAt").hasJsonPath()
                .jsonPath("$.steps.[0].endAt").isEmpty()
                .jsonPath("$.steps.[0].status").isEqualTo(Status.RUNNING.name())
                .jsonPath("$.steps.[0].idJob").hasJsonPath()
                .jsonPath("$.steps.[0].idResource").hasJsonPath()
                .returnResult();

        var errorTracer = ExceptionUtils.getStackTrace(new RuntimeException("Simulated exception"));
        var jobResponse = objectMapper.readValue(response.getResponseBody(), JobResponse.class);
        mockOrchestratorHandler(
                JobStepReceiver.builder()
                        .success(false)
                        .errorTracer(errorTracer)
                        .uuid(jobResponse.steps().get(0).uuid())
                        .build()
        );
        client.get()
                .uri("/jobs/" + jobResponse.id())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.initAt").hasJsonPath()
                .jsonPath("$.endAt").hasJsonPath()
                .jsonPath("$.status").isEqualTo(Status.FAILED.name())
                .jsonPath("$.steps.size()").isEqualTo(1)
                .jsonPath("$.steps.[0].id").hasJsonPath()
                .jsonPath("$.steps.[0].uuid").hasJsonPath()
                .jsonPath("$.steps.[0].payload").hasJsonPath()
                .jsonPath("$.steps.[0].errorTracer").isEqualTo(errorTracer)
                .jsonPath("$.steps.[0].initAt").hasJsonPath()
                .jsonPath("$.steps.[0].endAt").hasJsonPath()
                .jsonPath("$.steps.[0].status").isEqualTo(Status.FAILED.name())
                .jsonPath("$.steps.[0].idJob").hasJsonPath()
                .jsonPath("$.steps.[0].idResource").hasJsonPath()
                .returnResult();
    }

    /**
     * Deve retornar BadRequest pois a step já foi completada com sucesso
     */
    @Test
    void reprocessTest_ShouldReturnBadRequestBecauseCompleteStatus() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql", "/h2/job.sql", "/h2/step.sql");
        client.post()
                .uri("/jobs/100/steps/100/reprocess")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void reprocessTest_ShouldReturnOkBecauseRunningStatus() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql", "/h2/job.sql", "/h2/step.sql");
        client.post()
                .uri("/jobs/101/steps/103/reprocess")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void reprocessTest_ShouldReturnOkBecauseFailedStatus() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql", "/h2/job.sql", "/h2/step.sql");
        client.post()
                .uri("/jobs/102/steps/104/reprocess")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
    }

    private void mockOrchestratorHandler(JobStepReceiver jobStepReceiver) {
        jobService.jobPipeline(jobStepReceiver).block();
    }

    private static JobRequest getJobRequest() {
        return JobRequest.builder()
                .identifier("h2")
                .build();
    }
}
