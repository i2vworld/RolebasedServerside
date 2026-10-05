package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.response.UserResponse;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.entity.UserRole;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Line-by-line unit/smoke tests for {@link UserService}.
 * Every public method and every branch (success + failure paths) is exercised.
 */
class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    private User buildUser(Long id, String email, UserRole role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test User " + id);
        user.setEmail(email);
        user.setPassword("encoded-pass");
        user.setActive(active);
        user.setRole(role);
        user.setEmailVerified(true);
        user.setLastLogin(LocalDateTime.now());
        return user;
    }

    // ---------------- smoke test ----------------

    @Test
    void smokeTest_serviceConstructsAndBasicCallSucceeds() {
        when(userRepository.findAll()).thenReturn(List.of());
        assertDoesNotThrow(() -> userService.getAllUsers());
    }

    // ---------------- getAllUsers ----------------

    @Test
    void getAllUsers_returnsMappedResponses() {
        User u1 = buildUser(1L, "a@test.com", UserRole.GENERAL, true);
        User u2 = buildUser(2L, "b@test.com", UserRole.EMPLOYEE, false);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserResponse> responses = userService.getAllUsers();

        assertEquals(2, responses.size());
        assertEquals("a@test.com", responses.get(0).getEmail());
        assertEquals("GENERAL", responses.get(0).getRole());
        assertTrue(responses.get(0).isActive());
        assertEquals("b@test.com", responses.get(1).getEmail());
        assertEquals("EMPLOYEE", responses.get(1).getRole());
        assertFalse(responses.get(1).isActive());
    }

    @Test
    void getAllUsers_emptyRepository_returnsEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> responses = userService.getAllUsers();

        assertTrue(responses.isEmpty());
    }

    // ---------------- getUserById ----------------

    @Test
    void getUserById_found_returnsMappedResponse() {
        User user = buildUser(1L, "a@test.com", UserRole.GENERAL, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        assertEquals(1L, response.getId());
        assertEquals("a@test.com", response.getEmail());
    }

    @Test
    void getUserById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    // ---------------- getUserByEmail ----------------

    @Test
    void getUserByEmail_found_returnsMappedResponse() {
        User user = buildUser(1L, "a@test.com", UserRole.GENERAL, true);
        when(userRepository.findByEmail("a@test.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserByEmail("a@test.com");

        assertEquals("a@test.com", response.getEmail());
    }

    @Test
    void getUserByEmail_notFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("ghost@test.com"));
    }

    // ---------------- updateUserStatus ----------------

    @Test
    void updateUserStatus_found_updatesAndSaves() {
        User user = buildUser(1L, "a@test.com", UserRole.GENERAL, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.updateUserStatus(1L, false);

        assertFalse(user.isActive());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserStatus_notFound_throwsResourceNotFoundExceptionAndDoesNotSave() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserStatus(99L, true));

        verify(userRepository, never()).save(any());
    }
}
