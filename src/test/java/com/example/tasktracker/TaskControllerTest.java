package com.example.tasktracker;

import com.example.tasktracker.dto.TaskRequest;
import com.example.tasktracker.dto.TaskResponse;
import com.example.tasktracker.entity.Task;
import com.example.tasktracker.entity.User;
import com.example.tasktracker.enumz.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TaskControllerTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDatabase() {
        mongoTemplate.dropCollection("tasks").block();
        mongoTemplate.dropCollection("users").block();
    }

    @Test
    @DisplayName("GET /task -- USER can see all tasks")
    @WithMockUser(roles = "USER")
    void getAllTasks_userRole() {
        Task task = new Task();
        task.setName("Test task");
        task.setDescription("Test desc");
        task.setAuthor(new User("1", "name", "mail@mail.com", "pass", Set.of(RoleType.ROLE_MANAGER)));

        mongoTemplate.save(task).block();

        webTestClient.get().uri("/task")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(TaskResponse.class)
                .hasSize(1);
    }

    @Test
    @DisplayName("POST /task -- MANAGER create task")
    void createTask_managerRole() {
        TaskRequest request = new TaskRequest();
        request.setName("New Task");
        request.setDescription("New Desc");

        webTestClient.post().uri("/task")
                .headers(headers -> headers.setBasicAuth("manager", "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TaskResponse.class)
                .value(response -> {
                    assertEquals("New Task", response.getName());
                    assertEquals("New Desc", response.getDescription());
                    assertNotNull(response.getId());
                });
    }

    @Test
    @DisplayName("POST /task -- USER 403")
    void createTask_userRole_forbidden() {
        TaskRequest request = new TaskRequest();
        request.setName("New Task");
        request.setDescription("New Desc");

        webTestClient.post().uri("/task")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("GET /task -- no authenticated 401")
    void getTask_noAuth_unauthorized() {
        webTestClient.get().uri("/task")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("DELETE /task/{id} -- delete by MANAGER")
    void deleteTask_managerRole() {
        Task task = new Task();
        task.setName("Test task");
        task.setDescription("Test desc");
        task.setAuthor(new User("1", "name", "mail@mail.com", "pass", Set.of(RoleType.ROLE_MANAGER)));

        mongoTemplate.save(task).block();

        webTestClient.delete().uri("/task/{id}", task.getId())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get().uri("/task")
                .exchange()
                .expectBodyList(TaskResponse.class)
                .hasSize(0);
    }

    @Test
    @DisplayName("DELETE /task/{id} -- delete by USER 403")
    void deleteTask_userRole_forbidden() {
        webTestClient.delete().uri("/task/1")
                .exchange()
                .expectStatus().isForbidden();
    }
}
