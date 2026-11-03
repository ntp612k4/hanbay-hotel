INSERT INTO holiday_calendar (reference_id, name, start_date, end_date, active, created_at, updated_at)
SELECT
    gen_random_uuid(),
    LEFT(CONCAT('Lịch lễ chuyển tiếp - ', name), 150),
    start_date,
    end_date,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM price_rules
WHERE active = TRUE
  AND rule_type = 'HOLIDAY';
