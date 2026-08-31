CREATE TABLE venues(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT pk_venues PRIMARY KEY (id)
);


CREATE TABLE halls(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    rows_count INTEGER NOT NULL,
    seats_per_row INTEGER NOT NULL,
    venue_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT pk_halls PRIMARY KEY (id),
    CONSTRAINT fk_halls_venue FOREIGN KEY (venue_id) REFERENCES venues(id)
);


CREATE TABLE seats(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    row_number INTEGER NOT NULL,
    seat_number INTEGER NOT NULL,
    sector VARCHAR(50) NOT NULL,
    hall_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT pk_seats PRIMARY KEY (id),
    CONSTRAINT fk_seats_hall FOREIGN KEY (hall_id) REFERENCES halls(id),
    CONSTRAINT uq_seats_hall_row_seat UNIQUE (hall_id, row_number, seat_number)
);


CREATE TABLE events(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    poster_url VARCHAR(500),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED')),
    organizer_id UUID NOT NULL,
    hall_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_hall FOREIGN KEY (hall_id) REFERENCES halls(id)
);


CREATE TABLE event_seats(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('FREE', 'HELD', 'SOLD')),
    version INTEGER NOT NULL DEFAULT 0,
    event_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT pk_event_seats PRIMARY KEY (id),
    CONSTRAINT fk_event_seats_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_event_seats_seat FOREIGN KEY (seat_id) REFERENCES seats(id),
    CONSTRAINT uq_event_seats_event_seat UNIQUE (event_id, seat_id)
);
