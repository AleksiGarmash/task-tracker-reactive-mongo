package com.example.tasktracker.dto;

import com.example.tasktracker.enumz.RoleType;
import lombok.Data;

import java.util.Set;

@Data
public class UserRequest {
    private String username;
    private String email;
    private String password;
    private Set<RoleType> roles;
}
