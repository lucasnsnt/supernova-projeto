-- Preserve historic confirmations/trips; unknown academic deadlines remain NULL.
ALTER TABLE daily_confirmations ADD COLUMN academic_time TIME;
ALTER TABLE trips ADD COLUMN recurring_route_id BIGINT REFERENCES recurring_routes (id);
ALTER TABLE daily_confirmations DROP CONSTRAINT uk_daily_confirmation;
ALTER TABLE daily_confirmations ADD CONSTRAINT uk_daily_confirmation
    UNIQUE (student_id, service_date, direction, recurring_route_id, scheduled_time);
ALTER TABLE trips ADD CONSTRAINT uk_trip_route_occurrence
    UNIQUE (recurring_route_id, service_date, direction);

-- An existing request is the student's own consent to join.
UPDATE recurring_route_enrollments SET status = 'APPROVED' WHERE status = 'PENDING';

-- Backfill only unambiguous driver-route occurrences; keep the original manifest
-- and confirmation foreign keys, including active/completed trips.
UPDATE trips SET recurring_route_id = (
    SELECT MIN(c.recurring_route_id) FROM trip_participants p
    JOIN daily_confirmations c ON c.id = p.confirmation_id WHERE p.trip_id = trips.id
)
WHERE EXISTS (SELECT 1 FROM trip_participants p WHERE p.trip_id = trips.id)
AND NOT EXISTS (SELECT 1 FROM trip_participants p JOIN daily_confirmations c ON c.id = p.confirmation_id
    WHERE p.trip_id = trips.id AND c.recurring_route_id IS NULL)
AND (SELECT COUNT(DISTINCT c.recurring_route_id) FROM trip_participants p
    JOIN daily_confirmations c ON c.id = p.confirmation_id WHERE p.trip_id = trips.id) = 1
AND (SELECT COUNT(DISTINCT other.id) FROM trips other
    JOIN trip_participants op ON op.trip_id = other.id
    JOIN daily_confirmations oc ON oc.id = op.confirmation_id
    WHERE other.service_date = trips.service_date AND other.direction = trips.direction
    AND oc.recurring_route_id = (SELECT MIN(c.recurring_route_id) FROM trip_participants p
        JOIN daily_confirmations c ON c.id = p.confirmation_id WHERE p.trip_id = trips.id)) = 1;

-- Retire only legacy agenda operation; ambiguous route manifests remain visible
-- for attention, without deleting or moving their historical participants.
UPDATE trips SET status = 'CANCELLED', cancellation_reason = 'Fluxo substituído pelas rotas do motorista',
    cancelled_at = CURRENT_TIMESTAMP WHERE status IN ('PLANNING', 'PLANNED', 'NEEDS_ATTENTION')
    AND recurring_route_id IS NULL
    AND NOT EXISTS (SELECT 1 FROM trip_participants p JOIN daily_confirmations c ON c.id = p.confirmation_id
        WHERE p.trip_id = trips.id AND c.recurring_route_id IS NOT NULL);
UPDATE trips SET status = 'NEEDS_ATTENTION', planning_issue = 'Revise as saídas antigas com múltiplos horários ou rotas'
    WHERE recurring_route_id IS NULL AND status IN ('PLANNING', 'PLANNED', 'NEEDS_ATTENTION');
UPDATE daily_confirmations SET status = 'NO_RESPONSE'
    WHERE recurring_route_id IS NULL AND status IN ('PENDING', 'YES')
    AND NOT EXISTS (SELECT 1 FROM trip_participants p WHERE p.confirmation_id = daily_confirmations.id);
