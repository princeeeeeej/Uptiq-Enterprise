package com.uptiq.api.service;

import com.uptiq.api.dto.SigninRequest;
import com.uptiq.api.dto.SignupRequest;
import com.uptiq.api.dto.UserResponse;
import com.uptiq.api.exception.DuplicateUsernameException;
import com.uptiq.api.exception.InvalidCredentialsException;
import com.uptiq.api.exception.ResourceNotFoundException;
import com.uptiq.api.security.JwtUtil;
import com.uptiq.entity.User;
import com.uptiq.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public UUID signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUsernameException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        return saved.getId();
    }

    public String signin(SigninRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new InvalidCredentialsException("Invalid credentials");
        }

        return jwtUtil.generateToken(user.getId());
    }

    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return new UserResponse(user.getId(), user.getEmail());
    }
}