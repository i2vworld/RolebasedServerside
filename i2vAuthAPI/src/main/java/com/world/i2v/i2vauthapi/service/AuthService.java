package com.world.i2v.i2vauthapi.service;

import com.world.i2v.i2vauthapi.dto.request.ForgotPasswordRequest;
import com.world.i2v.i2vauthapi.dto.request.LoginRequest;
import com.world.i2v.i2vauthapi.dto.request.RefreshTokenRequest;
import com.world.i2v.i2vauthapi.dto.request.ResetPasswordRequest;
import com.world.i2v.i2vauthapi.dto.request.SignupRequest;
import com.world.i2v.i2vauthapi.dto.response.JwtResponse;
import com.world.i2v.i2vauthapi.entity.Department;
import com.world.i2v.i2vauthapi.entity.Designation;
import com.world.i2v.i2vauthapi.entity.EmployeeUser;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final EmployeeUserRepository employeeUserRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    public AuthService(UserRepository userRepository,
                        PasswordResetTokenRepository tokenRepository,
                        UserSessionRepository sessionRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil,
                        EmailService emailService,
                        AuthenticationManager authenticationManager,
                        EmployeeUserRepository employeeUserRepository,
                        DepartmentRepository departmentRepository,
                        DesignationRepository designationRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.employeeUserRepository = employeeUserRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
    }

    /**
     * Hash a token using BCrypt for secure storage
     */
    private String hashToken(String token) {
        return passwordEncoder.encode(token);
    }

    /**
     * Register a new user
     */
    public void signup(SignupRequest request) {
        log.info("Signup request for email: {}", request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false);
        user.setActive(true);

        try {
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        } catch (IllegalArgumentException e) {
            user.setRole(UserRole.GENERAL);
        }

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        if (user.getRole() == UserRole.EMPLOYEE) {
            createEmployeeProfile(savedUser);
        }

        emailService.sendWelcomeEmail(savedUser);
    }

    private void createEmployeeProfile(User user) {
        try {
            if (employeeUserRepository.existsById(user.getId())) {
                log.info("EmployeeUser already exists for userId: {}", user.getId());
                return;
            }

            Optional<Department> deptOpt = departmentRepository.findByCode("MGMT");
            Optional<Designation> desigOpt = designationRepository.findByCode("EMP");

            if (deptOpt.isEmpty() || desigOpt.isEmpty()) {
                log.warn("Cannot create EmployeeUser for {}: MGMT department or EMP designation not found", user.getEmail());
                return;
            }

            Department dept = deptOpt.get();
            Designation desig = desigOpt.get();
            String employeeCode = "EMP-" + user.getId();

            if (employeeUserRepository.existsByEmployeeCode(employeeCode)) {
                employeeCode = "EMP-" + user.getId() + "-" + System.currentTimeMillis();
            }

            EmployeeUser employee = new EmployeeUser();
            employee.setUser(user);
            employee.setEmployeeCode(employeeCode);
            employee.setDepartment(dept);
            employee.setDesignation(desig);
            employee.setJoiningDate(LocalDate.now());
            employee.setEmploymentType(EmployeeUser.EmploymentType.PERMANENT);
            employee.setEmploymentStatus(EmployeeUser.EmploymentStatus.ACTIVE);
            employee.setCountry("India");
            employee.setWeeklyOff("SATURDAY,SUNDAY");
            employee.setAnnualLeavesEntitled(24);
            employee.setSickLeavesEntitled(12);
            employee.setCasualLeavesEntitled(12);
            employee.setProbationPeriodMonths(6);
            employee.setNoticePeriodDays(30);

            employeeUserRepository.save(employee);
            log.info("EmployeeUser profile created for userId: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to create EmployeeUser profile for {}: {}", user.getEmail(), e.getMessage());
        }
    }

    /**
     * Authenticate user and generate JWT tokens
     */
    public JwtResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            log.warn("Login attempt for inactive account: {}", request.getEmail());
            throw new UnauthorizedException("Account is disabled");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(authentication);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Hash both tokens before storing for security
        String hashedSessionToken = hashToken(accessToken);
        String hashedRefreshToken = hashToken(refreshToken);

        UserSession session = new UserSession();
        session.setUser(user);
        session.setSessionToken(hashedSessionToken);  // Store hashed
        session.setRefreshToken(hashedRefreshToken);  // Store hashed
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setLoginAt(LocalDateTime.now());
        session.setLastActivityAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusHours(24));
        session.setIsActive(true);
        sessionRepository.save(session);

        log.info("Login successful for user: {}", user.getEmail());

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Logout user and invalidate session
     */
    public void logout(String token) {
        log.info("Logout request");

        // Session tokens are stored BCrypt-hashed, so they can't be looked up directly
        // by the raw access token. Instead, resolve the user from the JWT and match the
        // raw token against each of their active sessions' hashed session token.
        if (jwtUtil.validateToken(token)) {
            String email = jwtUtil.getEmailFromToken(token);
            userRepository.findByEmail(email).ifPresent(user -> {
                List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActive(user.getId(), true);
                for (UserSession session : activeSessions) {
                    if (session.getSessionToken() != null &&
                            passwordEncoder.matches(token, session.getSessionToken())) {
                        session.setIsActive(false);
                        session.setLogoutAt(LocalDateTime.now());
                        sessionRepository.save(session);
                        log.info("Session invalidated for user ID: {}", user.getId());
                        break;
                    }
                }
            });
        }

        SecurityContextHolder.clearContext();
    }

    /**
     * Refresh access token using refresh token (with rotation and hashed storage)
     */
    public JwtResponse refreshToken(String refreshToken) {
        log.info("Token refresh request");

        // Validate refresh token structure
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String email = jwtUtil.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Find the active session that contains a matching hashed refresh token
        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActive(user.getId(), true);
        UserSession matchingSession = null;

        for (UserSession session : activeSessions) {
            if (session.getRefreshToken() != null &&
                    passwordEncoder.matches(refreshToken, session.getRefreshToken())) {
                matchingSession = session;
                break;
            }
        }

        if (matchingSession == null) {
            throw new UnauthorizedException("Invalid or expired session");
        }

        if (matchingSession.getExpiresAt().isBefore(LocalDateTime.now())) {
            matchingSession.setIsActive(false);
            sessionRepository.save(matchingSession);
            throw new UnauthorizedException("Session expired");
        }

        // Token rotation: invalidate the old refresh token by removing it
        matchingSession.setRefreshToken(null);
        matchingSession.setIsActive(false);
        matchingSession.setLogoutAt(LocalDateTime.now());
        sessionRepository.save(matchingSession);

        // Generate new tokens
        String newAccessToken = jwtUtil.generateTokenFromEmail(user.getEmail(), user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        String hashedNewRefreshToken = hashToken(newRefreshToken);

        // Create a new session for the new refresh token
        UserSession newSession = new UserSession();
        newSession.setUser(user);
        newSession.setSessionToken(hashToken(newAccessToken));  // Store hashed, consistent with login()
        newSession.setRefreshToken(hashedNewRefreshToken);
        newSession.setIpAddress(matchingSession.getIpAddress());
        newSession.setUserAgent(matchingSession.getUserAgent());
        newSession.setLoginAt(matchingSession.getLoginAt());
        newSession.setLastActivityAt(LocalDateTime.now());
        newSession.setExpiresAt(LocalDateTime.now().plusHours(24));
        newSession.setIsActive(true);
        sessionRepository.save(newSession);

        log.info("Token refreshed for user: {} (old session invalidated)", user.getEmail());

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Process forgot password request
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is disabled");
        }

        tokenRepository.deactivateUserTokens(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setEmail(user.getEmail());
        resetToken.setExpiry(LocalDateTime.now().plusHours(24));
        resetToken.setIsUsed(false);
        tokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user, token);

        log.info("Password reset email sent to: {}", user.getEmail());
    }

    /**
     * Reset password using token
     */
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Reset password request");

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        PasswordResetToken resetToken = tokenRepository.findByTokenAndIsUsedFalse(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (resetToken.getExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        // Invalidate all user sessions on password reset
        sessionRepository.deactivateUserSessions(user.getId());

        log.info("Password reset successful for user: {}", user.getEmail());
    }

    /**
     * Change password for authenticated user
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all other sessions except current (handled by frontend token)
        log.info("Password changed for user ID: {}", userId);
    }
}