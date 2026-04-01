package com.example.tasktracker.mapper;

import com.example.tasktracker.dto.TaskRequest;
import com.example.tasktracker.dto.TaskResponse;
import com.example.tasktracker.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface TaskMapper {
    Task requestToTask(TaskRequest request);

    @Mapping(source = "taskId", target = "id")
    Task requestToTask(String taskId, TaskRequest request);

    TaskResponse taskToResponse(Task task);
}
