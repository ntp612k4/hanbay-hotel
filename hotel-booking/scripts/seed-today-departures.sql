-- Chạy thủ công trên database demo bằng psql hoặc pgAdmin Query Tool.
-- Tạo 2 booking đã nhận phòng, đã thanh toán đủ và trả phòng hôm nay.
-- Chạy lại cùng ngày sẽ bỏ qua booking đã tồn tại, kể cả khi đã checkout.
BEGIN;
SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

DO $seed$
DECLARE
    hotel_today DATE := CURRENT_DATE;
    arrival_date DATE := CURRENT_DATE - 1;
    sample RECORD;
    selected_type room_types%ROWTYPE;
    selected_room rooms%ROWTYPE;
    booking_code_value TEXT;
    payment_code_value TEXT;
    booking_id_value UUID;
    payment_id_value UUID;
    sellable_rooms BIGINT;
    reserved_rooms BIGINT;
    created_time TIMESTAMP;
    paid_time TIMESTAMP;
    checked_in_time TIMESTAMP;
BEGIN
    -- Hai lần chạy đồng thời trong cùng ngày không tạo thêm dữ liệu trùng.
    PERFORM pg_advisory_xact_lock(hashtext('seed-today-departures:' || hotel_today::TEXT));

    FOR sample IN
        SELECT * FROM (
            VALUES
                (1, 'SUPERIOR', 'Nguyễn Minh An (Demo)', '0900000101', 'checkout.demo01@example.com'),
                (2, 'JUNIOR_SUITE', 'Trần Ngọc Linh (Demo)', '0900000102', 'checkout.demo02@example.com')
        ) AS samples(sequence_number, room_type_code, guest_name, guest_phone, guest_email)
    LOOP
        booking_code_value := 'DEMO-OUT-' || TO_CHAR(hotel_today, 'YYYYMMDD')
            || '-' || LPAD(sample.sequence_number::TEXT, 2, '0');
        payment_code_value := 'DEMO-PAID-' || TO_CHAR(hotel_today, 'YYYYMMDD')
            || '-' || LPAD(sample.sequence_number::TEXT, 2, '0');

        IF EXISTS (SELECT 1 FROM bookings WHERE booking_code = booking_code_value) THEN
            RAISE NOTICE 'Bỏ qua %, booking đã tồn tại.', booking_code_value;
            CONTINUE;
        END IF;

        SELECT * INTO selected_type
        FROM room_types
        WHERE code = sample.room_type_code AND status = 'ACTIVE'
        FOR UPDATE;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Không tìm thấy hạng phòng đang hoạt động: %', sample.room_type_code;
        END IF;
        IF selected_type.base_price <= 0 OR selected_type.capacity_adults < 2 OR selected_type.max_guests < 2 THEN
            RAISE EXCEPTION 'Hạng phòng % không phù hợp với dữ liệu mẫu.', sample.room_type_code;
        END IF;

        -- Kiểm tra tồn của kỳ lưu trú [hôm qua, hôm nay).
        SELECT COUNT(*) INTO sellable_rooms
        FROM rooms
        WHERE room_type_id = selected_type.id
          AND status IN ('AVAILABLE', 'OCCUPIED', 'DIRTY', 'CLEANING');

        SELECT COUNT(*) INTO reserved_rooms
        FROM bookings
        WHERE room_type_id = selected_type.id
          AND check_in_date < hotel_today
          AND check_out_date > arrival_date
          AND (status IN ('CONFIRMED', 'CHECKED_IN')
               OR (status = 'PENDING' AND expired_at > LOCALTIMESTAMP));

        IF reserved_rooms >= sellable_rooms THEN
            RAISE EXCEPTION 'Hạng phòng % đã hết tồn cho kỳ lưu trú mẫu.', sample.room_type_code;
        END IF;

        -- Chỉ gán phòng AVAILABLE, không lấy phòng đang có khách hoặc bị giữ.
        SELECT r.* INTO selected_room
        FROM rooms r
        WHERE r.room_type_id = selected_type.id
          AND r.status = 'AVAILABLE'
          AND NOT EXISTS (
              SELECT 1 FROM bookings b
              WHERE b.assigned_room_id = r.id
                AND (
                    b.status = 'CHECKED_IN'
                    OR (
                        b.check_in_date < hotel_today AND b.check_out_date > arrival_date
                        AND (b.status = 'CONFIRMED'
                             OR (b.status = 'PENDING' AND b.expired_at > LOCALTIMESTAMP))
                    )
                )
          )
        ORDER BY r.floor NULLS LAST, r.room_number, r.id
        LIMIT 1
        FOR UPDATE OF r SKIP LOCKED;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'Không còn phòng vật lý AVAILABLE cho hạng %.', sample.room_type_code;
        END IF;

        booking_id_value := gen_random_uuid();
        payment_id_value := gen_random_uuid();
        created_time := arrival_date + INTERVAL '10 hours';
        paid_time := created_time + INTERVAL '5 minutes';
        checked_in_time := arrival_date + INTERVAL '14 hours';

        -- Giá mẫu lấy giá cơ bản hiện tại; lưu snapshot một đêm tương ứng.
        INSERT INTO bookings (
            id, booking_code, room_type_id, assigned_room_id,
            check_in_date, check_out_date, adult_count, child_count,
            status, total_amount, paid_amount, expired_at, created_at, updated_at
        ) VALUES (
            booking_id_value, booking_code_value, selected_type.id, selected_room.id,
            arrival_date, hotel_today, 2, 0,
            'CHECKED_IN', selected_type.base_price, selected_type.base_price,
            NULL, created_time, checked_in_time
        );

        INSERT INTO booking_guests (
            reference_id, booking_id, full_name, phone, email,
            is_primary, created_at, updated_at
        ) VALUES (
            gen_random_uuid(), booking_id_value, sample.guest_name,
            sample.guest_phone, sample.guest_email, TRUE, created_time, created_time
        );

        INSERT INTO booking_nights (
            reference_id, booking_id, stay_date, price_per_night, created_at, updated_at
        ) VALUES (
            gen_random_uuid(), booking_id_value, arrival_date,
            selected_type.base_price, created_time, created_time
        );

        -- Ghi nhận thanh toán MÔ PHỎNG; không thực hiện chuyển tiền ngân hàng.
        INSERT INTO payments (
            id, booking_id, payment_code, amount, currency, status,
            idempotency_key, expires_at, paid_at, created_at, updated_at
        ) VALUES (
            payment_id_value, booking_id_value, payment_code_value,
            selected_type.base_price, 'VND', 'SUCCESS',
            'demo-departure:' || booking_code_value,
            created_time + INTERVAL '15 minutes', paid_time, created_time, paid_time
        );

        INSERT INTO payment_transactions (
            payment_id, transaction_ref, type, status, amount, currency,
            provider_message, raw_payload, occurred_at, received_at, created_at
        ) VALUES (
            payment_id_value, payment_code_value, 'PAYMENT', 'SUCCESS',
            selected_type.base_price, 'VND', 'Thanh toán mô phỏng cho dữ liệu checkout demo',
            jsonb_build_object('method', 'DEMO', 'source', 'seed-today-departures.sql'),
            paid_time, paid_time, paid_time
        );

        INSERT INTO booking_status_histories (
            reference_id, booking_id, old_status, new_status, reason, created_at, updated_at
        ) VALUES
            (gen_random_uuid(), booking_id_value, NULL, 'PENDING',
             'Dữ liệu mẫu checkout hôm nay: tạo booking', created_time, created_time),
            (gen_random_uuid(), booking_id_value, 'PENDING', 'CONFIRMED',
             'Dữ liệu mẫu checkout hôm nay: thanh toán mô phỏng thành công', paid_time, paid_time),
            (gen_random_uuid(), booking_id_value, 'CONFIRMED', 'CHECKED_IN',
             'Dữ liệu mẫu checkout hôm nay: gán phòng ' || selected_room.room_number,
             checked_in_time, checked_in_time);

        UPDATE rooms
        SET status = 'OCCUPIED', updated_at = LOCALTIMESTAMP
        WHERE id = selected_room.id;

        RAISE NOTICE 'Đã tạo % cho %, phòng %, trả ngày %.',
            booking_code_value, sample.guest_name, selected_room.room_number, hotel_today;
    END LOOP;
END;
$seed$;

SELECT b.booking_code, g.full_name, rt.name AS room_type, r.room_number,
       b.check_in_date, b.check_out_date, b.status,
       b.total_amount, b.paid_amount, b.total_amount - b.paid_amount AS outstanding_amount
FROM bookings b
JOIN booking_guests g ON g.booking_id = b.id AND g.is_primary
JOIN room_types rt ON rt.id = b.room_type_id
JOIN rooms r ON r.id = b.assigned_room_id
WHERE b.booking_code IN (
    'DEMO-OUT-' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-01',
    'DEMO-OUT-' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || '-02'
)
ORDER BY b.booking_code;

COMMIT;
