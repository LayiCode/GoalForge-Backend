package com.uthman.VaultApi.auth;

import com.uthman.VaultApi.config.JwtUtil;
import com.uthman.VaultApi.user.User;
import com.uthman.VaultApi.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public Map<String, String> register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        validatePasswordNotPersonal(request);
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

    // Prevent users from using their name or email as their password
    private void validatePasswordNotPersonal(RegisterRequest request) {
        String password = request.getPassword().toLowerCase(Locale.ROOT);
        String fullName = request.getFullName().trim().toLowerCase(Locale.ROOT);
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String emailLocalPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;

        if (password.equals(fullName)
                || password.equals(email)
                || password.equals(emailLocalPart)
                || (!fullName.isEmpty() && password.contains(fullName))) {
            throw new RuntimeException("Password cannot be the same as your name or email");
        }
    }
}
