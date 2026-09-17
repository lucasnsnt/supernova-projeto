ALTER TABLE trips ADD COLUMN completed_stop_count INTEGER NOT NULL DEFAULT 0;

CREATE TABLE trip_locations (
    trip_id BIGINT PRIMARY KEY,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    accuracy DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    recorded_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_trip_locations_trip FOREIGN KEY (trip_id) REFERENCES trips (id) ON DELETE CASCADE
);

CREATE INDEX idx_trip_locations_updated_at ON trip_locations (updated_at);
