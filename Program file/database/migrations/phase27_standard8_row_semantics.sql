-- Phase27: STANDARD_8 only.  Run after confirming no PAID order references a
-- target stadium.  LEGACY stadiums and their seats are deliberately excluded.
UPDATE stadium_seat se
JOIN stadium_zone z ON z.stadium_zone_id=se.stadium_zone_id
JOIN stadium_info s ON s.stadium_id=se.stadium_id AND s.venue_model='STANDARD_8'
JOIN club_home_stadium_config c ON c.stadium_id=s.stadium_id
SET se.row_no=CONCAT(se.row_seq+c.rows_per_zone,_utf8mb4 0xE68E92),
    se.row_seq=se.row_seq+c.rows_per_zone
WHERE z.ticket_type='NORMAL'
  AND se.row_seq BETWEEN 1 AND c.rows_per_zone
  AND NOT EXISTS (
      SELECT 1 FROM ticket_order o
      JOIN match_info m ON m.match_id=o.match_id
      WHERE m.stadium_id=s.stadium_id AND o.order_status='PAID'
  );
