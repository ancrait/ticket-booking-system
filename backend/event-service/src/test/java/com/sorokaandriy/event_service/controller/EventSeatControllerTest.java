package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.responses.EventSeatResponse;
import com.sorokaandriy.event_service.entity.enumeration.EventSeatStatus;
import com.sorokaandriy.event_service.service.EventSeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventSeatControllerTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SEAT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private EventSeatService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(EventSeatService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EventSeatController(service)).build();
    }

    private EventSeatResponse seatResponse() {
        return EventSeatResponse.builder()
                .id(SEAT_ID)
                .seatId(SEAT_ID)
                .rowNumber(2)
                .seatNumber(7)
                .sector("A")
                .price(new BigDecimal("350.00"))
                .status(EventSeatStatus.FREE)
                .build();
    }

    @Test
    void findAllEventSeats_usesDefaultPagingAndReturnsSeatPage() throws Exception {
        when(service.findAllEventSeats(0, 10, "id", EVENT_ID))
                .thenReturn(new PageImpl<>(List.of(seatResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/events/{eventId}/seats", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].seatId").value(SEAT_ID.toString()))
                .andExpect(jsonPath("$.content[0].rowNumber").value(2))
                .andExpect(jsonPath("$.content[0].sector").value("A"))
                .andExpect(jsonPath("$.content[0].price").value(350.00))
                .andExpect(jsonPath("$.content[0].status").value("FREE"));

        verify(service).findAllEventSeats(0, 10, "id", EVENT_ID);
    }

    @Test
    void findAllEventSeats_passesPagingQueryParams() throws Exception {
        when(service.findAllEventSeats(2, 5, "price", EVENT_ID))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/api/events/{eventId}/seats", EVENT_ID)
                        .param("page", "2")
                        .param("size", "5")
                        .param("sortBy", "price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(service).findAllEventSeats(2, 5, "price", EVENT_ID);
    }

    @Test
    void findAvailableEventSeats_returnsOnlyFreeSeats() throws Exception {
        when(service.findAvailableEventSeats(0, 10, "id", EVENT_ID))
                .thenReturn(new PageImpl<>(List.of(seatResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/events/{eventId}/seats/available", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("FREE"));

        verify(service).findAvailableEventSeats(0, 10, "id", EVENT_ID);
    }
}
