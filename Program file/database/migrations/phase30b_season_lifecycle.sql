-- Phase30-B: normalize the season lifecycle without changing enrollment,
-- schedule, match, order, or ticket business records.
USE league_ticket;

ALTER TABLE season_info DROP CHECK ck_season_status;

UPDATE season_info s
LEFT JOIN season_schedule_batch b ON b.season_id = s.season_id
SET s.season_status = CASE
    WHEN s.season_status = 'FINISHED' THEN 'FINISHED'
    WHEN b.batch_status = 'CONFIRMED' THEN 'IN_PROGRESS'
    WHEN b.batch_status = 'GENERATED' THEN 'PREPARING'
    WHEN s.season_status = 'ACTIVE' THEN 'IN_PROGRESS'
    WHEN s.season_status = 'REGISTERING' THEN 'REGISTRATION'
    WHEN EXISTS (
        SELECT 1
        FROM club_season_enrollment e
        WHERE e.season_id = s.season_id
          AND e.enrollment_status = 'SUBMITTED'
    ) THEN 'REGISTRATION'
    ELSE 'DRAFT'
END;

ALTER TABLE season_info
    ADD CONSTRAINT ck_season_status CHECK (
        season_status IN ('DRAFT', 'REGISTRATION', 'PREPARING', 'IN_PROGRESS', 'FINISHED')
    );
