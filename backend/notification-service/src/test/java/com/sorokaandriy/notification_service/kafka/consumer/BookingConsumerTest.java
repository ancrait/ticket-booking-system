package com.sorokaandriy.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sorokaandriy.notification_service.dto.events.BookingCanceledEvent;
import com.sorokaandriy.notification_service.dto.events.BookingExpiredEvent;
import com.sorokaandriy.notification_service.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookingConsumerTest {

    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock
    private EmailNotificationService service;

    private ObjectMapper objectMapper;

    private BookingConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        consumer = new BookingConsumer(service, objectMapper);
    }

    private BookingCanceledEvent bookingCanceledEvent() {
        return BookingCanceledEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .eventId(EVENT_ID)
                .reason("canceled by user")
                .build();
    }

    private BookingExpiredEvent bookingExpiredEvent() {
        return BookingExpiredEvent.builder()
                .bookingId(BOOKING_ID)
                .userId(USER_ID)
                .email("john@example.com")
                .eventId(EVENT_ID)
                .reason("payment not completed in time")
                .build();
    }

    @Test
    void handleBookingCanceledEventDeserializesPayloadAndDelegatesToService() throws Exception {
        BookingCanceledEvent event = bookingCanceledEvent();

        consumer.handleBookingCanceledEvent(objectMapper.writeValueAsString(event));

        verify(service).handleBookingCanceledEvent(event);
    }

    @Test
    void handleBookingCanceledEventWithMalformedPayloadDoesNotDelegateAndDoesNotThrow() {
        assertThatCode(() -> consumer.handleBookingCanceledEvent("{not-json"))
                .doesNotThrowAnyException();

        verifyNoInteractions(service);
    }

    @Test
    void handleBookingExpiredEventDeserializesPayloadAndDelegatesToService() throws Exception {
        BookingExpiredEvent event = bookingExpiredEvent();

        consumer.handleBookingExpiredEvent(objectMapper.writeValueAsString(event));

        verify(service).handleBookingExpiredEvent(event);
    }

    @Test
    void handleBookingExpiredEventWithMalformedPayloadDoesNotDelegateAndDoesNotThrow() {
        assertThatCode(() -> consumer.handleBookingExpiredEvent("{not-json"))
                .doesNotThrowAnyException();

        verifyNoInteractions(service);
    }
}
