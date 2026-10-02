package com.sorokaandriy.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.auth_service.dto.AuthResponse;
import com.sorokaandriy.auth_service.dto.RefreshRequest;
import com.sorokaandriy.auth_service.dto.RegisterRequest;
import com.sorokaandriy.auth_service.dto.UserResponse;
import com.sorokaandriy.auth_service.entity.Role;
import com.sorokaandriy.auth_service.exception.EmailAlreadyExistsException;
import com.sorokaandriy.auth_service.exception.GlobalExceptionHandler;
import com.sorokaandriy.auth_service.exception.InvalidCredentialsException;
import com.sorokaandriy.auth_service.exception.UserNotFoundException;
import com.sorokaandriy.auth_service.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String REGISTER_BODY = """
            {"firstName":"John","lastName":"Doe","email":"john@example.com",
             "password":"secret123","phone":"+380501234567"}
            """;
    private static final String LOGIN_BODY = """
            {"email":"john@example.com","password":"secret123"}
            """;

    private AuthService authService;
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("+380501234567")
                .role(Role.USER)
                .emailVerified(true)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    private AuthResponse authResponse() {
        return AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(900_000L)
                .user(userResponse())
                .build();
    }

    @Test
    void register_returnsCreatedResponse() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"))
                .andExpect(jsonPath("$.user.role").value("USER"));

        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().firstName()).isEqualTo("John");
        assertThat(captor.getValue().email()).isEqualTo("john@example.com");
        assertThat(captor.getValue().password()).isEqualTo("secret123");
    }

    @Test
    void register_returns400WhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Doe","email":"not-an-email",
                                 "password":"123","phone":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.firstName").value("First name is required"))
                .andExpect(jsonPath("$.errors.email").value("Email format is invalid"))
                .andExpect(jsonPath("$.errors.password")
                        .value("Password must be at least 6 characters long"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_returns409WhenEmailAlreadyExists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException(
                        "User with email john@example.com already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("User with email john@example.com already exists"));
    }

    @Test
    void login_returnsAuthResponse() throws Exception {
        when(authService.login(any())).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.id").value(USER_ID));
    }

    @Test
    void login_returns401ForInvalidCredentials() throws Exception {
        when(authService.login(any()))
                .thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void refresh_returnsAuthResponse() throws Exception {
        when(authService.refresh(any(RefreshRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"refresh-token-value\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));

        verify(authService).refresh(new RefreshRequest("refresh-token-value"));
    }

    @Test
    void verifyEmail_returns204() throws Exception {
        mockMvc.perform(get("/api/auth/verify-email").param("token", "abc-123"))
                .andExpect(status().isNoContent());

        verify(authService).verifyEmail("abc-123");
    }

    @Test
    void resendVerification_returns204AndLowercasesEmail() throws Exception {
        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"John@Example.COM\"}"))
                .andExpect(status().isNoContent());

        verify(authService).resendVerificationEmail("john@example.com");
    }

    @Test
    void currentUser_returnsUserResponse() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null));
        when(authService.getCurrentUser(USER_ID)).thenReturn(userResponse());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    void currentUser_returns404WhenUserNotFound() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null));
        when(authService.getCurrentUser(USER_ID)).thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
