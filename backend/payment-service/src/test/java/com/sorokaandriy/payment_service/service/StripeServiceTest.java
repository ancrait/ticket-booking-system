package com.sorokaandriy.payment_service.service;

import com.sorokaandriy.payment_service.exception.PaymentProcessingException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StripeServiceTest {

    private static final UUID BOOKING_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService();
        ReflectionTestUtils.setField(stripeService, "currency", "UAH");
    }

    @Test
    void createPaymentIntent_returnsIntentIdAndBuildsCorrectParams() {
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getId()).thenReturn("pi_123");

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(intent);

            String id = stripeService.createPaymentIntent(
                    new BigDecimal("12.34"), BOOKING_ID, USER_ID);

            assertThat(id).isEqualTo("pi_123");

            ArgumentCaptor<PaymentIntentCreateParams> captor =
                    ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
            mocked.verify(() -> PaymentIntent.create(captor.capture()));
            PaymentIntentCreateParams params = captor.getValue();
            assertThat(params.getAmount()).isEqualTo(1234L);
            assertThat(params.getCurrency()).isEqualTo("UAH");
            assertThat(params.getMetadata())
                    .containsEntry("bookingId", BOOKING_ID.toString())
                    .containsEntry("userId", USER_ID.toString());
            assertThat(params.getPaymentMethodTypes()).containsExactly("card");
        }
    }

    @Test
    void createPaymentIntent_throwsPaymentProcessingExceptionOnStripeError() {
        StripeException stripeException = mock(StripeException.class);

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(stripeException);

            assertThatThrownBy(() -> stripeService.createPaymentIntent(
                    new BigDecimal("12.34"), BOOKING_ID, USER_ID))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessage("Failed to create Stripe payment intent")
                    .hasCause(stripeException);
        }
    }

    @Test
    void resolvePaymentIntent_reusesConfirmableCardOnlyIntent() throws StripeException {
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getId()).thenReturn("pi_1");
        when(intent.getPaymentMethodTypes()).thenReturn(List.of("card"));
        when(intent.getStatus()).thenReturn("requires_payment_method");

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(intent);

            StripeService.PaymentIntentResult result =
                    stripeService.resolvePaymentIntent(
                            "pi_1", new BigDecimal("12.34"), BOOKING_ID, USER_ID);

            assertThat(result.id()).isEqualTo("pi_1");
            assertThat(result.alreadySucceeded()).isFalse();
            verify(intent, never()).cancel();
            mocked.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)), never());
        }
    }

    @Test
    void resolvePaymentIntent_returnsAlreadySucceededForSucceededIntent() throws StripeException {
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getId()).thenReturn("pi_1");
        when(intent.getPaymentMethodTypes()).thenReturn(List.of("card", "link"));
        when(intent.getStatus()).thenReturn("succeeded");

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(intent);

            StripeService.PaymentIntentResult result =
                    stripeService.resolvePaymentIntent(
                            "pi_1", new BigDecimal("12.34"), BOOKING_ID, USER_ID);

            assertThat(result.id()).isEqualTo("pi_1");
            assertThat(result.alreadySucceeded()).isTrue();
            verify(intent, never()).cancel();
            mocked.verify(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)), never());
        }
    }

    @Test
    void resolvePaymentIntent_cancelsOldIntentAndCreatesNewWhenNotCardOnly() throws StripeException {
        PaymentIntent oldIntent = mock(PaymentIntent.class);
        when(oldIntent.getId()).thenReturn("pi_1");
        when(oldIntent.getPaymentMethodTypes()).thenReturn(List.of("card", "link"));
        when(oldIntent.getStatus()).thenReturn("requires_confirmation");

        PaymentIntent newIntent = mock(PaymentIntent.class);
        when(newIntent.getId()).thenReturn("pi_2");

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(oldIntent);
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(newIntent);

            StripeService.PaymentIntentResult result =
                    stripeService.resolvePaymentIntent(
                            "pi_1", new BigDecimal("12.34"), BOOKING_ID, USER_ID);

            assertThat(result.id()).isEqualTo("pi_2");
            assertThat(result.alreadySucceeded()).isFalse();
            verify(oldIntent).cancel();
        }
    }

    @Test
    void resolvePaymentIntent_createsNewWhenCurrentIdIsMissing() {
        PaymentIntent newIntent = mock(PaymentIntent.class);
        when(newIntent.getId()).thenReturn("pi_2");

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(newIntent);

            StripeService.PaymentIntentResult result =
                    stripeService.resolvePaymentIntent(
                            null, new BigDecimal("12.34"), BOOKING_ID, USER_ID);

            assertThat(result.id()).isEqualTo("pi_2");
            assertThat(result.alreadySucceeded()).isFalse();
            mocked.verify(() -> PaymentIntent.retrieve(any()), never());
        }
    }

    @Test
    void resolvePaymentIntent_createsNewWhenRetrieveFails() {
        StripeException stripeException = mock(StripeException.class);
        PaymentIntent newIntent = mock(PaymentIntent.class);
        when(newIntent.getId()).thenReturn("pi_2");

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.retrieve("pi_1")).thenThrow(stripeException);
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(newIntent);

            StripeService.PaymentIntentResult result =
                    stripeService.resolvePaymentIntent(
                            "pi_1", new BigDecimal("12.34"), BOOKING_ID, USER_ID);

            assertThat(result.id()).isEqualTo("pi_2");
            assertThat(result.alreadySucceeded()).isFalse();
        }
    }

    @Test
    void resolvePaymentIntent_createsNewForCanceledIntentWithoutCallingCancel() throws StripeException {
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getId()).thenReturn("pi_1");
        when(intent.getPaymentMethodTypes()).thenReturn(List.of("card"));
        when(intent.getStatus()).thenReturn("canceled");

        PaymentIntent newIntent = mock(PaymentIntent.class);
        when(newIntent.getId()).thenReturn("pi_2");

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(intent);
            mocked.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(newIntent);

            StripeService.PaymentIntentResult result =
                    stripeService.resolvePaymentIntent(
                            "pi_1", new BigDecimal("12.34"), BOOKING_ID, USER_ID);

            assertThat(result.id()).isEqualTo("pi_2");
            assertThat(result.alreadySucceeded()).isFalse();
            verify(intent, never()).cancel();
        }
    }

    @Test
    void getClientSecret_returnsSecret() {
        PaymentIntent intent = mock(PaymentIntent.class);
        when(intent.getClientSecret()).thenReturn("cs_secret_123");

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.retrieve("pi_1")).thenReturn(intent);

            assertThat(stripeService.getClientSecret("pi_1")).isEqualTo("cs_secret_123");
        }
    }

    @Test
    void getClientSecret_throwsPaymentProcessingExceptionOnStripeError() {
        StripeException stripeException = mock(StripeException.class);

        try (MockedStatic<PaymentIntent> mocked = mockStatic(PaymentIntent.class)) {
            mocked.when(() -> PaymentIntent.retrieve("pi_1")).thenThrow(stripeException);

            assertThatThrownBy(() -> stripeService.getClientSecret("pi_1"))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessage("Failed to retrieve payment intent")
                    .hasCause(stripeException);
        }
    }

    @Test
    void refundPayment_createsRefundForPaymentIntent() {
        Refund refund = mock(Refund.class);
        when(refund.getId()).thenReturn("re_123");

        try (MockedStatic<Refund> mocked = mockStatic(Refund.class)) {
            mocked.when(() -> Refund.create(any(RefundCreateParams.class))).thenReturn(refund);

            stripeService.refundPayment("pi_1");

            ArgumentCaptor<RefundCreateParams> captor =
                    ArgumentCaptor.forClass(RefundCreateParams.class);
            mocked.verify(() -> Refund.create(captor.capture()));
            assertThat(captor.getValue().getPaymentIntent()).isEqualTo("pi_1");
        }
    }

    @Test
    void refundPayment_throwsPaymentProcessingExceptionOnStripeError() {
        StripeException stripeException = mock(StripeException.class);

        try (MockedStatic<Refund> mocked = mockStatic(Refund.class)) {
            mocked.when(() -> Refund.create(any(RefundCreateParams.class)))
                    .thenThrow(stripeException);

            assertThatThrownBy(() -> stripeService.refundPayment("pi_1"))
                    .isInstanceOf(PaymentProcessingException.class)
                    .hasMessage("Failed to refund payment")
                    .hasCause(stripeException);
        }
    }
}
