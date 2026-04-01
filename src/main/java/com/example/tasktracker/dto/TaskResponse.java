package com.example.tasktracker.dto;

import com.example.tasktracker.entity.User;
import com.example.tasktracker.enumz.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class TaskResponse {
    private String id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private TaskStatus status;
    private User author;
    private User assignee;
    private Set<User> observers;
}
