package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.requests.HallRequest;
import com.sorokaandriy.event_service.dto.responses.HallResponse;
import com.sorokaandriy.event_service.exception.HallNotFoundException;
import com.sorokaandriy.event_service.exception.handler.GlobalExceptionHandler;
import com.sorokaandriy.event_service.service.HallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HallControllerTest {

    private static final UUID HALL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private HallService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(HallService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new HallController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private HallResponse hallResponse() {
        return HallResponse.builder()
                .id(HALL_ID)
                .name("Hall 1")
                .rowsCount(5)
                .seatsPerRow(8)
                .venueId(VENUE_ID)
                .build();
    }

    @Test
    void findHallById_returnsHall() throws Exception {
        when(service.findHallById(HALL_ID)).thenReturn(hallResponse());

        mockMvc.perform(get("/api/halls/{id}", HALL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(HALL_ID.toString()))
                .andExpect(jsonPath("$.name").value("Hall 1"))
                .andExpect(jsonPath("$.rowsCount").value(5))
                .andExpect(jsonPath("$.seatsPerRow").value(8))
                .andExpect(jsonPath("$.venueId").value(VENUE_ID.toString()));
    }

    @Test
    void findHallById_returns404WhenHallDoesNotExist() throws Exception {
        when(service.findHallById(HALL_ID))
                .thenThrow(new HallNotFoundException("Hall with id " + HALL_ID + " not found"));

        mockMvc.perform(get("/api/halls/{id}", HALL_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Hall with id " + HALL_ID + " not found"));
    }

    @Test
    void updateHall_returnsUpdatedHall() throws Exception {
        when(service.updateHall(eq(HALL_ID), any(HallRequest.class))).thenReturn(hallResponse());

        mockMvc.perform(put("/api/halls/{id}", HALL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed","rowsCount":7,"seatsPerRow":9}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(HALL_ID.toString()));

        ArgumentCaptor<HallRequest> captor = ArgumentCaptor.forClass(HallRequest.class);
        verify(service).updateHall(eq(HALL_ID), captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Renamed");
        assertThat(captor.getValue().rowsCount()).isEqualTo(7);
        assertThat(captor.getValue().seatsPerRow()).isEqualTo(9);
    }

    @Test
    void updateHall_returns404WhenHallDoesNotExist() throws Exception {
        when(service.updateHall(eq(HALL_ID), any(HallRequest.class)))
                .thenThrow(new HallNotFoundException("Hall with id " + HALL_ID + " not found"));

        mockMvc.perform(put("/api/halls/{id}", HALL_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed","rowsCount":7,"seatsPerRow":9}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Hall with id " + HALL_ID + " not found"));
    }

    @Test
    void deleteHall_returns204WithoutBody() throws Exception {
        mockMvc.perform(delete("/api/halls/{id}", HALL_ID))
                .andExpect(status().isNoContent());

        verify(service).deleteHall(HALL_ID);
    }

    @Test
    void deleteHall_returns404WhenHallDoesNotExist() throws Exception {
        doThrow(new HallNotFoundException("Hall with id " + HALL_ID + " not found"))
                .when(service).deleteHall(HALL_ID);

        mockMvc.perform(delete("/api/halls/{id}", HALL_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
