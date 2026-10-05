package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.ForgotPasswordRequest;
import com.world.i2v.i2vauthapi.dto.request.LoginRequest;
import com.world.i2v.i2vauthapi.dto.request.ResetPasswordRequest;
import com.world.i2v.i2vauthapi.dto.request.SignupRequest;
import com.world.i2v.i2vauthapi.dto.response.JwtResponse;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Designation;
import com.world.i2v.i2vauthapi.entity.PasswordResetToken;
import com.world.i2v.i2vauthapi.entity.User;
import com.world.i2v.i2vauthapi.entity.UserRole;
import com.world.i2v.i2vauthapi.entity.UserSession;
import com.world.i2v.i2vauthapi.exception.BadRequestException;
import com.world.i2v.i2vauthapi.exception.ResourceNotFoundException;
import com.world.i2v.i2vauthapi.exception.UnauthorizedException;
import com.world.i2v.i2vauthapi.repository.DepartmentRepository;
import com.world.i2v.i2vauthapi.repository.DesignationRepository;
import com.world.i2v.i2vauthapi.repository.EmployeeUserRepository;
import com.world.i2v.i2vauthapi.repository.PasswordResetTokenRepository;
import com.world.i2v.i2vauthapi.repository.UserRepository;
import com.world.i2v.i2vauthapi.repository.UserSessionRepository;
import com.world.i2v.i2vauthapi.security.JwtUtil;
import com.world.i2v.i2vauthapi.security.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private UserSessionRepository sessionRepository;
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private EmailService emailService;
    private AuthenticationManager authenticationManager;
    private EmployeeUserRepository employeeUserRepository;
    private DepartmentRepository departmentRepository;
    private DesignationRepository designationRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        sessionRepository = mock(UserSessionRepository.class);
        passwordEncoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        emailService = mock(EmailService.class);
        authenticationManager = mock(AuthenticationManager.class);
        employeeUserRepository = mock(EmployeeUserRepository.class);
        departmentRepository = mock(DepartmentRepository.class);
        designationRepository = mock(DesignationRepository.class);

        authService = new AuthService(userRepository, tokenRepository, sessionRepository,
                passwordEncoder, jwtUtil, emailService, authenticationManager,
                employeeUserRepository, departmentRepository, designationRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(Long id, String email, UserRole role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setFullName("Test User");
        user.setEmail(email);
        user.setPassword("encoded-pass");
        user.setActive(active);
        user.setRole(role);
        return user;
    }

    // ---------------- signup ----------------

    @Test
    void signup_passwordMismatch_throwsBadRequest() {
        SignupRequest request = new SignupRequest();
        request.setPassword("pass1");
        request.setConfirmPassword("pass2");

        assertThrows(BadRequestException.class, () -> authService.signup(request));
        verifyNoInteractions(userRepository);
    }

    @Test
    void signup_emailAlreadyExists_throwsBadRequest() {
        SignupRequest request = new SignupRequest();
        request.setEmail("existing@test.com");
        request.setPassword("pass123");
        request.setConfirmPassword("pass123");

        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.signup(request));
    }

    @Test
    void signup_success_defaultRoleGeneral() {
        SignupRequest request = new SignupRequest();
        request.setFullName("New User");
        request.setEmail("new@test.com");
        request.setPassword("pass123");
        request.setConfirmPassword("pass123");
        request.setRole("GENERAL");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        authService.signup(request);

        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(any(User.class));
        verifyNoInteractions(employeeUserRepository);
    }

    @Test
    void signup_invalidRole_fallsBackToGeneral() {
        SignupRequest request = new SignupRequest();
        request.setFullName("New User");
        request.setEmail("new2@test.com");
        request.setPassword("pass123");
        request.setConfirmPassword("pass123");
        request.setRole("NOT_A_ROLE");

        when(userRepository.existsByEmail("new2@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        authService.signup(request);

        verify(userRepository).save(argThat(u -> u.getRole() == UserRole.GENERAL));
    }

    @Test
    void signup_employeeRole_createsEmployeeProfile() {
        SignupRequest request = new SignupRequest();
        request.setFullName("Employee User");
        request.setEmail("emp@test.com");
        request.setPassword("pass123");
        request.setConfirmPassword("pass123");
        request.setRole("EMPLOYEE");

        when(userRepository.existsByEmail("emp@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(3L);
            return u;
        });

        Department dept = new Department();
        dept.setId(1L);
        dept.setCode("MGMT");
        Designation desig = new Designation();
        desig.setId(1L);
        desig.setCode("EMP");

        when(employeeUserRepository.existsById(3L)).thenReturn(false);
        when(departmentRepository.findByCode("MGMT")).thenReturn(Optional.of(dept));
        when(designationRepository.findByCode("EMP")).thenReturn(Optional.of(desig));
        when(employeeUserRepository.existsByEmployeeCode("EMP-3")).thenReturn(false);

        authService.signup(request);

        verify(employeeUserRepository).save(any());
        verify(emailService).sendWelcomeEmail(any(User.class));
    }

    @Test
    void signup_employeeRole_missingDeptOrDesignation_doesNotThrow() {
        SignupRequest request = new SignupRequest();
        request.setFullName("Employee User");
        request.setEmail("emp2@test.com");
        request.setPassword("pass123");
        request.setConfirmPassword("pass123");
        request.setRole("EMPLOYEE");

        when(userRepository.existsByEmail("emp2@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(4L);
            return u;
        });

        when(employeeUserRepository.existsById(4L)).thenReturn(false);
        when(departmentRepository.findByCode("MGMT")).thenReturn(Optional.empty());
        when(designationRepository.findByCode("EMP")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.signup(request));

        verify(employeeUserRepository, never()).save(any());
        verify(emailService).sendWelcomeEmail(any(User.class));
    }

    @Test
    void signup_employeeProfileAlreadyExists_skipsCreation() {
        SignupRequest request = new SignupRequest();
        request.setFullName("Employee User");
        request.setEmail("emp3@test.com");
        request.setPassword("pass123");
        request.setConfirmPassword("pass123");
        request.setRole("EMPLOYEE");

        when(userRepository.existsByEmail("emp3@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(5L);
            return u;
        });

        when(employeeUserRepository.existsById(5L)).thenReturn(true);

        authService.signup(request);

        verify(departmentRepository, never()).findByCode(anyString());
        verify(employeeUserRepository, never()).save(any());
    }

    @Test
    void signup_employeeCodeCollision_generatesTimestampSuffix() {
        SignupRequest request = new SignupRequest();
        request.setFullName("Employee User");
        request.setEmail("emp4@test.com");
        request.setPassword("pass123");
        request.setConfirmPassword("pass123");
        request.setRole("EMPLOYEE");

        when(userRepository.existsByEmail("emp4@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(6L);
            return u;
        });

        Department dept = new Department();
        dept.setId(1L);
        dept.setCode("MGMT");
        Designation desig = new Designation();
        desig.setId(1L);
        desig.setCode("EMP");

        when(employeeUserRepository.existsById(6L)).thenReturn(false);
        when(departmentRepository.findByCode("MGMT")).thenReturn(Optional.of(dept));
        when(designationRepository.findByCode("EMP")).thenReturn(Optional.of(desig));
        when(employeeUserRepository.existsByEmployeeCode("EMP-6")).thenReturn(true);

        authService.signup(request);

        verify(employeeUserRepository).save(argThat(e -> e.getEmployeeCode().startsWith("EMP-6-")));
    }

    // ---------------- login ----------------

    @Test
    void login_success_returnsJwtResponseAndSavesSession() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("pass123");

        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);

        UserDetailsImpl userDetails = new UserDetailsImpl(1L, "user@test.com", "Test User",
                "encoded-pass", "GENERAL", true, Collections.singletonList(new SimpleGrantedAuthority("ROLE_GENERAL")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(authentication)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("user@test.com")).thenReturn("refresh-token");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-token");

        JwtResponse response = authService.login(request, "127.0.0.1", "test-agent");

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(1L, response.getUserId());
        assertEquals("user@test.com", response.getEmail());
        assertEquals("GENERAL", response.getRole());

        verify(sessionRepository).save(argThat(s ->
                s.getSessionToken().equals("hashed-token") && s.getRefreshToken().equals("hashed-token")));
        verify(userRepository).save(argThat(u -> u.getLastLogin() != null));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void login_userNotFound_throwsResourceNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("pass123");

        UserDetailsImpl userDetails = new UserDetailsImpl(1L, "user@test.com", "Test User",
                "encoded-pass", "GENERAL", true, Collections.singletonList(new SimpleGrantedAuthority("ROLE_GENERAL")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request, "127.0.0.1", "agent"));
    }

    @Test
    void login_inactiveAccount_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("pass123");

        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, false);

        UserDetailsImpl userDetails = new UserDetailsImpl(1L, "user@test.com", "Test User",
                "encoded-pass", "GENERAL", false, Collections.singletonList(new SimpleGrantedAuthority("ROLE_GENERAL")));
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.login(request, "127.0.0.1", "agent"));

        verify(sessionRepository, never()).save(any());
    }

    // ---------------- logout ----------------

    @Test
    void logout_matchingHashedSession_invalidatesSession() {
        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);

        UserSession session = new UserSession();
        session.setId(10L);
        session.setUser(user);
        session.setIsActive(true);
        session.setSessionToken("hashed-token-abc");

        when(jwtUtil.validateToken("token-abc")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("token-abc")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserIdAndIsActive(1L, true)).thenReturn(List.of(session));
        when(passwordEncoder.matches("token-abc", "hashed-token-abc")).thenReturn(true);

        authService.logout("token-abc");

        verify(sessionRepository).save(argThat(s -> !s.getIsActive() && s.getLogoutAt() != null));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void logout_invalidToken_doesNotThrowAndSkipsLookup() {
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        assertDoesNotThrow(() -> authService.logout("bad-token"));

        verify(userRepository, never()).findByEmail(any());
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void logout_noMatchingSession_doesNotThrow() {
        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);

        UserSession session = new UserSession();
        session.setId(10L);
        session.setUser(user);
        session.setIsActive(true);
        session.setSessionToken("hashed-other-token");

        when(jwtUtil.validateToken("token-abc")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("token-abc")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserIdAndIsActive(1L, true)).thenReturn(List.of(session));
        when(passwordEncoder.matches("token-abc", "hashed-other-token")).thenReturn(false);

        assertDoesNotThrow(() -> authService.logout("token-abc"));

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void logout_userNotFound_doesNotThrow() {
        when(jwtUtil.validateToken("token-abc")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("token-abc")).thenReturn("ghost@test.com");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.logout("token-abc"));

        verify(sessionRepository, never()).save(any());
    }

    // ---------------- refreshToken ----------------

    @Test
    void refreshToken_invalidToken_throwsUnauthorized() {
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("bad-token"));
    }

    @Test
    void refreshToken_notARefreshToken_throwsUnauthorized() {
        when(jwtUtil.validateToken("access-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("access-token")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("access-token"));
    }

    @Test
    void refreshToken_userNotFound_throwsUnauthorized() {
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("refresh-token")).thenReturn("missing@test.com");
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("refresh-token"));
    }

    @Test
    void refreshToken_noMatchingSession_throwsUnauthorized() {
        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);

        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("refresh-token")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserIdAndIsActive(1L, true)).thenReturn(Collections.emptyList());

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("refresh-token"));
    }

    @Test
    void refreshToken_expiredSession_throwsUnauthorizedAndDeactivates() {
        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);

        UserSession session = new UserSession();
        session.setId(20L);
        session.setUser(user);
        session.setRefreshToken("hashed-refresh");
        session.setExpiresAt(LocalDateTime.now().minusHours(1));
        session.setIsActive(true);

        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("refresh-token")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserIdAndIsActive(1L, true)).thenReturn(List.of(session));
        when(passwordEncoder.matches("refresh-token", "hashed-refresh")).thenReturn(true);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("refresh-token"));

        verify(sessionRepository).save(argThat(s -> !s.getIsActive()));
    }

    @Test
    void refreshToken_success_rotatesTokenAndCreatesNewSession() {
        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);

        UserSession session = new UserSession();
        session.setId(20L);
        session.setUser(user);
        session.setRefreshToken("hashed-refresh");
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        session.setIsActive(true);
        session.setIpAddress("127.0.0.1");
        session.setUserAgent("agent");
        session.setLoginAt(LocalDateTime.now().minusHours(2));

        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getEmailFromToken("refresh-token")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(sessionRepository.findByUserIdAndIsActive(1L, true)).thenReturn(List.of(session));
        when(passwordEncoder.matches("refresh-token", "hashed-refresh")).thenReturn(true);
        when(jwtUtil.generateTokenFromEmail("user@test.com", "GENERAL")).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken("user@test.com")).thenReturn("new-refresh-token");
        when(passwordEncoder.encode("new-refresh-token")).thenReturn("hashed-new-refresh");

        JwtResponse response = authService.refreshToken("refresh-token");

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals(1L, response.getUserId());

        // Old session invalidated
        verify(sessionRepository).save(argThat(s -> s.getId() != null && s.getId().equals(20L) && !s.getIsActive()));
        // New session created and persisted
        verify(sessionRepository).save(argThat(s -> s.getId() == null && s.getIsActive()
                && "hashed-new-refresh".equals(s.getRefreshToken())));
    }

    // ---------------- forgotPassword ----------------

    @Test
    void forgotPassword_userNotFound_throwsResourceNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@test.com");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(request));
    }

    @Test
    void forgotPassword_inactiveAccount_throwsBadRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@test.com");

        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, false);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> authService.forgotPassword(request));
    }

    @Test
    void forgotPassword_success_savesTokenAndSendsEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@test.com");

        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        verify(tokenRepository).deactivateUserTokens(1L);
        verify(tokenRepository).save(argThat(t -> t.getEmail().equals("user@test.com") && !t.getIsUsed()));
        verify(emailService).sendPasswordResetEmail(eq(user), anyString());
    }

    // ---------------- resetPassword ----------------

    @Test
    void resetPassword_passwordMismatch_throwsBadRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("tok");
        request.setNewPassword("newpass1");
        request.setConfirmPassword("newpass2");

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void resetPassword_invalidToken_throwsBadRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("tok");
        request.setNewPassword("newpass1");
        request.setConfirmPassword("newpass1");

        when(tokenRepository.findByTokenAndIsUsedFalse("tok")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
    }

    @Test
    void resetPassword_expiredToken_throwsBadRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("tok");
        request.setNewPassword("newpass1");
        request.setConfirmPassword("newpass1");

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("tok");
        resetToken.setExpiry(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByTokenAndIsUsedFalse("tok")).thenReturn(Optional.of(resetToken));

        assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
    }

    @Test
    void resetPassword_success_updatesPasswordAndInvalidatesSessions() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("tok");
        request.setNewPassword("newpass1");
        request.setConfirmPassword("newpass1");

        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("tok");
        resetToken.setUser(user);
        resetToken.setExpiry(LocalDateTime.now().plusHours(1));
        resetToken.setIsUsed(false);

        when(tokenRepository.findByTokenAndIsUsedFalse("tok")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newpass1")).thenReturn("hashed-newpass");

        authService.resetPassword(request);

        verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed-newpass")));
        verify(tokenRepository).save(argThat(t -> t.getIsUsed() && t.getUsedAt() != null));
        verify(sessionRepository).deactivateUserSessions(1L);
    }

    // ---------------- changePassword ----------------

    @Test
    void changePassword_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.changePassword(1L, "current", "newpass"));
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsBadRequest() {
        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> authService.changePassword(1L, "wrong", "newpass"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_success_updatesPassword() {
        User user = buildUser(1L, "user@test.com", UserRole.GENERAL, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("hashed-newpass");

        authService.changePassword(1L, "current", "newpass");

        verify(userRepository).save(argThat(u -> u.getPassword().equals("hashed-newpass")));
    }
}
