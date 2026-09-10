CREATE TABLE email_notifications
(
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(256) NOT NULL,
    subject         VARCHAR(256),
    body            TEXT,
    type            VARCHAR(20)  NOT NULL CHECK (type
        IN ('USER_REGISTERED', 'TICKET_PURCHASED', 'BOOKING_CANCELED', 'PAYMENT_FAILED', 'BOOKING_EXPIRED')),
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    error_message   VARCHAR(256),
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_email_notifications PRIMARY KEY (id)
);
