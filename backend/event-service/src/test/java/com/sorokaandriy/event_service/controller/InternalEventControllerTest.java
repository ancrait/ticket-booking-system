package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.responses.HeldSeatInfo;
import com.sorokaandriy.event_service.dto.responses.HeldSeatsResponse;
import com.sorokaandriy.event_service.exception.SeatsNotAvailableException;
import com.sorokaandriy.event_service.exception.handler.GlobalExceptionHandler;
import com.sorokaandriy.event_service.service.EventSeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalEventControllerTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FIRST_SEAT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SECOND_SEAT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private EventSeatService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(EventSeatService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new InternalEventController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private HeldSeatsResponse heldSeatsResponse() {
        return HeldSeatsResponse.builder()
                .eventTitle("Concert")
                .seats(List.of(
                        HeldSeatInfo.builder()
                                .eventSeatId(FIRST_SEAT_ID)
                                .rowNumber(1)
                                .seatNumber(2)
                                .sector("VIP")
                                .price(new BigDecimal("500.00"))
                                .build(),
                        HeldSeatInfo.builder()
                                .eventSeatId(SECOND_SEAT_ID)
                                .rowNumber(2)
                                .seatNumber(3)
                                .sector("A")
                                .price(new BigDecimal("350.00"))
                                .build()))
                .build();
    }

    @Test
    void holdSeats_returnsHeldSeats() throws Exception {
        when(service.holdSeats(eq(EVENT_ID), any())).thenReturn(heldSeatsResponse());
        mockMvc.perform(post("/api/internal/events/{eventId}/hold-seats", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds":["22222222-2222-2222-2222-222222222222",
                                            "33333333-3333-3333-3333-333333333333"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTitle").value("Concert"))
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andExpect(jsonPath("$.seats[0].eventSeatId").value(FIRST_SEAT_ID.toString()))
                .andExpect(jsonPath("$.seats[0].sector").value("VIP"))
                .andExpect(jsonPath("$.seats[0].price").value(500.00))
                .andExpect(jsonPath("$.seats[1].eventSeatId").value(SECOND_SEAT_ID.toString()))
                .andExpect(jsonPath("$.seats[1].price").value(350.00));
    }

    @Test
    void holdSeats_returns400WhenSeatIdsAreEmpty() throws Exception {
        mockMvc.perform(post("/api/internal/events/{eventId}/hold-seats", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.seatIds").value("Seat IDs list cannot be empty"));

        verifyNoInteractions(service);
    }

    @Test
    void holdSeats_returns409WhenSomeSeatsAreTaken() throws Exception {
        when(service.holdSeats(eq(EVENT_ID), any()))
                .thenThrow(new SeatsNotAvailableException("Some seats are already taken"));

        mockMvc.perform(post("/api/internal/events/{eventId}/hold-seats", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds":["22222222-2222-2222-2222-222222222222"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Some seats are already taken"));
    }

    @Test
    void releaseSeats_returns204WithoutBody() throws Exception {
        mockMvc.perform(post("/api/internal/events/{eventId}/release-seats", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds":["22222222-2222-2222-2222-222222222222"]}
                                """))
                .andExpect(status().isNoContent());

        verify(service).releaseSeats(eq(EVENT_ID), any());
    }

    @Test
    void releaseSeats_returns409WhenSeatIsSold() throws Exception {
        doThrow(new SeatsNotAvailableException("Some seats are already sold"))
                .when(service).releaseSeats(eq(EVENT_ID), any());

        mockMvc.perform(post("/api/internal/events/{eventId}/release-seats", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds":["22222222-2222-2222-2222-222222222222"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Some seats are already sold"));
    }

    @Test
    void confirmSeats_returns204WithoutBody() throws Exception {
        mockMvc.perform(post("/api/internal/events/{eventId}/confirm-seats", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds":["22222222-2222-2222-2222-222222222222"]}
                                """))
                .andExpect(status().isNoContent());

        verify(service).confirmSeats(eq(EVENT_ID), any());
    }

    @Test
    void confirmSeats_returns409WhenSeatsAreNotHeld() throws Exception {
        doThrow(new SeatsNotAvailableException("Some seats are not in HELD status"))
                .when(service).confirmSeats(eq(EVENT_ID), any());

        mockMvc.perform(post("/api/internal/events/{eventId}/confirm-seats", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seatIds":["22222222-2222-2222-2222-222222222222"]}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Some seats are not in HELD status"));
    }
}
