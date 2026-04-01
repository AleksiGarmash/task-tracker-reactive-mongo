package com.example.tasktracker.service;

import com.example.tasktracker.dto.TaskRequest;
import com.example.tasktracker.dto.TaskResponse;
import com.example.tasktracker.entity.Task;
import com.example.tasktracker.entity.User;
import com.example.tasktracker.mapper.TaskMapper;
import com.example.tasktracker.repository.TaskRepository;
import com.example.tasktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public Flux<TaskResponse> findAll() {
        return taskRepository.findAll().flatMap(this::populateTaskRelations);
    }

    public Mono<TaskResponse> findById(String id) {
        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Task not found")))
                .flatMap(this::populateTaskRelations);
    }

    public Mono<TaskResponse> create(TaskRequest request) {
        Task task = Task.builder()
                .name(request.getName())
                .description(request.getDescription())
                .authorId(request.getAuthorId())
                .status(request.getStatus())
                .assigneeId(request.getAssigneeId())
                .observerIds(request.getObserverIds() != null ? new HashSet<>(request.getObserverIds()) : new HashSet<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return taskRepository.save(task).flatMap(this::populateTaskRelations);
    }

    public Mono<TaskResponse> update(String id, TaskRequest request) {
        Task updated = taskMapper.requestToTask(id, request);
        updated.setUpdatedAt(Instant.now());

        return taskRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Task not found")))
                .flatMap(task -> {
                    updated.setCreatedAt(task.getCreatedAt());
                    return taskRepository.save(updated);
                })
                .flatMap(this::populateTaskRelations);
    }

    public Mono<TaskResponse> addObserver(String taskId, String observerId) {
        return taskRepository.findById(taskId)
                .switchIfEmpty(Mono.error(new RuntimeException("Task not found")))
                .map(task -> {
                    Set<String> ids = task.getObserverIds();
                    if (ids == null) {
                        ids = new HashSet<>();
                        task.setObserverIds(ids);
                    }
                    ids.add(observerId);
                    task.setUpdatedAt(Instant.now());
                    return task;
                })
                .flatMap(taskRepository::save)
                .flatMap(this::populateTaskRelations);
    }

    public Mono<Void> deleteById(String id) {
        return taskRepository.deleteById(id);
    }

    private Mono<TaskResponse> populateTaskRelations(Task task) {
        Mono<User> authorMono = task.getAuthorId() != null ?
                userRepository.findById(task.getAuthorId()).defaultIfEmpty(createDummyUser(task.getAuthorId())) : Mono.just(new User());

        Mono<User> assigneeMono = task.getAssigneeId() != null ?
                userRepository.findById(task.getAssigneeId()).defaultIfEmpty(createDummyUser(task.getAssigneeId())) : Mono.just(new User());

        Mono<Set<User>> observerFlux = task.getObserverIds() != null && !task.getObserverIds().isEmpty() ?
                Flux.fromIterable(task.getObserverIds())
                        .flatMap(id -> userRepository.findById(id).defaultIfEmpty(createDummyUser(id)))
                        .collectList()
                        .map(list -> new HashSet<>(list)) : Mono.just(Collections.emptySet());

        return Mono.zip(authorMono, assigneeMono, observerFlux)
                .map(tuple -> TaskResponse.builder()
                        .id(task.getId())
                        .name(task.getName())
                        .description(task.getDescription())
                        .status(task.getStatus())
                        .createdAt(task.getCreatedAt())
                        .updatedAt(task.getUpdatedAt())
                        .author(tuple.getT1())
                        .assignee(tuple.getT2())
                        .observers(tuple.getT3())
                        .build());
    }

    private User createDummyUser(String id) {
        return User.builder()
                .id(id)
                .username(id)
                .email(id + "@tasktracker.com")
                .roles(new HashSet<>())
                .build();
    }
}
