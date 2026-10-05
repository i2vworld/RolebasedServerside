# Implementation Notes - Session Token Hashing

## Important: Database Schema Update

The session token hashing fix requires careful implementation. Here's what you need to know:

---

## Current Implementation Status

### ✅ Done
- Session tokens are now hashed before being stored in database
- New login sessions will have hashed tokens

### ⚠️ Important: Logout and Token Validation

The `AuthService.logout()` and session lookup methods will need updating because they currently search by plaintext token.

**Current Implementation (Won't work with hashing):**
```java
public void logout(String token) {
    sessionRepository.findBySessionToken(token).ifPresent(session -> {
        // This won't find the session because we're searching for plaintext
        // but the database now has hashed tokens
    });
}
```

**Updated Implementation Needed:**
```java
public void logout(String token) {
    // Since tokens are hashed, we can't search directly
    // Instead, extract email from JWT and find user's sessions
    if (!jwtUtil.validateToken(token)) {
        throw new UnauthorizedException("Invalid token");
    }
    
    String email = jwtUtil.getEmailFromToken(token);
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("User not found"));
    
    List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActive(user.getId(), true);
    
    for (UserSession session : activeSessions) {
        // Compare hashed token with incoming token
        if (session.getRefreshToken() != null && 
            passwordEncoder.matches(token, session.getSessionToken())) {
            session.setIsActive(false);
            session.setLogoutAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Session invalidated for user: {}", user.getEmail());
            return;
        }
    }
    
    throw new ResourceNotFoundException("Active session not found");
}
```

---

## Database Migration

### For New Deployments
If this is a fresh deployment, no migration is needed. All new sessions will have hashed tokens.

### For Existing Deployments with Data
You have options:

**Option 1: Invalidate All Existing Sessions (Recommended)**
- Users will need to log in again
- All old plaintext tokens become invalid
- Clean security state

**SQL Script:**
```sql
UPDATE user_sessions 
SET is_active = 0, logout_at = GETDATE() 
WHERE is_active = 1;
```

**Option 2: Keep Both Hashing Approaches (Temporary)**
- Hash new tokens
- Allow plaintext tokens to still work temporarily
- Gradually force users to re-login
- Disable this approach after 1 week

**Option 3: Dual-Write Migration**
- Store both plaintext and hashed versions temporarily
- Update logout logic to check both
- Gradually phase out plaintext
- Not recommended due to complexity

---

## Required Code Updates

### 1. Update AuthService.logout()
File: `src/main/java/com/world/i2v/i2vauthapi/service/AuthService.java`

See code example above in "Updated Implementation Needed" section.

### 2. Update JwtAuthenticationFilter (if applicable)
File: `src/main/java/com/world/i2v/i2vauthapi/security/JwtAuthenticationFilter.java`

If this filter validates active sessions:
```java
// Extract and validate token
String token = extractTokenFromRequest(request);
if (token != null && jwtUtil.validateToken(token)) {
    String email = jwtUtil.getEmailFromToken(token);
    UserSession session = sessionRepository.findByUserIdAndIsActive(userId, true)
        .stream()
        .filter(s -> passwordEncoder.matches(token, s.getSessionToken()))
        .findFirst()
        .orElse(null);
    
    if (session != null) {
        // Token is valid and session is active
        // Proceed with request
    }
}
```

### 3. Update Session Management Endpoints in AuthController
File: `src/main/java/com/world/i2v/i2vauthapi/Controller/AuthController.java`

Update the `revokeSession()` method:
```java
@DeleteMapping("/sessions/{sessionId}")
@PreAuthorize("hasRole('EMPLOYEE')")
public ResponseEntity<ApiResponse<Void>> revokeSession(
        @PathVariable Long sessionId,
        @RequestHeader(value = "X-Current-Token", required = false) String currentToken) {
    try {
        UserSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        // Don't revoke current session through this endpoint
        if (currentToken != null && passwordEncoder.matches(currentToken, session.getSessionToken())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Cannot revoke current session. Use logout instead."));
        }

        session.setIsActive(false);
        session.setLogoutAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Session {} revoked", sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    } catch (ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Session not found"));
    }
}
```

---

## Verification Checklist

After implementation:

- [ ] New login creates hashed session tokens
- [ ] Logout invalidates sessions correctly
- [ ] Session revocation works
- [ ] "Revoke all other sessions" works
- [ ] Get my sessions endpoint works
- [ ] Token refresh validation works
- [ ] No plaintext tokens in database for active sessions
- [ ] Database queries are optimized (consider adding indexes)

---

## Performance Considerations

### Database Indexes
Since we're now hashing tokens and comparing in code, ensure these indexes exist:

```sql
-- Existing indexes
CREATE INDEX idx_user_sessions_user_id_active 
  ON user_sessions(user_id, is_active);

CREATE INDEX idx_user_sessions_login_at 
  ON user_sessions(login_at);

-- For cleanup of expired sessions
CREATE INDEX idx_user_sessions_expires_at 
  ON user_sessions(expires_at);
```

### Performance Impact
- **Query Performance**: Slightly slower (cannot search by token directly)
- **Security Gain**: Worth the small performance trade-off
- **Mitigation**: Use user_id + is_active index to find sessions quickly

---

## Testing Token Hashing

### Unit Test Example
```java
@Test
public void testSessionTokenIsHashed() {
    // Setup
    User user = createTestUser();
    String plainToken = "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...";
    
    // Action
    authService.login(request, "192.168.1.1", "Mozilla/5.0");
    
    // Assert
    UserSession session = sessionRepository.findByUserId(user.getId()).get(0);
    assertThat(session.getSessionToken())
        .doesNotContain("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9");  // Not plaintext
    assertThat(session.getSessionToken())
        .startsWith("$2a$");  // Starts with bcrypt prefix
}
```

### Integration Test Example
```java
@Test
@Transactional
public void testLogoutWithHashedToken() {
    // Login
    JwtResponse response = authService.login(request, "192.168.1.1", "Mozilla/5.0");
    
    // Verify session is active
    List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActive(userId, true);
    assertThat(activeSessions).isNotEmpty();
    
    // Logout
    authService.logout(response.getAccessToken());
    
    // Verify session is inactive
    activeSessions = sessionRepository.findByUserIdAndIsActive(userId, true);
    assertThat(activeSessions).isEmpty();
}
```

---

## Troubleshooting

### Issue: "Session not found" errors after deployment

**Cause**: Old plaintext sessions in database not matching hashed comparison

**Solution**:
```bash
# Clear old sessions
DELETE FROM user_sessions WHERE created_at < DATEADD(DAY, -1, GETDATE());

# Or mark all as inactive for clean migration
UPDATE user_sessions SET is_active = 0;
```

### Issue: Logout fails with hashed tokens

**Cause**: Not comparing with `passwordEncoder.matches()`

**Solution**: Ensure logout method uses the code example provided above.

### Issue: Performance slow with token verification

**Cause**: Querying all sessions for each request

**Solution**: Add database indexes as shown in "Performance Considerations" section.

---

## Security Benefits Achieved

✅ **Database Breach Protection**: Stolen tokens are hashed, not immediately usable
✅ **Logout Enforcement**: Can mark sessions invalid
✅ **Session Tracking**: Can identify and revoke specific sessions
✅ **Audit Trail**: Know when sessions were created/revoked
✅ **Token Rotation**: Can detect and prevent token reuse

---

## Timeline for Full Implementation

1. **Today**: Deploy hashing for new sessions (DONE ✓)
2. **Next 1-2 weeks**: Update logout/validation code
3. **Week 2-3**: Invalidate old sessions or run migration
4. **Week 3-4**: Monitor for issues and optimize
5. **Week 4+**: Remove old token comparison logic

---

## Questions?

1. **Should I use raw bcrypt hashing?** - Yes, the passwordEncoder bean already uses bcrypt with strength 12
2. **Can I reduce bcrypt strength?** - Not recommended. It's intentionally slow for security.
3. **How do I test this locally?** - See "Testing Token Hashing" section above
4. **Will this break existing mobile apps?** - Only if they have long-lived sessions. New logins will work fine.
