package br.com.tdsoft.beethoven.stack;

import br.com.tdsoft.beethoven.common.BaseIntegrationTest;
import br.com.tdsoft.beethoven.stack.dto.ResourcePutRequest;
import br.com.tdsoft.beethoven.stack.dto.ResourceRequest;
import br.com.tdsoft.beethoven.stack.dto.StackPutRequest;
import br.com.tdsoft.beethoven.stack.dto.StackRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.rabbitmq.Sender;

import java.util.List;
import java.util.UUID;

class StackControllerTest extends BaseIntegrationTest {
    @Autowired
    private WebTestClient client;
    @MockBean
    private Sender sender;

    @Test
    void findAllTest() {
        executeScript("/h2/stack.sql");
        client.get()
                .uri("/stacks")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.[0].id").hasJsonPath()
                .jsonPath("$.content.[0].externalId").hasJsonPath()
                .jsonPath("$.content.[0].name").hasJsonPath()
                .jsonPath("$.content.[0].description").hasJsonPath();
    }

    @Test
    void findByIdTest() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql");
        client.get()
                .uri("/stacks/100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.externalId").hasJsonPath()
                .jsonPath("$.name").hasJsonPath()
                .jsonPath("$.description").hasJsonPath()
                .jsonPath("$.resources.size()").isEqualTo(2)
                .jsonPath("$.resources.[0].id").hasJsonPath()
                .jsonPath("$.resources.[0].name").hasJsonPath()
                .jsonPath("$.resources.[0].resourceOrder").hasJsonPath()
                .jsonPath("$.resources.[0].routing").hasJsonPath()
                .jsonPath("$.resources.[0].idStack").hasJsonPath();
    }

    @Test
    void saveTest() {
        client.post()
                .uri("/stacks")
                .bodyValue(getStackRequest())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.externalId").hasJsonPath()
                .jsonPath("$.name").hasJsonPath()
                .jsonPath("$.description").hasJsonPath()
                .jsonPath("$.resources.size()").isEqualTo(1)
                .jsonPath("$.resources.[0].id").hasJsonPath()
                .jsonPath("$.resources.[0].name").hasJsonPath()
                .jsonPath("$.resources.[0].resourceOrder").hasJsonPath()
                .jsonPath("$.resources.[0].routing").hasJsonPath()
                .jsonPath("$.resources.[0].idStack").hasJsonPath();
    }

    @Test
    void saveTest_ShouldReturnBadRequest() {
        executeScript("/h2/stack.sql");
        client.post()
                .uri("/stacks")
                .bodyValue(getStackRequest().withExternalId("h2"))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateTest() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql");
        var stackPutRequest = getStackPutRequest();
        client.put()
                .uri("/stacks")
                .bodyValue(stackPutRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.externalId").hasJsonPath()
                .jsonPath("$.name").isEqualTo(stackPutRequest.name())
                .jsonPath("$.description").isEqualTo(stackPutRequest.description())
                .jsonPath("$.resources.size()").isEqualTo(2)
                .jsonPath("$.resources.[0].id").hasJsonPath()
                .jsonPath("$.resources.[0].name").isEqualTo(stackPutRequest.resources().get(0).name())
                .jsonPath("$.resources.[0].resourceOrder").isEqualTo(stackPutRequest.resources().get(0).resourceOrder())
                .jsonPath("$.resources.[0].routing").isEqualTo(stackPutRequest.resources().get(0).routing())
                .jsonPath("$.resources.[1].id").hasJsonPath()
                .jsonPath("$.resources.[1].name").isEqualTo(stackPutRequest.resources().get(1).name())
                .jsonPath("$.resources.[1].resourceOrder").isEqualTo(stackPutRequest.resources().get(1).resourceOrder())
                .jsonPath("$.resources.[1].routing").isEqualTo(stackPutRequest.resources().get(1).routing());
    }

    /**
     * Esse teste edita o id passando o mesmo external_id
     */
    @Test
    void updateTest_same_externalId() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql");
        var stackPutRequest = getStackPutRequest()
                .withExternalId("h2");
        client.put()
                .uri("/stacks")
                .bodyValue(stackPutRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").hasJsonPath()
                .jsonPath("$.externalId").hasJsonPath()
                .jsonPath("$.name").isEqualTo(stackPutRequest.name())
                .jsonPath("$.description").isEqualTo(stackPutRequest.description())
                .jsonPath("$.resources.size()").isEqualTo(2)
                .jsonPath("$.resources.[0].id").hasJsonPath()
                .jsonPath("$.resources.[0].name").isEqualTo(stackPutRequest.resources().get(0).name())
                .jsonPath("$.resources.[0].resourceOrder").isEqualTo(stackPutRequest.resources().get(0).resourceOrder())
                .jsonPath("$.resources.[0].routing").isEqualTo(stackPutRequest.resources().get(0).routing())
                .jsonPath("$.resources.[1].id").hasJsonPath()
                .jsonPath("$.resources.[1].name").isEqualTo(stackPutRequest.resources().get(1).name())
                .jsonPath("$.resources.[1].resourceOrder").isEqualTo(stackPutRequest.resources().get(1).resourceOrder())
                .jsonPath("$.resources.[1].routing").isEqualTo(stackPutRequest.resources().get(1).routing());
    }

    @Test
    void updateTest_ShouldReturnBadRequest() {
        executeScripts("/h2/stack.sql");
        client.put()
                .uri("/stacks")
                .bodyValue(
                        getStackPutRequest()
                                .withId(101L)
                                .withExternalId("h2")
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void deleteStackTest() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql");
        client.delete()
                .uri("/stacks/100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        client.get()
                .uri("/stacks/100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteResourceTest() {
        executeScripts("/h2/stack.sql", "/h2/resource.sql");
        client.delete()
                .uri("/stacks/100/resources/100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNoContent();
        client.get()
                .uri("/stacks/100")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.resources.size()").isEqualTo(1)
                .jsonPath("$.resources.[0].id").isEqualTo(101L);
    }

    private StackRequest getStackRequest() {
        return StackRequest.builder()
                .externalId("integration_mock_test")
                .name("Integration test")
                .description("Integration test with h2")
                .resources(List.of(
                        ResourceRequest.builder()
                                .name("Resource 1")
                                .resourceOrder(1)
                                .routing("h2")
                                .build()
                ))
                .build();
    }

    private StackPutRequest getStackPutRequest() {
        return StackPutRequest.builder()
                .id(100L)
                .externalId(UUID.randomUUID().toString())
                .name(UUID.randomUUID().toString())
                .description(UUID.randomUUID().toString())
                .resources(List.of(
                        ResourcePutRequest.builder()
                                .id(100L)
                                .name(UUID.randomUUID().toString())
                                .resourceOrder(4)
                                .routing(UUID.randomUUID().toString())
                                .build(),
                        ResourcePutRequest.builder()
                                .id(101L)
                                .name(UUID.randomUUID().toString())
                                .resourceOrder(5)
                                .routing(UUID.randomUUID().toString())
                                .build()
                ))
                .build();
    }
}
