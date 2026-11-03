INSERT INTO rooms (id, room_type_id, room_number, floor, status, created_at)
SELECT inventory.id, room_types.id, inventory.room_number, inventory.floor, 'AVAILABLE'::room_status, CURRENT_TIMESTAMP
FROM (
    VALUES
        ('b1000000-0000-4000-8000-000000000001'::UUID, 'SUPERIOR', 'S101', 1),
        ('b1000000-0000-4000-8000-000000000002'::UUID, 'SUPERIOR', 'S102', 1),
        ('b1000000-0000-4000-8000-000000000003'::UUID, 'SUPERIOR', 'S201', 2),
        ('b1000000-0000-4000-8000-000000000004'::UUID, 'DELUXE', 'D301', 3),
        ('b1000000-0000-4000-8000-000000000005'::UUID, 'DELUXE', 'D302', 3),
        ('b1000000-0000-4000-8000-000000000006'::UUID, 'DELUXE', 'D401', 4),
        ('b1000000-0000-4000-8000-000000000007'::UUID, 'JUNIOR_SUITE', 'J501', 5),
        ('b1000000-0000-4000-8000-000000000008'::UUID, 'JUNIOR_SUITE', 'J502', 5),
        ('b1000000-0000-4000-8000-000000000009'::UUID, 'JUNIOR_SUITE', 'J601', 6)
) AS inventory(id, room_type_code, room_number, floor)
JOIN room_types ON room_types.code = inventory.room_type_code
ON CONFLICT (room_number) DO NOTHING;
