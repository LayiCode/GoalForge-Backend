package com.uthman.VaultApi.auth;

import com.uthman.VaultApi.config.JwtUtil;
import com.uthman.VaultApi.mail.MailService;
import com.uthman.VaultApi.user.User;
import com.uthman.VaultApi.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private static final long TOKEN_TTL_SECONDS = 3600;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetTokenRepository tokenRepository;
    private final MailService mailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       PasswordResetTokenRepository tokenRepository,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.tokenRepository = tokenRepository;
        this.mailService = mailService;
    }

    public Map<String, String> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        validatePasswordNotPersonal(request.getPassword(), request.getFullName(), request.getEmail());
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail());
        return Map.of("token", token);
    }

    public Map<String, String> login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));
        String token = jwtUtil.generateToken(request.getEmail());
        return Map.of("token", token);
    }

    // Always respond the same way whether or not the email exists,
    // so the endpoint cannot be used to probe for registered accounts.
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        tokenRepository.findByUserIdAndUsedFalse(user.getId()).ifPresent(tokenRepository::delete);
        String rawToken = generateToken();
        PasswordResetToken reset = new PasswordResetToken();
        reset.setUser(user);
        reset.setTokenHash(hash(rawToken));
        reset.setExpiresAt(Instant.now().plusSeconds(TOKEN_TTL_SECONDS));
        reset.setUsed(false);
        tokenRepository.save(reset);
        mailService.sendPasswordReset(email, rawToken);
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken reset = tokenRepository.findByTokenHash(hash(request.getToken().trim()))
                .filter(t -> !t.isUsed())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link"));
        User user = reset.getUser();
        validatePasswordNotPersonal(request.getNewPassword(), user.getFullName(), user.getEmail());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        reset.setUsed(true);
        tokenRepository.save(reset);
    }

    // Prevent users from using their name or email as their password
    private void validatePasswordNotPersonal(String password, String fullName, String email) {
        String p = password.toLowerCase(Locale.ROOT);
        String name = fullName.trim().toLowerCase(Locale.ROOT);
        String mail = email.trim().toLowerCase(Locale.ROOT);
        String emailLocalPart = mail.contains("@") ? mail.substring(0, mail.indexOf('@')) : mail;

        if (p.equals(name)
                || p.equals(mail)
                || p.equals(emailLocalPart)
                || (!name.isEmpty() && p.contains(name))) {
            throw new RuntimeException("Password cannot be the same as your name or email");
        }
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
