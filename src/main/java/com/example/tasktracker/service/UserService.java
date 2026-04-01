package com.example.tasktracker.service;

import com.example.tasktracker.dto.UserRequest;
import com.example.tasktracker.dto.UserResponse;
import com.example.tasktracker.entity.User;
import com.example.tasktracker.mapper.UserMapper;
import com.example.tasktracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Flux<UserResponse> findAll() {
        return  userRepository.findAll().map(userMapper::userToResponse);
    }

    public Mono<UserResponse> findById(String id) {
        return userRepository.findById(id).map(userMapper::userToResponse);
    }

    public Mono<UserResponse> create(UserRequest request) {
        User user = userMapper.requestToUser(request);
        return userRepository.save(user).map(userMapper::userToResponse);
    }

    public Mono<UserResponse> update(String id, UserRequest request) {
        User updated = userMapper.requestToUser(id, request);

        return userRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                .flatMap(user -> {
                    updated.setId(user.getId());
                    return userRepository.save(updated);
                })
                .map(userMapper::userToResponse);
    }

    public Mono<Void> deleteById(String id) {
        return userRepository.deleteById(id);
    }
}
