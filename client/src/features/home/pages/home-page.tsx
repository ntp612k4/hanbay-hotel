import { Button, Card, Col, DatePicker, Form, Grid, Row, Select, theme, Typography } from 'antd';
import dayjs, { type Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';

import { MainFooter } from '../../../components/layout/MainFooter';
import { MainHeader } from '../../../components/layout/MainHeader';
import { RoomTypeCatalog } from '../../room';

import './home-page.css';

const { Paragraph, Text, Title } = Typography;

const amenities = [
  { icon: '⌂', name: 'Nhà hàng Hanami', description: 'Ẩm thực Việt Nam và quốc tế' },
  { icon: '♨', name: 'Quán cà phê Momo', description: 'Cà phê thơm ngon bên bờ biển' },
  { icon: '◇', name: 'Sky Bar & Tapas', description: 'Cocktail và tầm nhìn trên cao' },
  { icon: '❀', name: 'Anona Spa', description: 'Thư giãn cơ thể và tâm trí' },
  { icon: '≋', name: 'Hồ bơi vô cực', description: 'Ngắm biển Mỹ Khê bất tận' },
  { icon: '↔', name: 'Phòng gym hiện đại', description: 'Không gian tập luyện cân bằng' },
];

interface SearchFormValues {
  checkIn: Dayjs;
  checkOut: Dayjs;
  adults: number;
  children: number;
}

export function HomePage() {
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const navigate = useNavigate();
  const [searchForm] = Form.useForm<SearchFormValues>();
  const today = dayjs().startOf('day');
  const defaultCheckIn = today;
  const selectedCheckIn = Form.useWatch('checkIn', searchForm) ?? defaultCheckIn;

  const searchAvailableRooms = (values: SearchFormValues) => {
    const params = new URLSearchParams({
      checkIn: values.checkIn.format('YYYY-MM-DD'),
      checkOut: values.checkOut.format('YYYY-MM-DD'),
      adults: String(values.adults),
      children: String(values.children),
    });
    navigate(`/rooms/search?${params.toString()}`);
  };

  const sectionSpacing = screens.md ? 96 : 64;
  const numberOptions = (start: number, end: number, suffix: string) =>
    Array.from({ length: end - start + 1 }, (_, index) => {
      const value = start + index;
      return { value, label: `${value} ${suffix}` };
    });

  return (
    <div className="hotel-home" style={{ background: token.colorBgBase }}>
      <MainHeader />
      <main>
        <section className="home-hero" aria-label="HANBAY Hotel bên bờ biển Đà Nẵng">
          <div className="home-hero__content">
            <Text className="home-kicker" style={{ color: token.colorWhite }}>
              HANBAY HOTEL · ĐÀ NẴNG
            </Text>
            <Title
              style={{ maxWidth: 720, margin: `${token.marginSM}px 0`, color: token.colorWhite }}
            >
              Nơi biển trời chạm đến sự bình yên
            </Title>
            <Text style={{ color: token.colorWhite, fontSize: token.fontSizeLG }}>
              Trải nghiệm kỳ nghỉ tinh tế bên bờ biển Mỹ Khê.
            </Text>
          </div>
        </section>

        <section
          id="booking"
          className="booking-search-card"
          aria-label="Kiểm tra phòng trống"
          style={{
            border: `${token.lineWidth}px solid ${token.colorBorderSecondary}`,
            background: token.colorBgContainer,
            boxShadow: token.boxShadowSecondary,
          }}
        >
          <Form<SearchFormValues>
            form={searchForm}
            className="booking-search-form"
            layout="vertical"
            requiredMark={false}
            initialValues={{
              checkIn: defaultCheckIn,
              checkOut: defaultCheckIn.add(1, 'day'),
              adults: 2,
              children: 0,
            }}
            onFinish={searchAvailableRooms}
          >
            <Form.Item label="Nhận phòng" name="checkIn" rules={[{ required: true }]}>
              <DatePicker
                className="booking-search-control"
                format="DD/MM/YYYY"
                disabledDate={(date) => date.startOf('day').isBefore(today)}
                onChange={() => {
                  const checkOut = searchForm.getFieldValue('checkOut');
                  const checkIn = searchForm.getFieldValue('checkIn');
                  if (checkIn && checkOut && !checkOut.isAfter(checkIn)) {
                    searchForm.setFieldValue('checkOut', checkIn.add(1, 'day'));
                  }
                }}
              />
            </Form.Item>
            <Form.Item label="Trả phòng" name="checkOut" rules={[{ required: true }]}>
              <DatePicker
                className="booking-search-control"
                format="DD/MM/YYYY"
                disabledDate={(date) =>
                  !date.startOf('day').isAfter(selectedCheckIn.startOf('day'))
                }
              />
            </Form.Item>
            <Form.Item label="Người lớn" name="adults">
              <Select options={numberOptions(1, 12, 'người lớn')} />
            </Form.Item>
            <Form.Item label="Trẻ em" name="children">
              <Select options={numberOptions(0, 8, 'trẻ em')} />
            </Form.Item>
            <Button className="booking-search-submit" type="primary" htmlType="submit">
              Kiểm tra phòng
            </Button>
          </Form>
        </section>

        <section
          className="home-intro home-container"
          style={{ paddingBlock: sectionSpacing, textAlign: 'center' }}
        >
          <SectionHeading
            kicker="MỘT NƠI ĐỂ DỪNG LẠI, MỘT NƠI ĐỂ BẮT ĐẦU"
            title="Chào mừng bạn đến với HANBAY"
          />
          <Paragraph style={{ maxWidth: 760, color: token.colorTextSecondary, lineHeight: 1.9 }}>
            Tọa lạc bên bãi biển Mỹ Khê, HANBAY mang đến không gian nghỉ dưỡng nhẹ nhàng giữa nhịp
            sống Đà Nẵng. Mỗi căn phòng được chăm chút để đón ánh sáng, gió biển và tạo nên cảm giác
            thân thuộc trong suốt hành trình của bạn.
          </Paragraph>
          <Button type="primary" href="#rooms">
            Khám phá hạng phòng
          </Button>
        </section>

        <RoomTypeCatalog />

        <section id="amenities" className="home-container" style={{ paddingBlock: sectionSpacing }}>
          <SectionHeading
            kicker="TIỆN NGHI KHÁCH SẠN"
            title="Mọi trải nghiệm bạn cần trong một kỳ nghỉ"
            description="Những tiện ích được kết nối cân bằng giữa nghỉ ngơi, ẩm thực và khám phá."
          />
          <div className="amenities-grid">
            {amenities.map((amenity) => (
              <article className="amenity-item" key={amenity.name}>
                <Button shape="circle" size="large" aria-hidden tabIndex={-1}>
                  {amenity.icon}
                </Button>
                <Text strong>{amenity.name}</Text>
                <Text type="secondary">{amenity.description}</Text>
              </article>
            ))}
          </div>
        </section>

        <section
          className="experience-section"
          style={{ paddingBlock: sectionSpacing, background: token.colorBgContainer }}
        >
          <div className="home-container experience-list">
            <ExperienceCard
              kicker="ẨM THỰC"
              title="Hương vị làm nên những cuộc gặp gỡ"
              description="Bắt đầu ngày mới bằng bữa sáng đầy năng lượng và khép lại buổi tối với thực đơn được tạo nên từ nguyên liệu địa phương tươi ngon."
              image="https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1400&q=85"
              imageAlt="Không gian nhà hàng HANBAY"
              action="Khám phá nhà hàng"
            />
            <ExperienceCard
              reverse
              kicker="HÀNH TRÌNH SỨC KHỎE"
              title="Chạm vào sự tĩnh tại"
              description="Những liệu trình spa dịu nhẹ, không gian ấm áp và mùi hương thư thái giúp bạn tái tạo năng lượng sau một ngày khám phá thành phố biển."
              image="https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=1400&q=85"
              imageAlt="Trải nghiệm spa tại HANBAY"
              action="Đặt lịch thư giãn"
            />
          </div>
        </section>

        <section className="home-cta">
          <div className="home-cta__content">
            <Text className="home-kicker" style={{ color: token.colorWhite }}>
              KỲ NGHỈ CỦA BẠN ĐANG CHỜ ĐỢI
            </Text>
            <Title level={2} style={{ maxWidth: 650, margin: 0, color: token.colorWhite }}>
              Tìm một khoảng trời riêng tại Đà Nẵng
            </Title>
            <Button ghost href="#booking">
              Đặt phòng ngay
            </Button>
          </div>
        </section>
      </main>
      <MainFooter />
    </div>
  );
}

interface SectionHeadingProps {
  kicker: string;
  title: string;
  description?: string;
}

function SectionHeading({ kicker, title, description }: SectionHeadingProps) {
  const { token } = theme.useToken();

  return (
    <header className="home-section-heading" style={{ marginBottom: token.marginXL }}>
      <Text className="home-kicker" style={{ color: token.colorPrimary }}>
        {kicker}
      </Text>
      <Title level={2} style={{ margin: `${token.marginXS}px 0` }}>
        {title}
      </Title>
      {description && <Paragraph type="secondary">{description}</Paragraph>}
    </header>
  );
}

interface ExperienceCardProps {
  kicker: string;
  title: string;
  description: string;
  image: string;
  imageAlt: string;
  action: string;
  reverse?: boolean;
}

function ExperienceCard(props: ExperienceCardProps) {
  const { token } = theme.useToken();
  const copy = (
    <Col
      key="copy"
      xs={24}
      md={12}
      className="experience-copy"
      style={{ padding: token.paddingXL }}
    >
      <Text className="home-kicker" style={{ color: token.colorPrimary }}>
        {props.kicker}
      </Text>
      <Title level={2}>{props.title}</Title>
      <Paragraph type="secondary" style={{ lineHeight: 1.8 }}>
        {props.description}
      </Paragraph>
      <Button href="#contact">{props.action}</Button>
    </Col>
  );
  const image = (
    <Col key="image" xs={24} md={12}>
      <img className="experience-image" src={props.image} alt={props.imageAlt} loading="lazy" />
    </Col>
  );

  return (
    <Card styles={{ body: { padding: 0, overflow: 'hidden' } }}>
      <Row>{props.reverse ? [image, copy] : [copy, image]}</Row>
    </Card>
  );
}
