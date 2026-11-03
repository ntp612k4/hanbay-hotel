import { Button, theme, Typography } from 'antd';

import { MainFooter } from '../../../components/layout/MainFooter';
import { MainHeader } from '../../../components/layout/MainHeader';
import { RoomTypeCatalog } from '../components/room-type-catalog';

import './room-types-page.css';

export function RoomTypesPage() {
  const { token } = theme.useToken();

  return (
    <div className="room-types-page" style={{ background: token.colorBgBase }}>
      <MainHeader />
      <main>
        <section className="room-types-hero" style={{ background: token.colorBgContainer }}>
          <div className="room-types-hero__content">
            <Typography.Text
              className="room-types-hero__kicker"
              style={{ color: token.colorPrimary }}
            >
              PHÒNG & SUITE
            </Typography.Text>
            <Typography.Title style={{ marginBlock: token.marginSM }}>
              Không gian nghỉ dưỡng dành cho bạn
            </Typography.Title>
            <Typography.Paragraph
              style={{
                maxWidth: 680,
                marginBottom: token.marginLG,
                color: token.colorTextSecondary,
              }}
            >
              Khám phá các hạng phòng đang kinh doanh tại HANBAY, so sánh sức chứa và mức giá để
              chọn không gian phù hợp cho kỳ lưu trú của bạn.
            </Typography.Paragraph>
            <Button type="primary" href="#rooms">
              Xem các hạng phòng
            </Button>
          </div>
          <div className="room-types-hero__image" role="img" aria-label="Phòng nghỉ tại HANBAY" />
        </section>

        <RoomTypeCatalog />
      </main>
      <MainFooter />
    </div>
  );
}
