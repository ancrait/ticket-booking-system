CREATE TABLE outbox(
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP,

    CONSTRAINT pk_outbox PRIMARY KEY (id)
);
