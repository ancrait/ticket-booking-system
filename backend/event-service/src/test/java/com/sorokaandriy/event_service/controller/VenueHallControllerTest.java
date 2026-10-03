package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.requests.HallRequest;
import com.sorokaandriy.event_service.dto.responses.HallResponse;
import com.sorokaandriy.event_service.exception.VenueNotFoundException;
import com.sorokaandriy.event_service.exception.handler.GlobalExceptionHandler;
import com.sorokaandriy.event_service.service.HallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VenueHallControllerTest {

    private static final UUID VENUE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HALL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private HallService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(HallService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new VenueHallController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private HallResponse hallResponse(int rowsCount, int seatsPerRow) {
        return HallResponse.builder()
                .id(HALL_ID)
                .name("Hall 1")
                .rowsCount(rowsCount)
                .seatsPerRow(seatsPerRow)
                .venueId(VENUE_ID)
                .build();
    }

    @Test
    void createHall_returnsCreatedHallOfVenue() throws Exception {
        when(service.createHall(eq(VENUE_ID), any(HallRequest.class))).thenReturn(hallResponse(5, 8));

        mockMvc.perform(post("/api/venues/{venueId}/halls", VENUE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hall 1","rowsCount":5,"seatsPerRow":8}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(HALL_ID.toString()))
                .andExpect(jsonPath("$.name").value("Hall 1"))
                .andExpect(jsonPath("$.rowsCount").value(5))
                .andExpect(jsonPath("$.seatsPerRow").value(8))
                .andExpect(jsonPath("$.venueId").value(VENUE_ID.toString()));

        ArgumentCaptor<HallRequest> captor = ArgumentCaptor.forClass(HallRequest.class);
        verify(service).createHall(eq(VENUE_ID), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Hall 1");
        assertThat(captor.getValue().rowsCount()).isEqualTo(5);
        assertThat(captor.getValue().seatsPerRow()).isEqualTo(8);
    }

    @Test
    void createHall_returns400WithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/venues/{venueId}/halls", VENUE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","rowsCount":0,"seatsPerRow":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").value("Hall name is required"))
                .andExpect(jsonPath("$.errors.rowsCount").value("Rows count must be greater than 0"))
                .andExpect(jsonPath("$.errors.seatsPerRow").value("Seats per row must be greater than 0"));

        verifyNoInteractions(service);
    }

    @Test
    void createHall_returns404WhenVenueDoesNotExist() throws Exception {
        when(service.createHall(eq(VENUE_ID), any(HallRequest.class)))
                .thenThrow(new VenueNotFoundException("Venue with id " + VENUE_ID + " not found"));

        mockMvc.perform(post("/api/venues/{venueId}/halls", VENUE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hall 1","rowsCount":5,"seatsPerRow":8}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Venue with id " + VENUE_ID + " not found"));
    }

    @Test
    void findHallsByVenueId_returnsHallsOfVenue() throws Exception {
        when(service.findHallsByVenueId(VENUE_ID))
                .thenReturn(List.of(hallResponse(5, 8), hallResponse(3, 4)));

        mockMvc.perform(get("/api/venues/{venueId}/halls", VENUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rowsCount").value(5))
                .andExpect(jsonPath("$[1].rowsCount").value(3))
                .andExpect(jsonPath("$[0].venueId").value(VENUE_ID.toString()));

        verify(service).findHallsByVenueId(VENUE_ID);
    }
}
