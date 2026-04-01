package com.example.tasktracker.dto;

import com.example.tasktracker.enumz.RoleType;
import lombok.Data;

import java.util.Set;

@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private Set<RoleType> roles;
}
