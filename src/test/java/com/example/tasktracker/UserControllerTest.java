/*
package com.example.tasktracker;

import com.example.tasktracker.config.SecurityConfig;
import com.example.tasktracker.controller.UserController;
import com.example.tasktracker.dto.UserResponse;
import com.example.tasktracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import static reactor.core.publisher.Mono.when;

@WebFluxTest(UserController.class)
@Import(TestSecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "USER")
    void getAllUsers_whenUserRole_thenOk() {
        when(userService.findAll())
                .thenReturn(Flux.just(new UserResponse()));

        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAllUsers_whenManagerRole_thenOk() {
        when(userService.findAll())
                .thenReturn(Flux.just(new UserResponse()));

        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getAllUsers_whenNonAuth_thenUnauthorized() {
        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(roles = {"USER", "MANAGER"})
    void getAllUsers_whenWrongRoles_thenForbidden() {
        webTestClient.get()
                .uri("/users")
                .exchange()
                .expectStatus().isForbidden();
    }
}
*/
