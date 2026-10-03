package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.requests.VenueRequest;
import com.sorokaandriy.event_service.dto.responses.VenueResponse;
import com.sorokaandriy.event_service.exception.VenueNotFoundException;
import com.sorokaandriy.event_service.exception.handler.GlobalExceptionHandler;
import com.sorokaandriy.event_service.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VenueControllerTest {

    private static final UUID VENUE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private VenueService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(VenueService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new VenueController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private VenueResponse venueResponse() {
        return VenueResponse.builder()
                .id(VENUE_ID)
                .name("Arena")
                .city("Kyiv")
                .address("Main St 1")
                .createdAt(CREATED_AT)
                .build();
    }

    @Test
    void createVenue_returnsCreatedVenue() throws Exception {
        when(service.createVenue(any(VenueRequest.class))).thenReturn(venueResponse());

        mockMvc.perform(post("/api/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Arena","city":"Kyiv","address":"Main St 1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$.name").value("Arena"))
                .andExpect(jsonPath("$.city").value("Kyiv"))
                .andExpect(jsonPath("$.address").value("Main St 1"))
                .andExpect(jsonPath("$.createdAt").value("2026-01-01T10:00:00Z"));

        ArgumentCaptor<VenueRequest> captor = ArgumentCaptor.forClass(VenueRequest.class);
        verify(service).createVenue(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Arena");
        assertThat(captor.getValue().city()).isEqualTo("Kyiv");
        assertThat(captor.getValue().address()).isEqualTo("Main St 1");
    }

    @Test
    void createVenue_returns400WithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","city":"","address":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.city").value("City is required"))
                .andExpect(jsonPath("$.errors.address").value("Address is required"));

        verifyNoInteractions(service);
    }

    @Test
    void findAllVenues_usesDefaultPagingAndReturnsVenuePage() throws Exception {
        when(service.findAllVenues(0, 10, "id", null))
                .thenReturn(new PageImpl<>(List.of(venueResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/venues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Arena"));

        verify(service).findAllVenues(0, 10, "id", null);
    }

    @Test
    void findAllVenues_passesCityFilterAndPagingParams() throws Exception {
        when(service.findAllVenues(1, 5, "name", "Kyiv"))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/venues")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "name")
                        .param("city", "Kyiv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(service).findAllVenues(1, 5, "name", "Kyiv");
    }

    @Test
    void findVenue_returnsVenue() throws Exception {
        when(service.findVenue(VENUE_ID)).thenReturn(venueResponse());

        mockMvc.perform(get("/api/venues/{id}", VENUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VENUE_ID.toString()))
                .andExpect(jsonPath("$.city").value("Kyiv"));
    }

    @Test
    void findVenue_returns404WhenVenueDoesNotExist() throws Exception {
        when(service.findVenue(VENUE_ID))
                .thenThrow(new VenueNotFoundException("Venue with id " + VENUE_ID + " not found"));

        mockMvc.perform(get("/api/venues/{id}", VENUE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Venue with id " + VENUE_ID + " not found"));
    }

    @Test
    void updateVenue_returnsUpdatedVenue() throws Exception {
        when(service.updateVenue(eq(VENUE_ID), any(VenueRequest.class))).thenReturn(venueResponse());

        mockMvc.perform(put("/api/venues/{id}", VENUE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Arena","city":"Kyiv","address":"Main St 1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(VENUE_ID.toString()));

        ArgumentCaptor<VenueRequest> captor = ArgumentCaptor.forClass(VenueRequest.class);
        verify(service).updateVenue(eq(VENUE_ID), captor.capture());
        assertThat(captor.getValue().address()).isEqualTo("Main St 1");
    }

    @Test
    void updateVenue_returns404WhenVenueDoesNotExist() throws Exception {
        when(service.updateVenue(eq(VENUE_ID), any(VenueRequest.class)))
                .thenThrow(new VenueNotFoundException("Venue with id " + VENUE_ID + " not found"));

        mockMvc.perform(put("/api/venues/{id}", VENUE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Arena","city":"Kyiv","address":"Main St 1"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Venue with id " + VENUE_ID + " not found"));
    }

    @Test
    void deleteVenue_returns204WithoutBody() throws Exception {
        mockMvc.perform(delete("/api/venues/{id}", VENUE_ID))
                .andExpect(status().isNoContent());

        verify(service).deleteVenue(VENUE_ID);
    }

    @Test
    void deleteVenue_returns404WhenVenueDoesNotExist() throws Exception {
        doThrow(new VenueNotFoundException("Venue with id " + VENUE_ID + " not found"))
                .when(service).deleteVenue(VENUE_ID);

        mockMvc.perform(delete("/api/venues/{id}", VENUE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
