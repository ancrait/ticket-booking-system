package com.sorokaandriy.booking_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.booking_service.dto.requests.BookingItemsRequest;
import com.sorokaandriy.booking_service.dto.requests.CreateBookingRequest;
import com.sorokaandriy.booking_service.dto.responses.BookingResponse;
import com.sorokaandriy.booking_service.entity.enumeration.BookingStatus;
import com.sorokaandriy.booking_service.exception.BookingNotFoundException;
import com.sorokaandriy.booking_service.exception.GlobalExceptionHandler;
import com.sorokaandriy.booking_service.exception.SeatHoldException;
import com.sorokaandriy.booking_service.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingControllerTest {

    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String BOOKING_ID = "33333333-3333-3333-3333-333333333333";
    private static final String EVENT_ID = "44444444-4444-4444-4444-444444444444";
    private static final String SEAT_ID = "55555555-5555-5555-5555-555555555555";
    private static final String EMAIL = "user@example.com";
    private static final String CREATE_BODY = """
            {"eventId":"%s","bookingItemsRequests":[{"eventSeatId":"%s"}]}
            """.formatted(EVENT_ID, SEAT_ID);

    private BookingService bookingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bookingService = mock(BookingService.class);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mockMvc = MockMvcBuilders.standaloneSetup(new BookingController(bookingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    private BookingResponse bookingResponse(BookingStatus status) {
        return BookingResponse.builder()
                .id(UUID.fromString(BOOKING_ID))
                .userId(UUID.fromString(USER_ID))
                .email(EMAIL)
                .eventId(UUID.fromString(EVENT_ID))
                .eventTitle("Rock Concert")
                .status(status)
                .totalPrice(new BigDecimal("300.00"))
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .expiresAt(Instant.parse("2026-01-01T10:10:00Z"))
                .build();
    }

    @Test
    void createBooking_returnsCreatedResponse() throws Exception {
        when(bookingService.createBooking(any(UUID.class), any(String.class), any(CreateBookingRequest.class)))
                .thenReturn(bookingResponse(BookingStatus.PENDING));

        mockMvc.perform(post("/api/bookings")
                        .header("X-User-Id", USER_ID)
                        .header("X-User-Email", EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(BOOKING_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                .andExpect(jsonPath("$.eventTitle").value("Rock Concert"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(300.00))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"));

        ArgumentCaptor<CreateBookingRequest> captor = ArgumentCaptor.forClass(CreateBookingRequest.class);
        verify(bookingService).createBooking(
                eq(UUID.fromString(USER_ID)), eq(EMAIL), captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo(UUID.fromString(EVENT_ID));
        assertThat(captor.getValue().bookingItemsRequests())
                .containsExactly(new BookingItemsRequest(UUID.fromString(SEAT_ID)));
    }

    @Test
    void createBooking_returns400WhenRequestBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("X-User-Id", USER_ID)
                        .header("X-User-Email", EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingItemsRequests\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.eventId").value("Event ID is required"))
                .andExpect(jsonPath("$.errors.bookingItemsRequests")
                        .value("Booking items list cannot be empty"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void createBooking_returns409WhenSeatHoldFails() throws Exception {
        when(bookingService.createBooking(any(UUID.class), any(String.class), any(CreateBookingRequest.class)))
                .thenThrow(new SeatHoldException("Unable to hold selected seats"));

        mockMvc.perform(post("/api/bookings")
                        .header("X-User-Id", USER_ID)
                        .header("X-User-Email", EMAIL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Unable to hold selected seats"));
    }

    @Test
    void findBookingById_returnsBooking() throws Exception {
        when(bookingService.findBookingById(UUID.fromString(BOOKING_ID), UUID.fromString(USER_ID)))
                .thenReturn(bookingResponse(BookingStatus.PAID));

        mockMvc.perform(get("/api/bookings/" + BOOKING_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BOOKING_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void findBookingById_returns404WhenBookingNotFound() throws Exception {
        when(bookingService.findBookingById(UUID.fromString(BOOKING_ID), UUID.fromString(USER_ID)))
                .thenThrow(new BookingNotFoundException("Booking with id " + BOOKING_ID + " not found"));

        mockMvc.perform(get("/api/bookings/" + BOOKING_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Booking with id " + BOOKING_ID + " not found"));
    }

    @Test
    void findMyBookings_returnsPageWithDefaultPagination() throws Exception {
        when(bookingService.findMyBookings(UUID.fromString(USER_ID), 0, 10, "id"))
                .thenReturn(new PageImpl<>(
                        List.of(bookingResponse(BookingStatus.PENDING)),
                        PageRequest.of(0, 10),
                        1));

        mockMvc.perform(get("/api/bookings/my")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(BOOKING_ID))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void cancelBooking_returnsCanceledBooking() throws Exception {
        when(bookingService.cancelBooking(UUID.fromString(USER_ID), UUID.fromString(BOOKING_ID)))
                .thenReturn(bookingResponse(BookingStatus.CANCELED));

        mockMvc.perform(patch("/api/bookings/" + BOOKING_ID + "/cancel")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BOOKING_ID))
                .andExpect(jsonPath("$.status").value("CANCELED"));

        verify(bookingService).cancelBooking(UUID.fromString(USER_ID), UUID.fromString(BOOKING_ID));
    }

    @Test
    void cancelBooking_returns403WhenBookingBelongsToAnotherUser() throws Exception {
        when(bookingService.cancelBooking(UUID.fromString(USER_ID), UUID.fromString(BOOKING_ID)))
                .thenThrow(new AccessDeniedException("Not your booking"));

        mockMvc.perform(patch("/api/bookings/" + BOOKING_ID + "/cancel")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Not your booking"));
    }
}
