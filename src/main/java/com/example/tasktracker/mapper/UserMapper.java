package com.example.tasktracker.mapper;

import com.example.tasktracker.dto.UserRequest;
import com.example.tasktracker.dto.UserResponse;
import com.example.tasktracker.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User requestToUser(UserRequest request);
    User requestToUser(String id, UserRequest request);
    UserResponse userToResponse(User user);
}
