CREATE TABLE payments(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    user_id UUID NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING',
        'SUCCEEDED', 'FAILED', 'CANCELED', 'REFUNDED')),
    provider_payment_id VARCHAR(256),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT pk_payments PRIMARY KEY (id)
);

CREATE INDEX idx_payments_booking_id ON payments(booking_id);
CREATE INDEX idx_payments_user_id ON payments(user_id);