import { Typography } from 'antd';

const { Text, Title } = Typography;

export function RoomSearchHero() {
  return (
    <section className="search-hero" aria-labelledby="room-search-title">
      <div className="search-hero-content">
        <Text className="eyebrow">Kỳ nghỉ ven biển tại Đà Nẵng</Text>
        <Title id="room-search-title" className="hero-title">
          Tìm hạng phòng phù hợp cho kỳ lưu trú của bạn
        </Title>
      </div>
    </section>
  );
}
