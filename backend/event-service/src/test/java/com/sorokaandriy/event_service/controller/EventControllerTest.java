package com.sorokaandriy.event_service.controller;

import com.sorokaandriy.event_service.dto.requests.EventRequest;
import com.sorokaandriy.event_service.dto.responses.EventResponse;
import com.sorokaandriy.event_service.entity.enumeration.EventStatus;
import com.sorokaandriy.event_service.exception.EventNotFoundException;
import com.sorokaandriy.event_service.exception.HallNotFoundException;
import com.sorokaandriy.event_service.exception.SeatsAlreadyGeneratedException;
import com.sorokaandriy.event_service.exception.handler.GlobalExceptionHandler;
import com.sorokaandriy.event_service.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HALL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID VENUE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ORGANIZER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant STARTS_AT = Instant.now()
            .plus(Duration.ofDays(30))
            .truncatedTo(ChronoUnit.MINUTES)
            .plusSeconds(45);
    private static final Instant ENDS_AT = STARTS_AT.plus(Duration.ofHours(3));

    private static final String VALID_BODY = """
            {"title":"Concert","description":"Live show","posterUrl":"http://example.com/poster.png",
             "startsAt":"%s","endsAt":"%s","hallId":"%s"}
            """.formatted(STARTS_AT, ENDS_AT, HALL_ID);

    private EventService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(EventService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EventController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private EventResponse eventResponse(EventStatus status) {
        return EventResponse.builder()
                .id(EVENT_ID)
                .title("Concert")
                .description("Live show")
                .posterUrl("http://example.com/poster.png")
                .startsAt(STARTS_AT)
                .endsAt(ENDS_AT)
                .status(status)
                .organizerId(ORGANIZER_ID)
                .hallId(HALL_ID)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    private PageImpl<EventResponse> pageOf(EventResponse response) {
        return new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1);
    }

    @Test
    void createEvent_returnsEventAndReadsOrganizerIdFromHeader() throws Exception {
        when(service.createEvent(any(EventRequest.class), eq(ORGANIZER_ID)))
                .thenReturn(eventResponse(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events")
                        .header("X-User-Id", ORGANIZER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.title").value("Concert"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.startsAt").value(STARTS_AT.toString()))
                .andExpect(jsonPath("$.hallId").value(HALL_ID.toString()));

        ArgumentCaptor<EventRequest> captor = ArgumentCaptor.forClass(EventRequest.class);
        verify(service).createEvent(captor.capture(), eq(ORGANIZER_ID));
        assertThat(captor.getValue().title()).isEqualTo("Concert");
        assertThat(captor.getValue().hallId()).isEqualTo(HALL_ID);
        assertThat(captor.getValue().startsAt()).isEqualTo(STARTS_AT);
    }

    @Test
    void createEvent_returns400WithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header("X-User-Id", ORGANIZER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","startsAt":"2020-01-01T00:00:00Z","endsAt":"2020-01-01T01:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.title").value("Title is required"))
                .andExpect(jsonPath("$.errors.startsAt").value("Start time must be in the future"))
                .andExpect(jsonPath("$.errors.endsAt").value("End time must be in the future"))
                .andExpect(jsonPath("$.errors.hallId").value("Hall ID is required"))
                .andExpect(jsonPath("$.instant").isNotEmpty());

        verifyNoInteractions(service);
    }

    @Test
    void createEvent_returns404WhenHallDoesNotExist() throws Exception {
        when(service.createEvent(any(EventRequest.class), eq(ORGANIZER_ID)))
                .thenThrow(new HallNotFoundException("Hall with id " + HALL_ID + " not found"));

        mockMvc.perform(post("/api/events")
                        .header("X-User-Id", ORGANIZER_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Hall with id " + HALL_ID + " not found"));
    }

    @Test
    void findAllEvents_usesDefaultPagingAndSorting() throws Exception {
        when(service.findAllEvent(0, 10, "id")).thenReturn(pageOf(eventResponse(EventStatus.PUBLISHED)));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10));

        verify(service).findAllEvent(0, 10, "id");
    }

    @Test
    void findAllEvents_passesPagingQueryParams() throws Exception {
        when(service.findAllEvent(anyInt(), anyInt(), anyString()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/api/events").param("page", "2").param("size", "5").param("sortBy", "startsAt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        verify(service).findAllEvent(2, 5, "startsAt");
    }

    @Test
    void findPublishedEventByCityAndDate_parsesIsoDate() throws Exception {
        when(service.findPublishedEventByCityAndDate(eq("Kyiv"), eq(LocalDate.of(2026, 5, 1)),
                anyInt(), anyInt(), anyString()))
                .thenReturn(pageOf(eventResponse(EventStatus.PUBLISHED)));

        mockMvc.perform(get("/api/events/sortedBy")
                        .param("city", "Kyiv")
                        .param("date", "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Concert"));

        verify(service).findPublishedEventByCityAndDate("Kyiv", LocalDate.of(2026, 5, 1), 0, 10, "id");
    }

    @Test
    void findPublishedEventByCityAndDate_passesNullsWhenFiltersAreMissing() throws Exception {
        when(service.findPublishedEventByCityAndDate(isNull(), isNull(), anyInt(), anyInt(), anyString()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/events/sortedBy"))
                .andExpect(status().isOk());

        verify(service).findPublishedEventByCityAndDate(null, null, 0, 10, "id");
    }

    @Test
    void findPublishedEventsByVenue_returnsPageForVenue() throws Exception {
        when(service.findPublishedEventsByVenue(VENUE_ID, 0, 10, "id"))
                .thenReturn(pageOf(eventResponse(EventStatus.PUBLISHED)));

        mockMvc.perform(get("/api/events/by-venue/{venueId}", VENUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].hallId").value(HALL_ID.toString()));

        verify(service).findPublishedEventsByVenue(VENUE_ID, 0, 10, "id");
    }

    @Test
    void findEventById_returnsEvent() throws Exception {
        when(service.findEventById(EVENT_ID)).thenReturn(eventResponse(EventStatus.PUBLISHED));

        mockMvc.perform(get("/api/events/{id}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.description").value("Live show"))
                .andExpect(jsonPath("$.endsAt").value(ENDS_AT.toString()))
                .andExpect(jsonPath("$.organizerId").value(ORGANIZER_ID.toString()));
    }

    @Test
    void findEventById_returns404WhenEventDoesNotExist() throws Exception {
        when(service.findEventById(EVENT_ID))
                .thenThrow(new EventNotFoundException("Event with id " + EVENT_ID + " not found"));

        mockMvc.perform(get("/api/events/{id}", EVENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event with id " + EVENT_ID + " not found"));
    }

    @Test
    void updateEvent_returnsUpdatedEvent() throws Exception {
        when(service.updateEvent(eq(EVENT_ID), any(EventRequest.class)))
                .thenReturn(eventResponse(EventStatus.DRAFT));

        mockMvc.perform(put("/api/events/{id}", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Concert"));

        ArgumentCaptor<EventRequest> captor = ArgumentCaptor.forClass(EventRequest.class);
        verify(service).updateEvent(eq(EVENT_ID), captor.capture());
        assertThat(captor.getValue().endsAt()).isEqualTo(ENDS_AT);
    }

    @Test
    void publishEvent_returnsPublishedEvent() throws Exception {
        when(service.publishEvent(EVENT_ID)).thenReturn(eventResponse(EventStatus.PUBLISHED));

        mockMvc.perform(post("/api/events/{id}/publish", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishEvent_returns409WhenSeatsAreAlreadyGenerated() throws Exception {
        when(service.publishEvent(EVENT_ID))
                .thenThrow(new SeatsAlreadyGeneratedException("Event seats already generated for event " + EVENT_ID));

        mockMvc.perform(post("/api/events/{id}/publish", EVENT_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Event seats already generated for event " + EVENT_ID));
    }

    @Test
    void publishEvent_returns400WhenEventIsNotDraft() throws Exception {
        when(service.publishEvent(EVENT_ID)).thenThrow(new IllegalStateException("Only draft event can be published"));

        mockMvc.perform(post("/api/events/{id}/publish", EVENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Only draft event can be published"));
    }

    @Test
    void cancelEvent_returns204WithoutBody() throws Exception {
        mockMvc.perform(post("/api/events/{id}/cancel", EVENT_ID))
                .andExpect(status().isNoContent());

        verify(service).cancelEvent(EVENT_ID);
    }

    @Test
    void cancelEvent_returns400WhenEventIsNotPublished() throws Exception {
        doThrow(new IllegalStateException("Only published event can be cancelled"))
                .when(service).cancelEvent(EVENT_ID);

        mockMvc.perform(post("/api/events/{id}/cancel", EVENT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only published event can be cancelled"));
    }

    @Test
    void findMyEvents_returnsEventsOfOrganizerFromHeader() throws Exception {
        when(service.findByOrganizerId(ORGANIZER_ID))
                .thenReturn(List.of(eventResponse(EventStatus.DRAFT), eventResponse(EventStatus.PUBLISHED)));

        mockMvc.perform(get("/api/events/organizer/my").header("X-User-Id", ORGANIZER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[1].status").value("PUBLISHED"));

        verify(service).findByOrganizerId(ORGANIZER_ID);
    }
}
