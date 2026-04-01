package com.example.tasktracker.dto;

import com.example.tasktracker.enumz.TaskStatus;
import lombok.Data;

import java.util.Set;

@Data
public class TaskRequest {
    private String name;
    private String description;
    private TaskStatus status;
    private String authorId;
    private String assigneeId;
    private Set<String> observerIds;
}
