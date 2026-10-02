package com.sorokaandriy.auth_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.auth_service.dto.AuthResponse;
import com.sorokaandriy.auth_service.dto.LoginRequest;
import com.sorokaandriy.auth_service.dto.RefreshRequest;
import com.sorokaandriy.auth_service.dto.RegisterRequest;
import com.sorokaandriy.auth_service.dto.UserResponse;
import com.sorokaandriy.auth_service.entity.EmailVerificationToken;
import com.sorokaandriy.auth_service.entity.OutBox;
import com.sorokaandriy.auth_service.entity.Role;
import com.sorokaandriy.auth_service.entity.User;
import com.sorokaandriy.auth_service.exception.EmailAlreadyExistsException;
import com.sorokaandriy.auth_service.exception.EmailAlreadyVerifiedException;
import com.sorokaandriy.auth_service.exception.EmailNotVerifiedException;
import com.sorokaandriy.auth_service.exception.InvalidCredentialsException;
import com.sorokaandriy.auth_service.exception.InvalidTokenException;
import com.sorokaandriy.auth_service.exception.InvalidVerificationTokenException;
import com.sorokaandriy.auth_service.exception.UserNotFoundException;
import com.sorokaandriy.auth_service.exception.VerificationTokenExpiredException;
import com.sorokaandriy.auth_service.kafka.UserRegisteredEvent;
import com.sorokaandriy.auth_service.repository.EmailVerificationTokenRepository;
import com.sorokaandriy.auth_service.repository.OutBoxRepository;
import com.sorokaandriy.auth_service.repository.UserRepository;
import com.sorokaandriy.auth_service.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String ACCESS_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String REFRESH_SECRET = "fedcba9876543210fedcba9876543210fedcba9876543210";
    private static final long ACCESS_EXPIRATION = 900_000L;
    private static final long REFRESH_EXPIRATION = 604_800_000L;
    private static final long VERIFICATION_TOKEN_EXPIRATION_MS = 3_600_000L;
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String RAW_PASSWORD = "secret123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private OutBoxRepository outBoxRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private ObjectMapper objectMapper;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(ACCESS_SECRET, REFRESH_SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        authService = new AuthService(
                userRepository,
                tokenRepository,
                new UserMapper(),
                passwordEncoder,
                jwtService,
                outBoxRepository,
                objectMapper);
        ReflectionTestUtils.setField(authService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(authService, "verificationTokenExpiration", VERIFICATION_TOKEN_EXPIRATION_MS);
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest("John", "Doe", "John@Example.COM", RAW_PASSWORD, "+380501234567");
    }

    private User user(boolean emailVerified) {
        return User.builder()
                .id(USER_ID)
                .email("john@example.com")
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .firstName("John")
                .lastName("Doe")
                .phone("+380501234567")
                .role(Role.USER)
                .emailVerified(emailVerified)
                .createdAt(Instant.now())
                .build();
    }

    private EmailVerificationToken verificationToken(Instant expiresAt) {
        return EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .token("token-" + UUID.randomUUID())
                .userId(USER_ID)
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void register_savesUserTokenAndOutboxEvent() throws Exception {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(USER_ID);
            return saved;
        });

        AuthResponse response = authService.register(registerRequest());

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(ACCESS_EXPIRATION);
        assertThat(jwtService.parseAccessToken(response.accessToken()).getSubject())
                .isEqualTo(USER_ID.toString());
        assertThat(jwtService.parseRefreshToken(response.refreshToken()).get("type", String.class))
                .isEqualTo("REFRESH");
        assertThat(response.user().id()).isEqualTo(USER_ID.toString());
        assertThat(response.user().email()).isEqualTo("john@example.com");
        assertThat(response.user().role()).isEqualTo(Role.USER);
        assertThat(response.user().emailVerified()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("john@example.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getPassword()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, savedUser.getPassword())).isTrue();
        assertThat(savedUser.getEmailVerified()).isFalse();

        ArgumentCaptor<EmailVerificationToken> tokenCaptor =
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        EmailVerificationToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.getUserId()).isEqualTo(USER_ID);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());

        ArgumentCaptor<OutBox> outBoxCaptor = ArgumentCaptor.forClass(OutBox.class);
        verify(outBoxRepository).save(outBoxCaptor.capture());
        OutBox outBox = outBoxCaptor.getValue();
        assertThat(outBox.getTopic()).isEqualTo("user-registered-topic");
        assertThat(outBox.getAggregateId()).isEqualTo(USER_ID.toString());
        UserRegisteredEvent event = objectMapper.readValue(outBox.getPayload(), UserRegisteredEvent.class);
        assertThat(event.userId()).isEqualTo(USER_ID.toString());
        assertThat(event.email()).isEqualTo("john@example.com");
        assertThat(event.verificationToken()).isEqualTo(savedToken.getToken());
        assertThat(event.registeredAt()).isNotNull();
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
        verifyNoInteractions(tokenRepository, outBoxRepository);
    }

    @Test
    void login_returnsTokensForVerifiedUserWithCorrectPassword() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user(true)));

        AuthResponse response = authService.login(new LoginRequest("John@Example.COM", RAW_PASSWORD));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(jwtService.parseAccessToken(response.accessToken()).getSubject())
                .isEqualTo(USER_ID.toString());
        assertThat(response.user().id()).isEqualTo(USER_ID.toString());
        assertThat(response.user().emailVerified()).isTrue();
    }

    @Test
    void login_throwsWhenEmailNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_throwsWhenPasswordDoesNotMatch() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user(true)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_throwsWhenEmailNotVerified() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user(false)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", RAW_PASSWORD)))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessage("Email is not verified. Please check your inbox.");
    }

    @Test
    void refresh_returnsNewTokensForValidRefreshToken() {
        User user = user(true);
        String refreshToken = jwtService.generateRefreshToken(user);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh(new RefreshRequest(refreshToken));

        assertThat(jwtService.parseAccessToken(response.accessToken()).getSubject())
                .isEqualTo(USER_ID.toString());
        assertThat(jwtService.parseRefreshToken(response.refreshToken()).get("type", String.class))
                .isEqualTo("REFRESH");
        assertThat(response.user().id()).isEqualTo(USER_ID.toString());
    }

    @Test
    void refresh_throwsForInvalidToken() {
        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("invalid-token")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token is invalid or expired");
    }

    @Test
    void refresh_throwsWhenTokenIsNotRefreshType() {
        String notRefreshToken = Jwts.builder()
                .subject(USER_ID.toString())
                .claim("type", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(notRefreshToken)))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Provided token is not a refresh token");
    }

    @Test
    void refresh_throwsWhenUserNotFound() {
        String refreshToken = jwtService.generateRefreshToken(user(true));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(refreshToken)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());
    }

    @Test
    void refresh_throwsWhenEmailNotVerified() {
        User user = user(false);
        String refreshToken = jwtService.generateRefreshToken(user);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(refreshToken)))
                .isInstanceOf(EmailNotVerifiedException.class);
    }

    @Test
    void verifyEmail_marksUserVerifiedAndDeletesToken() {
        EmailVerificationToken token = verificationToken(Instant.now().plusSeconds(600));
        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        User user = user(false);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        authService.verifyEmail(token.getToken());

        assertThat(user.getEmailVerified()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).delete(token);
    }

    @Test
    void verifyEmail_throwsWhenTokenNotFound() {
        when(tokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("missing"))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessage("Invalid verification token");

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    void verifyEmail_throwsWhenTokenExpired() {
        EmailVerificationToken token = verificationToken(Instant.now().minusSeconds(60));
        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.verifyEmail(token.getToken()))
                .isInstanceOf(VerificationTokenExpiredException.class)
                .hasMessage("Verification token has expired");

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).delete(any());
    }

    @Test
    void verifyEmail_throwsWhenUserNotFound() {
        EmailVerificationToken token = verificationToken(Instant.now().plusSeconds(600));
        when(tokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail(token.getToken()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void resendVerificationEmail_replacesTokenAndWritesOutbox() throws Exception {
        User user = user(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        EmailVerificationToken oldToken = verificationToken(Instant.now().plusSeconds(600));
        when(tokenRepository.findByUserId(USER_ID)).thenReturn(Optional.of(oldToken));

        authService.resendVerificationEmail("john@example.com");

        verify(tokenRepository).delete(oldToken);

        ArgumentCaptor<EmailVerificationToken> tokenCaptor =
                ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        EmailVerificationToken newToken = tokenCaptor.getValue();
        assertThat(newToken.getToken()).isNotBlank().isNotEqualTo(oldToken.getToken());
        assertThat(newToken.getUserId()).isEqualTo(USER_ID);
        assertThat(newToken.getExpiresAt()).isAfter(Instant.now());

        ArgumentCaptor<OutBox> outBoxCaptor = ArgumentCaptor.forClass(OutBox.class);
        verify(outBoxRepository).save(outBoxCaptor.capture());
        OutBox outBox = outBoxCaptor.getValue();
        assertThat(outBox.getTopic()).isEqualTo("user-registered-topic");
        UserRegisteredEvent event = objectMapper.readValue(outBox.getPayload(), UserRegisteredEvent.class);
        assertThat(event.verificationToken()).isEqualTo(newToken.getToken());
        assertThat(event.email()).isEqualTo("john@example.com");
    }

    @Test
    void resendVerificationEmail_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resendVerificationEmail("unknown@example.com"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("unknown@example.com");

        verifyNoInteractions(tokenRepository, outBoxRepository);
    }

    @Test
    void resendVerificationEmail_throwsWhenAlreadyVerified() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user(true)));

        assertThatThrownBy(() -> authService.resendVerificationEmail("john@example.com"))
                .isInstanceOf(EmailAlreadyVerifiedException.class)
                .hasMessage("Email is already verified");

        verifyNoInteractions(tokenRepository, outBoxRepository);
    }

    @Test
    void getCurrentUser_returnsUserResponse() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user(true)));

        UserResponse response = authService.getCurrentUser(USER_ID.toString());

        assertThat(response.id()).isEqualTo(USER_ID.toString());
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.emailVerified()).isTrue();
    }

    @Test
    void getCurrentUser_throwsWhenUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(USER_ID.toString()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());
    }

    @Test
    void findAll_mapsPageOfUsers() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(true))));

        Page<UserResponse> result = authService.findAll(0, 10, "createdAt");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().email()).isEqualTo("john@example.com");
        assertThat(result.getContent().getFirst().id()).isEqualTo(USER_ID.toString());
    }
}
