-- ============================================================
-- HAN-22 - Separate hero copy from detailed room content
-- ============================================================

ALTER TABLE room_types
    ADD COLUMN short_description TEXT;

UPDATE room_types
SET
    short_description = CASE id
        WHEN '10000000-0000-0000-0000-000000000001' THEN
            'Một kỳ nghỉ riêng tư trên tầng cao với tầm nhìn đại dương rộng mở và dịch vụ được chăm chút theo từng trải nghiệm.'
        WHEN 'a1000000-0000-4000-8000-000000000001' THEN
            'Không gian sáng thoáng và yên tĩnh dành cho những chuyến công tác, kỳ nghỉ đôi hoặc gia đình nhỏ tại Đà Nẵng.'
        WHEN 'a1000000-0000-4000-8000-000000000002' THEN
            'Thức dậy cùng ánh nắng trên ban công hướng biển và tận hưởng nhịp nghỉ dưỡng nhẹ nhàng trong căn phòng thanh lịch.'
        WHEN 'a1000000-0000-4000-8000-000000000003' THEN
            'Tận hưởng sự riêng tư của phòng ngủ riêng cùng khu tiếp khách rộng rãi trong một không gian nhìn ra vịnh Đà Nẵng.'
        ELSE description
    END,
    description = CASE id
        WHEN '10000000-0000-0000-0000-000000000001' THEN
            E'Phòng Suite Hướng Biển Cao Cấp sở hữu diện tích 85m² với phòng ngủ, khu tiếp khách và các khoảng nghỉ được kết nối liền mạch. Nội thất tối giản sử dụng chất liệu cao cấp, bảng màu ấm và những điểm nhấn nghệ thuật tạo nên cảm giác sang trọng nhưng không phô trương.\n\nKhung cửa kính lớn đưa ánh sáng tự nhiên vào phòng và mở ra tầm nhìn bao quát đại dương. Từ buổi sớm trong trẻo đến hoàng hôn trên biển, mỗi thời điểm trong ngày đều trở thành một phần đáng nhớ của trải nghiệm lưu trú.\n\nSuite phù hợp cho khách tìm kiếm sự riêng tư, không gian rộng và dịch vụ chỉn chu. Giường King-size, mini-bar, Smart TV, Wi-Fi tốc độ cao, tầm nhìn toàn cảnh và hỗ trợ 24/7 đáp ứng đầy đủ nhu cầu nghỉ dưỡng dài ngày.'
        WHEN 'a1000000-0000-4000-8000-000000000001' THEN
            E'Phòng Superior được thiết kế theo phong cách hiện đại với bảng màu trung tính, nội thất gọn gàng và cách bố trí tối ưu diện tích. Cửa sổ lớn đón ánh sáng tự nhiên, giúp căn phòng luôn sáng thoáng và mang lại cảm giác thư thái ngay khi bước vào.\n\nKhu vực nghỉ ngơi, bàn làm việc và các tiện ích được sắp xếp hợp lý để khách có thể chuyển đổi nhẹ nhàng giữa làm việc và thư giãn. Đây là lựa chọn phù hợp cho cặp đôi, khách công tác hoặc gia đình nhỏ muốn ở gần biển nhưng vẫn cần sự yên tĩnh.\n\nWi-Fi tốc độ cao, giường ngủ êm ái, Smart TV và dịch vụ hỗ trợ 24/7 đáp ứng những nhu cầu thiết yếu trong suốt kỳ lưu trú. Mọi chi tiết đều hướng đến một trải nghiệm tiện nghi, đơn giản và dễ chịu.'
        WHEN 'a1000000-0000-4000-8000-000000000002' THEN
            E'Phòng Deluxe sử dụng chất liệu gỗ ấm, đường nét thanh lịch và nội thất mềm mại để tạo nên không gian nghỉ ngơi sang trọng nhưng gần gũi. Khu vực giường ngủ rộng rãi kết hợp cùng góc thư giãn riêng giúp khách tận hưởng trọn vẹn thời gian bên trong phòng.\n\nBan công riêng mở về phía biển là điểm nhấn của hạng phòng này. Khách có thể đón ánh nắng buổi sớm, cảm nhận không khí trong lành từ vịnh Đà Nẵng hoặc dành một buổi tối yên tĩnh bên người thân.\n\nPhòng đáp ứng tối đa bốn khách, phù hợp cho cặp đôi hoặc gia đình. Giường King-size, mini-bar, Smart TV, Wi-Fi tốc độ cao và tầm nhìn toàn cảnh hoàn thiện trải nghiệm lưu trú thoải mái.'
        WHEN 'a1000000-0000-4000-8000-000000000003' THEN
            E'Junior Suite được tổ chức như một căn hộ nghỉ dưỡng thu nhỏ với khu vực phòng ngủ và phòng khách tách biệt. Cách phân chia này tạo thêm sự riêng tư, đồng thời mang đến diện tích sinh hoạt thoải mái cho những kỳ nghỉ nhiều ngày.\n\nCửa sổ rộng hướng ra đường chân trời của vịnh Đà Nẵng, đưa ánh sáng tự nhiên vào từng khu vực. Nội thất tinh tế và các khoảng ngồi thư giãn tạo điều kiện để gia đình hoặc nhóm bạn quây quần mà không ảnh hưởng đến không gian nghỉ ngơi.\n\nVới sức chứa tối đa năm khách, Junior Suite phù hợp cho gia đình và nhóm bạn. Wi-Fi tốc độ cao, giường King-size, mini-bar, Smart TV, tầm nhìn toàn cảnh và dịch vụ 24/7 mang lại trải nghiệm đầy đủ và linh hoạt.'
        ELSE description
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE id IN (
    '10000000-0000-0000-0000-000000000001',
    'a1000000-0000-4000-8000-000000000001',
    'a1000000-0000-4000-8000-000000000002',
    'a1000000-0000-4000-8000-000000000003'
);
