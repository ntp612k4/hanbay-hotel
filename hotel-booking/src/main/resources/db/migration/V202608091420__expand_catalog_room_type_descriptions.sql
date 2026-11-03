-- ============================================================
-- HAN-22 - Detailed descriptions for catalog room types
--
-- Keep this change in a new migration because previous Flyway
-- migrations may already have been applied to local databases.
-- ============================================================

UPDATE room_types
SET
    description = CASE id
        WHEN 'a1000000-0000-4000-8000-000000000001' THEN
            'Phòng Superior mang đến không gian nghỉ dưỡng hiện đại, sáng thoáng và cân bằng giữa sự tiện nghi với cảm giác riêng tư. Ánh sáng tự nhiên đi qua khung cửa lớn làm nổi bật bảng màu trung tính, nội thất gọn gàng và khu vực nghỉ ngơi được bố trí hợp lý. Phòng phù hợp cho cặp đôi, khách công tác hoặc gia đình nhỏ cần một nơi lưu trú yên tĩnh sau ngày dài khám phá Đà Nẵng. Wi-Fi tốc độ cao, giường ngủ êm ái, Smart TV và dịch vụ hỗ trợ 24/7 giúp mọi nhu cầu cơ bản luôn được đáp ứng trọn vẹn.'
        WHEN 'a1000000-0000-4000-8000-000000000002' THEN
            'Phòng Deluxe nổi bật với ban công riêng hướng biển, nơi khách lưu trú có thể đón ánh nắng buổi sớm và tận hưởng không khí trong lành từ vịnh Đà Nẵng. Không gian sử dụng chất liệu gỗ ấm, đường nét thanh lịch và khu vực nghỉ ngơi rộng rãi, tạo cảm giác sang trọng nhưng vẫn gần gũi. Phòng đáp ứng tối đa bốn khách, phù hợp cho cặp đôi hoặc gia đình muốn có kỳ nghỉ thoải mái. Giường King-size, mini-bar, Smart TV, Wi-Fi tốc độ cao và tầm nhìn toàn cảnh hoàn thiện trải nghiệm lưu trú tiện nghi.'
        WHEN 'a1000000-0000-4000-8000-000000000003' THEN
            'Junior Suite được thiết kế như một căn hộ nghỉ dưỡng thu nhỏ với khu vực phòng ngủ và phòng khách tách biệt, mang lại sự thoải mái cho những kỳ lưu trú dài ngày. Cửa sổ rộng mở ra đường chân trời của vịnh Đà Nẵng, kết hợp cùng ánh sáng tự nhiên và nội thất tinh tế để tạo nên không gian thư giãn đầy cảm hứng. Sức chứa tối đa năm khách phù hợp cho gia đình hoặc nhóm bạn cần thêm diện tích sinh hoạt. Hệ thống tiện nghi đầy đủ gồm Wi-Fi tốc độ cao, giường King-size, mini-bar, Smart TV, tầm nhìn toàn cảnh và dịch vụ hỗ trợ 24/7.'
        ELSE description
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    'a1000000-0000-4000-8000-000000000001',
    'a1000000-0000-4000-8000-000000000002',
    'a1000000-0000-4000-8000-000000000003'
);
