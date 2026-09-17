ALTER TABLE daily_confirmations ADD COLUMN recurring_route_id BIGINT;
ALTER TABLE daily_confirmations ADD CONSTRAINT fk_confirmations_recurring_route
    FOREIGN KEY (recurring_route_id) REFERENCES recurring_routes (id);
CREATE INDEX idx_confirmations_recurring_route ON daily_confirmations (recurring_route_id, service_date);
