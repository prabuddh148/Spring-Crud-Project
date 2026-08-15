package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.dto.AuthResponse;
import com.example.dto.LoginRequest;
import com.example.dto.RefreshTokenRequest;
import com.example.dto.RegisterRequest;
import com.example.dto.UserResponseDto;
import com.example.model.Role;
import com.example.model.User;
import com.example.repository.RoleRepository;
import com.example.repository.UserRepository;
import com.example.security.JwtService;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RoleRepository roleRepository;

    public UserResponseDto register(RegisterRequest registerRequest) {

        if (userRepository
                .findByEmail(registerRequest.email())
                .isPresent()) {

            throw new RuntimeException("Email already registered");
        }

        Role userRole = roleRepository
                .findByName("USER")
                .orElseThrow(
                        () -> new RuntimeException(
                                "USER role not found"));
        User user = new User();

        user.setName(registerRequest.name());
        user.setEmail(registerRequest.email());

        user.setPassword(
                passwordEncoder.encode(
                        registerRequest.password()));

        user.setRole(userRole);
        User savedUser = userRepository.save(user);

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail());
    }

    public AuthResponse login(LoginRequest loginRequest) {

        User user = userRepository
                .findByEmail(loginRequest.username())
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Invalid username or password"));

        boolean passwordMatches = passwordEncoder.matches(
                loginRequest.password(),
                user.getPassword());

        if (!passwordMatches) {

            throw new IllegalArgumentException(
                    "Invalid username or password");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getEmail());

        String refreshToken = jwtService.generateRefreshToken(
                user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken);
    }

    public AuthResponse refreshToken(
            RefreshTokenRequest refreshTokenRequest) {

        String email = jwtService.extractEmail(
                refreshTokenRequest.refreshToken());

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Invalid refresh token"));

        String newAccessToken = jwtService.generateAccessToken(
                user.getEmail());

        String newRefreshToken = jwtService.generateRefreshToken(
                user.getEmail());

        return new AuthResponse(
                newAccessToken,
                newRefreshToken);
    }
}