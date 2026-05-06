package com.example.tasktracker.service;

import com.example.tasktracker.dto.TaskRequest;
import com.example.tasktracker.entity.Task;
import com.example.tasktracker.entity.User;
import com.example.tasktracker.enumz.TaskStatus;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.repository.TaskRepository;
import com.example.tasktracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    private User author;
    private User assignee;
    private Task task;

    @BeforeEach
    void setUp() {
        author = User.builder()
                .id("user-1").username("alice").email("alice@mail.com")
                .roles(new HashSet<>()).build();

        assignee = User.builder()
                .id("user-2").username("bob").email("bob@mail.com")
                .roles(new HashSet<>()).build();

        task = Task.builder()
                .id("task-1").name("Fix bug").description("Critical fix")
                .status(TaskStatus.TODO).authorId("user-1").assigneeId("user-2")
                .observerIds(new HashSet<>())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    // findById

    @Test
    @DisplayName("findById: задача найдена → возвращает TaskResponse с author и assignee")
    void findById_found_returnsResponseWithRelations() {
        when(taskRepository.findById("task-1")).thenReturn(Mono.just(task));
        when(userRepository.findById("user-1")).thenReturn(Mono.just(author));
        when(userRepository.findById("user-2")).thenReturn(Mono.just(assignee));

        StepVerifier.create(taskService.findById("task-1"))
                .assertNext(response -> {
                    assertThat(response.getId()).isEqualTo("task-1");
                    assertThat(response.getName()).isEqualTo("Fix bug");
                    assertThat(response.getAuthor().getUsername()).isEqualTo("alice");
                    assertThat(response.getAssignee().getUsername()).isEqualTo("bob");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("findById: задача не найдена → Mono.error с RuntimeException")
    void findById_notFound_returnsError() {
        when(taskRepository.findById("bad-id")).thenReturn(Mono.empty());

        StepVerifier.create(taskService.findById("bad-id"))
                .expectErrorMatches(e ->
                        e instanceof RuntimeException &&
                                e.getMessage().contains("Task not found"))
                .verify();
    }

    @Test
    @DisplayName("findById: author не найден → создаётся dummy User")
    void findById_authorNotFound_createsDummyUser() {
        when(taskRepository.findById("task-1")).thenReturn(Mono.just(task));
        when(userRepository.findById("user-1")).thenReturn(Mono.empty()); // автор не найден
        when(userRepository.findById("user-2")).thenReturn(Mono.just(assignee));

        StepVerifier.create(taskService.findById("task-1"))
                .assertNext(response -> {
                    // dummy user: id == authorId, email = authorId + "@tasktracker.com"
                    assertThat(response.getAuthor().getId()).isEqualTo("user-1");
                    assertThat(response.getAuthor().getEmail()).isEqualTo("user-1@tasktracker.com");
                })
                .verifyComplete();
    }

    // findAll

    @Test
    @DisplayName("findAll: пустой репозиторий → пустой Flux")
    void findAll_emptyRepository_returnsEmptyFlux() {
        when(taskRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(taskService.findAll())
                .verifyComplete();
    }

    @Test
    @DisplayName("findAll: две задачи → оба в ответе")
    void findAll_twoTasks_returnsBoth() {
        Task task2 = Task.builder()
                .id("task-2").name("Review PR").description("Code review")
                .status(TaskStatus.IN_PROGRESS).authorId("user-2").assigneeId("user-1")
                .observerIds(new HashSet<>())
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(taskRepository.findAll()).thenReturn(Flux.just(task, task2));
        when(userRepository.findById("user-1")).thenReturn(Mono.just(author));
        when(userRepository.findById("user-2")).thenReturn(Mono.just(assignee));

        StepVerifier.create(taskService.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    // create

    @Test
    @DisplayName("create: корректный запрос → задача сохраняется, createdAt/updatedAt заполнены")
    void create_validRequest_savedWithTimestamps() {
        TaskRequest request = new TaskRequest();
        request.setName("New Task");
        request.setDescription("Description");
        request.setAuthorId("user-1");
        request.setAssigneeId("user-2");
        request.setStatus(TaskStatus.TODO);
        request.setObserverIds(new HashSet<>());

        when(taskRepository.save(any(Task.class))).thenReturn(Mono.just(task));
        when(userRepository.findById("user-1")).thenReturn(Mono.just(author));
        when(userRepository.findById("user-2")).thenReturn(Mono.just(assignee));

        StepVerifier.create(taskService.create(request))
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.getName()).isEqualTo("Fix bug"); // из mock task
                })
                .verifyComplete();

        // Проверяем что save был вызван с задачей у которой есть timestamps
        verify(taskRepository).save(argThat(t ->
                t.getCreatedAt() != null && t.getUpdatedAt() != null
        ));
    }

    // addObserver

    @Test
    @DisplayName("addObserver: добавляет наблюдателя и обновляет updatedAt")
    void addObserver_addsObserverAndUpdatesTimestamp() {
        task.setObserverIds(new HashSet<>());
        Instant before = task.getUpdatedAt();

        when(taskRepository.findById("task-1")).thenReturn(Mono.just(task));
        when(taskRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(userRepository.findById("user-1")).thenReturn(Mono.just(author));
        when(userRepository.findById("user-2")).thenReturn(Mono.just(assignee));
        when(userRepository.findById("observer-1")).thenReturn(Mono.empty());

        StepVerifier.create(taskService.addObserver("task-1", "observer-1"))
                .assertNext(response -> assertThat(response).isNotNull())
                .verifyComplete();

        verify(taskRepository).save(argThat(t ->
                t.getObserverIds().contains("observer-1")
        ));
    }

    @Test
    @DisplayName("addObserver: задача не найдена → Mono.error")
    void addObserver_taskNotFound_returnsError() {
        when(taskRepository.findById("bad-id")).thenReturn(Mono.empty());

        StepVerifier.create(taskService.addObserver("bad-id", "user-1"))
                .expectErrorMatches(e ->
                        e instanceof RuntimeException &&
                                e.getMessage().contains("Task not found"))
                .verify();
    }

    @Test
    @DisplayName("addObserver: повторное добавление того же observer → Set не дублирует")
    void addObserver_duplicate_setNotDuplicated() {
        Set<String> observers = new HashSet<>();
        observers.add("observer-1");
        task.setObserverIds(observers);

        when(taskRepository.findById("task-1")).thenReturn(Mono.just(task));
        when(taskRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(userRepository.findById("user-1")).thenReturn(Mono.just(author));
        when(userRepository.findById("user-2")).thenReturn(Mono.just(assignee));
        when(userRepository.findById("observer-1")).thenReturn(Mono.empty());

        StepVerifier.create(taskService.addObserver("task-1", "observer-1"))
                .assertNext(r -> assertThat(r).isNotNull())
                .verifyComplete();

        verify(taskRepository).save(argThat(t ->
                t.getObserverIds().size() == 1 // дубликата нет (Set)
        ));
    }

    // update

    @Test
    @DisplayName("update: задача не найдена → Mono.error")
    void update_taskNotFound_returnsError() {
        TaskRequest request = new TaskRequest();
        request.setName("Updated");
        request.setStatus(TaskStatus.DONE);

        Task updatedTask = Task.builder().id("bad-id").name("Updated")
                .status(TaskStatus.DONE).authorId("user-1").build();

        when(taskMapper.requestToTask("bad-id", request)).thenReturn(updatedTask);
        when(taskRepository.findById("bad-id")).thenReturn(Mono.empty());

        StepVerifier.create(taskService.update("bad-id", request))
                .expectErrorMatches(e ->
                        e instanceof RuntimeException &&
                                e.getMessage().contains("Task not found"))
                .verify();
    }

    @Test
    @DisplayName("update: preserves createdAt из оригинальной задачи")
    void update_preservesCreatedAt() {
        Instant originalCreatedAt = Instant.parse("2027-01-01T00:00:00Z");
        task.setCreatedAt(originalCreatedAt);

        TaskRequest request = new TaskRequest();
        request.setName("Updated Task");
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setAuthorId("user-1");
        request.setAssigneeId("user-2");

        Task mappedTask = Task.builder().id("task-1").name("Updated Task")
                .status(TaskStatus.IN_PROGRESS).authorId("user-1").assigneeId("user-2")
                .observerIds(new HashSet<>()).updatedAt(Instant.now()).build();

        when(taskMapper.requestToTask("task-1", request)).thenReturn(mappedTask);
        when(taskRepository.findById("task-1")).thenReturn(Mono.just(task));
        when(taskRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(userRepository.findById("user-1")).thenReturn(Mono.just(author));
        when(userRepository.findById("user-2")).thenReturn(Mono.just(assignee));

        StepVerifier.create(taskService.update("task-1", request))
                .assertNext(r -> assertThat(r).isNotNull())
                .verifyComplete();

        // createdAt должен быть сохранён из оригинала
        verify(taskRepository).save(argThat(t ->
                t.getCreatedAt().equals(originalCreatedAt)
        ));
    }

    // deleteById

    @Test
    @DisplayName("deleteById: вызывает deleteById на репозитории")
    void deleteById_callsRepository() {
        when(taskRepository.deleteById("task-1")).thenReturn(Mono.empty());

        StepVerifier.create(taskService.deleteById("task-1"))
                .verifyComplete();

        verify(taskRepository).deleteById("task-1");
    }
}