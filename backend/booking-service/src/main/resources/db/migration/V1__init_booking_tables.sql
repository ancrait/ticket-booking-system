CREATE TABLE bookings
(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    event_id UUID NOT NULL,
    event_title VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'CANCELED')),
    total_price  DECIMAL(10,2) NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT pk_bookings PRIMARY KEY (id)

);

CREATE TABLE booking_items(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    event_seat_id UUID NOT NULL,
    row_number INTEGER NOT NULL,
    seat_number INTEGER NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    booking_id UUID NOT NULL,

    CONSTRAINT pk_booking_item PRIMARY KEY (id),
    CONSTRAINT fk_booking_item FOREIGN KEY (booking_id) REFERENCES bookings(id)

);

CREATE TABLE outbox(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP,

    CONSTRAINT pk_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_event_id ON bookings(event_id);
CREATE INDEX idx_booking_items_booking_id ON booking_items(booking_id);
CREATE INDEX idx_outbox_unpublished ON outbox(published_at, created_at);
