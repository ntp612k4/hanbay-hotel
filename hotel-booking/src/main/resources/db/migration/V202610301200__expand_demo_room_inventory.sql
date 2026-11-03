-- Complete the demo catalog on fresh databases. Preserve room types already
-- created through management, including their IDs, prices and full descriptions.
INSERT INTO room_types (
    code, name, description, short_description, base_price, max_guests,
    capacity_adults, capacity_children, bed_type, area_m2, thumbnail_url,
    status, created_at, updated_at
)
VALUES
    (
        'PREMIER_STUDIO',
        'Premier Studio',
        'Premier Studio kết hợp khu nghỉ ngơi với bàn làm việc riêng trong không gian hiện đại. Cửa sổ hướng thành phố đón ánh sáng tự nhiên, phù hợp cho khách công tác hoặc kỳ nghỉ của hai người.',
        'Studio hiện đại với bàn làm việc riêng, giường ngủ êm ái và cửa sổ hướng thành phố, dành cho chuyến công tác hoặc kỳ nghỉ của hai người.',
        2100000, 2, 2, 0, '1 giường Queen', 36.00,
        'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=1200&q=85',
        'ACTIVE'::room_type_status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    ),
    (
        'FAMILY_SUITE',
        'Suite gia đình',
        'Suite gia đình có khu nghỉ và khu sinh hoạt riêng, mang đến sự thoải mái cho những kỳ lưu trú cùng người thân. Không gian rộng thoáng với giường King và giường đơn đáp ứng nhu cầu nghỉ ngơi của gia đình.',
        'Suite rộng thoáng với khu nghỉ và sinh hoạt riêng, tiện nghi đầy đủ cùng không gian gần gũi để gia đình tận hưởng những ngày bên nhau.',
        5200000, 5, 3, 2, '1 giường King và 1 giường đơn', 68.00,
        'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=85',
        'ACTIVE'::room_type_status, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    )
ON CONFLICT (code) DO NOTHING;

-- Keep the card summaries similar in length. Full room detail content is kept.
UPDATE room_types AS room_type
SET short_description = summary.description,
    updated_at = CURRENT_TIMESTAMP
FROM (
    VALUES
        ('SUPERIOR', 'Không gian sáng thoáng với nội thất hiện đại và góc nghỉ yên tĩnh, phù hợp cho chuyến công tác, kỳ nghỉ đôi hoặc gia đình nhỏ.'),
        ('DELUXE', 'Đón nắng sớm từ ban công hướng biển, thư giãn trên giường êm ái và tận hưởng nội thất thanh lịch cho kỳ nghỉ trọn vẹn tại Đà Nẵng.'),
        ('DELUXE_OCEAN', 'Thư giãn bên ban công hướng biển Mỹ Khê, tận hưởng phòng nghỉ rộng rãi và không gian ấm cúng dành cho cặp đôi hoặc gia đình nhỏ.'),
        ('JUNIOR_SUITE', 'Phòng ngủ riêng kết hợp khu tiếp khách rộng thoáng, mở ra tầm nhìn vịnh Đà Nẵng cho kỳ nghỉ thư thái bên gia đình và bạn bè.'),
        ('HANBAY-OCEAN-SUITE', 'Tận hưởng suite riêng tư trên tầng cao, khu tiếp khách sang trọng cùng tầm nhìn đại dương rộng mở cho kỳ nghỉ đáng nhớ bên người thân.'),
        ('PREMIER_STUDIO', 'Studio hiện đại với bàn làm việc riêng, giường ngủ êm ái và cửa sổ hướng thành phố, dành cho chuyến công tác hoặc kỳ nghỉ của hai người.'),
        ('FAMILY_SUITE', 'Suite rộng thoáng với khu nghỉ và sinh hoạt riêng, tiện nghi đầy đủ cùng không gian gần gũi để gia đình tận hưởng những ngày bên nhau.')
) AS summary(code, description)
WHERE room_type.code = summary.code
  AND room_type.short_description IS DISTINCT FROM summary.description;

-- Top up active types to ten physical rooms unless rooms were explicitly taken
-- out of service. Respect a manually reduced inventory when this migration runs
-- after demo data was prepared; never reset existing room/booking states.
DO $$
DECLARE
    room_type RECORD;
    missing_rooms INTEGER;
    sequence_number INTEGER;
    inserted_rooms INTEGER;
    demo_floor INTEGER;
    room_prefix TEXT;
BEGIN
    FOR room_type IN
        SELECT catalog.id, catalog.code, catalog.name
        FROM room_types catalog
        WHERE catalog.status = 'ACTIVE'
          AND NOT EXISTS (
              SELECT 1 FROM rooms existing
              WHERE existing.room_type_id = catalog.id
                AND existing.status = 'OUT_OF_SERVICE'
          )
        ORDER BY catalog.code
        FOR UPDATE
    LOOP
        SELECT GREATEST(10 - COUNT(*), 0),
               COALESCE(MIN(floor), CASE room_type.code
                   WHEN 'PREMIER_STUDIO' THEN 4
                   WHEN 'FAMILY_SUITE' THEN 6
                   WHEN 'HANBAY-OCEAN-SUITE' THEN 7
                   ELSE 1
               END)
        INTO missing_rooms, demo_floor
        FROM rooms
        WHERE room_type_id = room_type.id;

        room_prefix := CASE room_type.code
            WHEN 'SUPERIOR' THEN 'S'
            WHEN 'DELUXE' THEN 'D'
            WHEN 'DELUXE_OCEAN' THEN 'DO'
            WHEN 'JUNIOR_SUITE' THEN 'J'
            WHEN 'HANBAY-OCEAN-SUITE' THEN 'OS'
            WHEN 'PREMIER_STUDIO' THEN 'PS'
            WHEN 'FAMILY_SUITE' THEN 'FS'
            ELSE LEFT(room_type.code, 20) || '-'
        END;
        sequence_number := 1;
        WHILE missing_rooms > 0 LOOP
            INSERT INTO rooms (
                room_type_id, room_number, floor, status, note, created_at, updated_at
            )
            VALUES (
                room_type.id,
                room_prefix || (demo_floor * 100 + sequence_number)::TEXT,
                demo_floor,
                'AVAILABLE'::room_status,
                'Phòng mẫu bổ sung cho ' || room_type.name,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            )
            ON CONFLICT (room_number) DO NOTHING;

            GET DIAGNOSTICS inserted_rooms = ROW_COUNT;
            missing_rooms := missing_rooms - inserted_rooms;
            sequence_number := sequence_number + 1;
        END LOOP;
    END LOOP;
END;
$$;
