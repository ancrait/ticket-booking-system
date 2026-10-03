package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.responses.SeatResponse;
import com.sorokaandriy.event_service.exception.HallNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsAlreadyGeneratedException;
import com.sorokaandriy.event_service.exception.handler.GlobalExceptionHandler;
import com.sorokaandriy.event_service.service.HallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeatControllerTest {

    private static final UUID HALL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SEAT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private HallService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(HallService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SeatController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private SeatResponse seatResponse(String sector) {
        return SeatResponse.builder()
                .id(SEAT_ID)
                .rowNumber(1)
                .seatNumber(2)
                .sector(sector)
                .hallId(HALL_ID)
                .build();
    }

    @Test
    void generateSeats_returnsCreatedSeats() throws Exception {
        when(service.generateSeats(HALL_ID))
                .thenReturn(List.of(seatResponse("VIP"), seatResponse("VIP")));

        mockMvc.perform(post("/api/halls/{hallId}/seats", HALL_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sector").value("VIP"))
                .andExpect(jsonPath("$[0].rowNumber").value(1))
                .andExpect(jsonPath("$[0].seatNumber").value(2))
                .andExpect(jsonPath("$[0].hallId").value(HALL_ID.toString()));

        verify(service).generateSeats(HALL_ID);
    }

    @Test
    void generateSeats_returns404WhenHallDoesNotExist() throws Exception {
        when(service.generateSeats(HALL_ID))
                .thenThrow(new HallNotFoundException("Hall with id " + HALL_ID + " not found"));

        mockMvc.perform(post("/api/halls/{hallId}/seats", HALL_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Hall with id " + HALL_ID + " not found"));
    }

    @Test
    void generateSeats_returns409WhenSeatsAreAlreadyGenerated() throws Exception {
        when(service.generateSeats(HALL_ID)).thenThrow(new SeatsAlreadyGeneratedException("Seats already exist"));

        mockMvc.perform(post("/api/halls/{hallId}/seats", HALL_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Seats already exist"));
    }

    @Test
    void findSeatsByHallId_usesDefaultPagingAndReturnsSeatPage() throws Exception {
        when(service.findSeatsByHallId(HALL_ID, 0, 10, "id"))
                .thenReturn(new PageImpl<>(List.of(seatResponse("VIP")), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/halls/{hallId}/seats", HALL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(SEAT_ID.toString()))
                .andExpect(jsonPath("$.content[0].sector").value("VIP"));

        verify(service).findSeatsByHallId(HALL_ID, 0, 10, "id");
    }

    @Test
    void findSeatsByHallId_passesPagingQueryParams() throws Exception {
        when(service.findSeatsByHallId(HALL_ID, 1, 5, "rowNumber"))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/halls/{hallId}/seats", HALL_ID)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sortBy", "rowNumber"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(service).findSeatsByHallId(HALL_ID, 1, 5, "rowNumber");
    }
}
