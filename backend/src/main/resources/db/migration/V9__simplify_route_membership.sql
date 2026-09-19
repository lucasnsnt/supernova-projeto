ALTER TABLE recurring_route_enrollments ADD COLUMN active BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE recurring_route_enrollments
SET active = CASE WHEN status IN ('PENDING', 'APPROVED') THEN TRUE ELSE FALSE END;

DROP INDEX idx_recurring_route_enrollments_student;
CREATE INDEX idx_recurring_route_enrollments_student_active
    ON recurring_route_enrollments (student_id, active);

ALTER TABLE recurring_route_enrollments DROP COLUMN status;
ALTER TABLE recurring_route_enrollments DROP COLUMN reviewed_at;
