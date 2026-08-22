package com.sorokaandriy.auth_service.service;

import com.sorokaandriy.auth_service.dto.*;
import com.sorokaandriy.auth_service.entity.User;
import com.sorokaandriy.auth_service.exception.EmailAlreadyExistsException;
import com.sorokaandriy.auth_service.exception.InvalidCredentialsException;
import com.sorokaandriy.auth_service.exception.InvalidTokenException;
import com.sorokaandriy.auth_service.exception.UserNotFoundException;
import com.sorokaandriy.auth_service.kafka.UserEventProducer;
import com.sorokaandriy.auth_service.kafka.UserRegisteredEvent;
import com.sorokaandriy.auth_service.repository.UserRepository;
import com.sorokaandriy.auth_service.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserEventProducer eventProducer;

    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.email().toLowerCase())) {
            throw new EmailAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        User user = mapper.fromRegisterRequestToUser(request, passwordEncoder.encode(request.password()));
        User saved = repository.save(user);

        eventProducer.sendUserRegisteredEvent(UserRegisteredEvent.builder()
                .userId(saved.getId().toString())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .registeredAt(saved.getCreatedAt())
                .build());

        return buildAuthResponse(saved);

    }


    public AuthResponse login(LoginRequest request) {
        User user = repository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return buildAuthResponse(user);

    }

    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseRefreshToken(request.refreshToken());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        if (!"REFRESH".equals(claims.get("type", String.class))) {
            throw new InvalidTokenException("Provided token is not a refresh token");
        }

        User user = repository.findById(UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> new UserNotFoundException("User with id " + claims.getSubject() + " not found"));

        return buildAuthResponse(user);
    }



    public UserResponse getCurrentUser(String userId) {
        User user = repository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));
        return mapper.fromUserToUserResponse(user);
    }

    public Page<UserResponse> findAll(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return repository.findAll(pageable).map(mapper::fromUserToUserResponse);
    }


    public AuthResponse buildAuthResponse(User user){

        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpiration())
                .user(mapper.fromUserToUserResponse(user))
                .build();
    }
}
