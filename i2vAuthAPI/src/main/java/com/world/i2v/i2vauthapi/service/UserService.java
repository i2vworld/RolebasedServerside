package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.response.UserResponse;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserResponse> getAllUsers() {
        log.info("════════════════════════════════════════");
        log.info("👥 START: getAllUsers()");
        List<User> allUsers = userRepository.findAll();
        log.info("📊 Total users in database: {}", allUsers.size());
        
        allUsers.forEach(user -> {
            log.info("  User ID: {} | Name: {} | Email: {} | Active: {}", 
                user.getId(), user.getFullName(), user.getEmail(), user.isActive());
        });
        
        List<UserResponse> responses = allUsers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        
        log.info("👥 Returning {} users", responses.size());
        log.info("════════════════════════════════════════");
        return responses;
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return mapToResponse(user);
    }

    @Transactional
    public void updateUserStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        user.setActive(active);
        userRepository.save(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .active(user.isActive())
                .emailVerified(user.getEmailVerified())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}