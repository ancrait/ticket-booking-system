package com.sorokaandriy.auth_service.service;

import com.sorokaandriy.auth_service.dto.*;
import com.sorokaandriy.auth_service.entity.EmailVerificationToken;
import com.sorokaandriy.auth_service.entity.OutBox;
import com.sorokaandriy.auth_service.entity.User;
import com.sorokaandriy.auth_service.exception.*;
import com.sorokaandriy.auth_service.kafka.UserRegisteredEvent;
import com.sorokaandriy.auth_service.repository.EmailVerificationTokenRepository;
import com.sorokaandriy.auth_service.repository.OutBoxRepository;
import com.sorokaandriy.auth_service.repository.UserRepository;
import com.sorokaandriy.auth_service.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OutBoxRepository outBoxRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.verification-token-expiration}")
    private long verificationTokenExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.email().toLowerCase())) {
            throw new EmailAlreadyExistsException("User with email " + request.email() + " already exists");
        }

        User user = mapper.fromRegisterRequestToUser(request, passwordEncoder.encode(request.password()));
        user.setEmailVerified(false);
        User saved = repository.save(user);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .userId(saved.getId())
                .expiresAt(Instant.now().plusMillis(verificationTokenExpiration))
                .build();
        tokenRepository.save(verificationToken);

        UserRegisteredEvent registeredEvent = mapper.fromUserToUserRegisteredEvent(user, verificationToken.getToken());

        outBoxRepository.save(OutBox.builder()
                .aggregateId(String.valueOf(saved.getId()))
                .topic("user-registered-topic")
                .payload(serialize(registeredEvent))
                .build());

        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        User user = repository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException("Email is not verified. Please check your inbox.");
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

        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailNotVerifiedException("Email is not verified. Please check your inbox.");
        }

        return buildAuthResponse(user);
    }



    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidVerificationTokenException("Invalid verification token"));

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new VerificationTokenExpiredException("Verification token has expired");
        }

        User user = repository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setEmailVerified(true);
        repository.save(user);
        tokenRepository.delete(verificationToken);

        log.info("Email verified for user {}", user.getId());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = repository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email " + email + " not found"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailAlreadyVerifiedException("Email is already verified");
        }

        tokenRepository.findByUserId(user.getId()).ifPresent(tokenRepository::delete);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .userId(user.getId())
                .expiresAt(Instant.now().plusMillis(verificationTokenExpiration))
                .build();
        tokenRepository.save(verificationToken);

        UserRegisteredEvent registeredEvent = mapper.fromUserToUserRegisteredEvent(user, verificationToken.getToken());

        outBoxRepository.save(OutBox.builder()
                .aggregateId(String.valueOf(user.getId()))
                .topic("user-registered-topic")
                .payload(serialize(registeredEvent))
                .build());

        log.info("Verification email resent to user {}", user.getId());
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

    public AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpiration())
                .user(mapper.fromUserToUserResponse(user))
                .build();
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new PaymentProcessingException("Failed to serialize event", ex);
        }
    }
}
