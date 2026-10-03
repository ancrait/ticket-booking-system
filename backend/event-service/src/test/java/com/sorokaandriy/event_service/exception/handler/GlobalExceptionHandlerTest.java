package com.sorokaandriy.event_service.exception.handler;

import com.sorokaandriy.event_service.dto.errors.ErrorResponseDto;
import com.sorokaandriy.event_service.dto.errors.ErrorValidationResponse;
import com.sorokaandriy.event_service.exception.EventNotFoundException;
import com.sorokaandriy.event_service.exception.HallNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsAlreadyGeneratedException;
import com.sorokaandriy.event_service.exception.SeatsNotAvailableException;
import com.sorokaandriy.event_service.exception.VenueNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    void sampleHandler(Object payload) {
    }

    @Test
    void handleValidation_collectsFieldErrorsInto400Response() throws Exception {
        MethodParameter parameter = new MethodParameter(
                getClass().getDeclaredMethod("sampleHandler", Object.class), 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "payload");
        bindingResult.addError(new FieldError("payload", "title", "Title is required"));
        bindingResult.addError(new FieldError("payload", "hallId", "Hall ID is required"));

        ResponseEntity<ErrorValidationResponse> response =
                handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getBody().getErrors())
                .containsEntry("title", "Title is required")
                .containsEntry("hallId", "Hall ID is required");
        assertThat(response.getBody().getInstant()).isNotNull();
    }

    @Test
    void handleAuthentication_mapsBadCredentialsTo401() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleAuthentication(new BadCredentialsException("Invalid credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials");
        assertThat(response.getBody().getInstant()).isNotNull();
    }

    @Test
    void handleAuthentication_mapsAccessDeniedTo403() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleAuthentication(new AccessDeniedException("Access denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    void handleNotFound_mapsEveryNotFoundExceptionTo404() {
        assertNotFound(new EventNotFoundException("Event with id 1 not found"));
        assertNotFound(new HallNotFoundException("Hall with id 2 not found"));
        assertNotFound(new VenueNotFoundException("Venue with id 3 not found"));
    }

    private void assertNotFound(RuntimeException exception) {
        ResponseEntity<ErrorResponseDto> response = handler.handleNotFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo(exception.getMessage());
        assertThat(response.getBody().getInstant()).isNotNull();
    }

    @Test
    void handleConflict_mapsSeatConflictsAndOptimisticLockTo409() {
        assertConflict(new SeatsNotAvailableException("Some seats are already taken"));
        assertConflict(new SeatsAlreadyGeneratedException("Seats already exist"));
        assertConflict(new OptimisticLockingFailureException("Concurrent modification"));
    }

    private void assertConflict(RuntimeException exception) {
        ResponseEntity<ErrorResponseDto> response = handler.handleConflict(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo(exception.getMessage());
    }

    @Test
    void handleIllegalState_mapsTo400() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleIllegalState(new IllegalStateException("Only draft event can be published"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Only draft event can be published");
    }

    @Test
    void handleAll_mapsUnexpectedExceptionTo500() {
        ResponseEntity<ErrorResponseDto> response =
                handler.handleAll(new RuntimeException("Something broke"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Something broke");
        assertThat(response.getBody().getInstant()).isNotNull();
    }
}
