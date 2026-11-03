INSERT INTO room_types (
    id,
    code,
    name,
    description,
    capacity_adults,
    capacity_children,
    base_price,
    thumbnail_url,
    active,
    max_guests
)
VALUES
    (
        'a1000000-0000-4000-8000-000000000001',
        'SUPERIOR',
        'Phòng Superior',
        'Không gian hiện đại, tràn ngập ánh sáng tự nhiên và đầy đủ tiện nghi cho một kỳ nghỉ thư thái.',
        2,
        1,
        1850000.00,
        'https://images.unsplash.com/photo-1611892440504-42a792e24d32?auto=format&fit=crop&w=1200&q=85',
        TRUE,
        3
    ),
    (
        'a1000000-0000-4000-8000-000000000002',
        'DELUXE',
        'Phòng Deluxe',
        'Ban công riêng hướng biển, nội thất thanh lịch và chiếc giường êm ái dành cho những ngày nghỉ trọn vẹn.',
        2,
        2,
        2450000.00,
        'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=85',
        TRUE,
        4
    ),
    (
        'a1000000-0000-4000-8000-000000000003',
        'JUNIOR_SUITE',
        'Junior Suite',
        'Phòng ngủ và khu tiếp khách rộng rãi, mở ra đường chân trời của vịnh Đà Nẵng tuyệt đẹp.',
        3,
        2,
        3600000.00,
        'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=85',
        TRUE,
        5
    );
