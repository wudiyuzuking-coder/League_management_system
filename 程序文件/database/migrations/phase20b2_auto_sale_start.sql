-- Phase 20B2: derive every ticket-zone sale start from its match time.
-- Run the preflight SELECT first and review its result. If it returns rows, this
-- script deliberately updates zero rows; correct sale_end_time manually and rerun.

SET @phase20b2_conflict_count := (
    SELECT COUNT(*)
    FROM match_ticket_zone mz
    JOIN match_info m ON m.match_id = mz.match_id
    WHERE mz.sale_end_time IS NULL
       OR mz.sale_end_time <= DATE_ADD(DATE_SUB(DATE(m.match_time), INTERVAL 7 DAY), INTERVAL 20 HOUR)
);

SELECT mz.match_zone_id,
       mz.match_id,
       m.match_time,
       mz.sale_start_time AS old_sale_start_time,
       DATE_ADD(DATE_SUB(DATE(m.match_time), INTERVAL 7 DAY), INTERVAL 20 HOUR) AS new_auto_sale_start_time,
       mz.sale_end_time
FROM match_ticket_zone mz
JOIN match_info m ON m.match_id = mz.match_id
WHERE mz.sale_end_time IS NULL
   OR mz.sale_end_time <= DATE_ADD(DATE_SUB(DATE(m.match_time), INTERVAL 7 DAY), INTERVAL 20 HOUR)
ORDER BY mz.match_zone_id;

UPDATE match_ticket_zone mz
JOIN match_info m ON m.match_id = mz.match_id
SET mz.sale_start_time = DATE_ADD(DATE_SUB(DATE(m.match_time), INTERVAL 7 DAY), INTERVAL 20 HOUR)
WHERE @phase20b2_conflict_count = 0;

SELECT @phase20b2_conflict_count AS conflict_count,
       CASE WHEN @phase20b2_conflict_count = 0
            THEN 'MIGRATION_APPLIED'
            ELSE 'MIGRATION_BLOCKED_REVIEW_SALE_END_TIME'
       END AS migration_result;
