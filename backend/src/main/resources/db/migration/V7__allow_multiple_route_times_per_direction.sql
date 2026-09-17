ALTER TABLE recurring_route_schedules
    DROP CONSTRAINT uk_recurring_route_schedule;

ALTER TABLE daily_confirmations
    DROP CONSTRAINT uk_daily_confirmation;

ALTER TABLE daily_confirmations
    ADD CONSTRAINT uk_daily_confirmation
        UNIQUE (student_id, service_date, direction, scheduled_time);

CREATE INDEX idx_route_schedules_day_direction
    ON recurring_route_schedules (route_id, day_of_week, direction, departure_time);
