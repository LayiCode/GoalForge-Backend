package com.uthman.VaultApi;

import com.uthman.VaultApi.auth.*;
import com.uthman.VaultApi.user.User;
import com.uthman.VaultApi.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthFlowTests {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;

    private static final String STRONG_PASSWORD = "Str0ng!Password";

    private User register(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setFullName("Test User");
        request.setPassword(STRONG_PASSWORD);
        authService.register(request);
        return userRepository.findByEmail(email).orElseThrow();
    }

    private PasswordResetToken createToken(User user, String rawToken, boolean expired, boolean used) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(expired ? Instant.now().minusSeconds(60) : Instant.now().plusSeconds(3600));
        token.setUsed(used);
        return tokenRepository.save(token);
    }

    @Test
    void forgotPasswordCreatesPendingTokenForKnownEmail() {
        User user = register("reset1@test.com");
        authService.forgotPassword(request("reset1@test.com"));

        PasswordResetToken token = tokenRepository.findByUserIdAndUsedFalse(user.getId())
                .orElseThrow(() -> new AssertionError("no pending token created"));
        assertFalse(token.isUsed());
        assertTrue(token.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void forgotPasswordIsSilentForUnknownEmail() {
        long before = tokenRepository.count();
        authService.forgotPassword(request("nobody@test.com"));
        assertEquals(before, tokenRepository.count());
    }

    @Test
    void resetPasswordWithValidTokenUpdatesPasswordAndAllowsLogin() {
        User user = register("reset2@test.com");
        String rawToken = "0a0b0c0d0e0f00112233445566778899aabbccddeeff00112233445566778899";
        createToken(user, rawToken, false, false);

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setToken(rawToken);
        reset.setNewPassword("Brand!New7");
        authService.resetPassword(reset);

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("Brand!New7", updated.getPassword()));
        assertTrue(tokenRepository.findByTokenHash(hash(rawToken)).orElseThrow().isUsed());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("reset2@test.com", "Brand!New7"));
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        User user = register("reset3@test.com");
        createToken(user, "expired-token-abc-123-def-456", true, false);

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setToken("expired-token-abc-123-def-456");
        reset.setNewPassword("Brand!New7");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.resetPassword(reset));
        assertEquals("Invalid or expired reset link", ex.getMessage());
    }

    @Test
    void resetPasswordRejectsAlreadyUsedToken() {
        User user = register("reset4@test.com");
        createToken(user, "used-token-abc-123-def-456", false, true);

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setToken("used-token-abc-123-def-456");
        reset.setNewPassword("Brand!New7");
        assertThrows(RuntimeException.class, () -> authService.resetPassword(reset));
    }

    @Test
    void resetPasswordRejectsPersonalPassword() {
        User user = register("reset5@test.com");
        createToken(user, "personal-token-abc-123-def-456", false, false);

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setToken("personal-token-abc-123-def-456");
        reset.setNewPassword("Test User!");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.resetPassword(reset));
        assertEquals("Password cannot be the same as your name or email", ex.getMessage());
    }

    @Test
    void changePasswordWithCorrectCurrentPasswordWorks() {
        User user = register("change1@test.com");

        ChangePasswordRequest change = new ChangePasswordRequest();
        change.setCurrentPassword(STRONG_PASSWORD);
        change.setNewPassword("Fresh!Pass9");
        authService.changePassword("change1@test.com", change);

        assertTrue(passwordEncoder.matches("Fresh!Pass9",
                userRepository.findById(user.getId()).orElseThrow().getPassword()));
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("change1@test.com", "Fresh!Pass9"));
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        register("change2@test.com");

        ChangePasswordRequest change = new ChangePasswordRequest();
        change.setCurrentPassword("Wrong!Pass1");
        change.setNewPassword("Fresh!Pass9");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.changePassword("change2@test.com", change));
        assertEquals("Current password is incorrect", ex.getMessage());
    }

    private static ForgotPasswordRequest request(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
