-- ============================================================
-- HAN-22 - Chi tiết loại phòng
--
-- IMPORTANT:
-- room_types and room_type_images are created by
-- V202607291337__create_booking_domain_tables.sql.
--
-- This migration only adds data/tables required by HAN-22.
-- ============================================================


-- ============================================================
-- 1. AMENITIES
-- ============================================================

CREATE TABLE amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    icon VARCHAR(100) NOT NULL
);


-- ============================================================
-- 2. ROOM TYPE - AMENITY RELATIONSHIP
-- ============================================================

CREATE TABLE room_type_amenities (
    room_type_id UUID NOT NULL,
    amenity_id UUID NOT NULL,

    PRIMARY KEY (room_type_id, amenity_id),

    CONSTRAINT fk_room_type_amenities_room_type
        FOREIGN KEY (room_type_id)
        REFERENCES room_types(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_room_type_amenities_amenity
        FOREIGN KEY (amenity_id)
        REFERENCES amenities(id)
        ON DELETE CASCADE
);


CREATE INDEX idx_room_type_amenities_room_type_id
    ON room_type_amenities(room_type_id);

CREATE INDEX idx_room_type_amenities_amenity_id
    ON room_type_amenities(amenity_id);


-- ============================================================
-- 3. DEMO ROOM TYPE DATA
--
-- room_types already exists from develop.
-- Current schema:
-- id
-- code
-- name
-- description
-- base_price
-- max_guests
-- bed_type
-- area_m2
-- status
-- created_at
-- updated_at
-- ============================================================

INSERT INTO room_types (
    id,
    code,
    name,
    description,
    base_price,
    max_guests,
    capacity_adults,
    capacity_children,
    bed_type,
    area_m2,
    status,
    created_at,
    updated_at
)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'HANBAY-OCEAN-SUITE',
    'Phòng Suite Hướng Biển Cao Cấp',
    'Trải nghiệm sự xa hoa tột bậc với tầm nhìn bao quát đại dương. Không gian được thiết kế tinh tế với nội thất cao cấp, kết hợp giữa phong cách tối giản hiện đại và những điểm nhấn nghệ thuật độc bản. Phòng Suite rộng 85m² mang đến sự riêng tư tuyệt đối, ánh sáng tự nhiên ngập tràn và mọi tiện nghi cần thiết cho một kỳ nghỉ đáng hoàn hảo.',
    8500000,
    2,
    2,
    0,
    '1 giường King',
    85.00,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. DEMO ROOM TYPE IMAGES
--
-- room_type_images already exists from develop.
--
-- Current schema:
-- id
-- reference_id
-- room_type_id
-- image_url
-- sort_order
-- created_at
-- updated_at
-- ============================================================

INSERT INTO room_type_images (
    id,
    reference_id,
    room_type_id,
    image_url,
    sort_order,
    created_at,
    updated_at
)
VALUES
(
    '11000000-0000-0000-0000-000000000001',
    '11100000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=1800&q=88',
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '11000000-0000-0000-0000-000000000002',
    '11100000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000001',
    'https://images.unsplash.com/photo-1618773928121-c32242e63f39?auto=format&fit=crop&w=1200&q=85',
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '11000000-0000-0000-0000-000000000003',
    '11100000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000001',
    'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=85',
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);


-- ============================================================
-- 5. DEMO AMENITIES
-- ============================================================

INSERT INTO amenities (
    id,
    name,
    icon
)
VALUES
(
    '12000000-0000-0000-0000-000000000001',
    'Wi-Fi tốc độ cao',
    'wifi'
),
(
    '12000000-0000-0000-0000-000000000002',
    'Tầm nhìn toàn cảnh',
    'panorama'
),
(
    '12000000-0000-0000-0000-000000000003',
    'Giường King-size',
    'bed'
),
(
    '12000000-0000-0000-0000-000000000004',
    'Mini-bar miễn phí',
    'minibar'
),
(
    '12000000-0000-0000-0000-000000000005',
    'Smart TV 65"',
    'tv'
),
(
    '12000000-0000-0000-0000-000000000006',
    'Dịch vụ 24/7',
    'service'
);


-- ============================================================
-- 6. LINK AMENITIES TO DEMO ROOM TYPE
-- ============================================================

INSERT INTO room_type_amenities (
    room_type_id,
    amenity_id
)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    '12000000-0000-0000-0000-000000000001'
),
(
    '10000000-0000-0000-0000-000000000001',
    '12000000-0000-0000-0000-000000000002'
),
(
    '10000000-0000-0000-0000-000000000001',
    '12000000-0000-0000-0000-000000000003'
),
(
    '10000000-0000-0000-0000-000000000001',
    '12000000-0000-0000-0000-000000000004'
),
(
    '10000000-0000-0000-0000-000000000001',
    '12000000-0000-0000-0000-000000000005'
),
(
    '10000000-0000-0000-0000-000000000001',
    '12000000-0000-0000-0000-000000000006'
);
