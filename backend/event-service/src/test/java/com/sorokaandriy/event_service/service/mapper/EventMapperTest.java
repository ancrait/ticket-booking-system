package com.sorokaandriy.event_service.service.mapper;

import com.sorokaandriy.event_service.dto.requests.EventRequest;
import com.sorokaandriy.event_service.dto.responses.EventResponse;
import com.sorokaandriy.event_service.entity.Event;
import com.sorokaandriy.event_service.entity.Hall;
import com.sorokaandriy.event_service.entity.Venue;
import com.sorokaandriy.event_service.entity.enumeration.EventStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventMapperTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID HALL_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID VENUE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ORGANIZER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant STARTS_AT = Instant.parse("2026-05-01T18:00:00Z");
    private static final Instant ENDS_AT = Instant.parse("2026-05-01T21:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");

    private final EventMapper mapper = new EventMapper();

    private EventRequest request() {
        return EventRequest.builder()
                .title("Concert")
                .description("Live show")
                .posterUrl("http://example.com/poster.png")
                .startsAt(STARTS_AT)
                .endsAt(ENDS_AT)
                .hallId(HALL_ID)
                .build();
    }

    private Hall hall() {
        return Hall.builder()
                .id(HALL_ID)
                .name("Hall 1")
                .rowsCount(3)
                .seatsPerRow(2)
                .venue(Venue.builder().id(VENUE_ID).name("Arena").city("Kyiv").address("Main St 1").build())
                .build();
    }

    @Test
    void fromEventRequestToEvent_createsDraftEventWithHallAndOrganizer() {
        Event event = mapper.fromEventRequestToEvent(request(), ORGANIZER_ID, hall());

        assertThat(event.getTitle()).isEqualTo("Concert");
        assertThat(event.getDescription()).isEqualTo("Live show");
        assertThat(event.getPosterUrl()).isEqualTo("http://example.com/poster.png");
        assertThat(event.getStartsAt()).isEqualTo(STARTS_AT);
        assertThat(event.getEndsAt()).isEqualTo(ENDS_AT);
        assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        assertThat(event.getOrganizerId()).isEqualTo(ORGANIZER_ID);
        assertThat(event.getHall().getId()).isEqualTo(HALL_ID);
        assertThat(event.getId()).isNull();
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    void fromEventToEventResponse_mapsAllFieldsAndFlattensHallId() {
        Event event = Event.builder()
                .id(EVENT_ID)
                .title("Concert")
                .description("Live show")
                .posterUrl("http://example.com/poster.png")
                .startsAt(STARTS_AT)
                .endsAt(ENDS_AT)
                .status(EventStatus.PUBLISHED)
                .organizerId(ORGANIZER_ID)
                .hall(hall())
                .createdAt(CREATED_AT)
                .build();

        EventResponse response = mapper.fromEventToEventResponse(event);

        assertThat(response.id()).isEqualTo(EVENT_ID);
        assertThat(response.title()).isEqualTo("Concert");
        assertThat(response.description()).isEqualTo("Live show");
        assertThat(response.posterUrl()).isEqualTo("http://example.com/poster.png");
        assertThat(response.startsAt()).isEqualTo(STARTS_AT);
        assertThat(response.endsAt()).isEqualTo(ENDS_AT);
        assertThat(response.status()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(response.organizerId()).isEqualTo(ORGANIZER_ID);
        assertThat(response.hallId()).isEqualTo(HALL_ID);
        assertThat(response.createdAt()).isEqualTo(CREATED_AT);
    }
}
