import {
  ArrowLeftOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Flex,
  Form,
  Input,
  InputNumber,
  Row,
  Skeleton,
  Space,
  theme,
  Typography,
} from 'antd';
import type { Dayjs } from 'dayjs';

import { BookingSummary } from '../components/booking-summary';
import type { AddOnCatalogItem, RoomTypeCatalogItem, SelectedAddOn } from '../types/booking';

interface GuestInformationPageProps {
  roomType?: RoomTypeCatalogItem;
  stayDates?: [Dayjs, Dayjs];
  adults?: number;
  children?: number;
  addOns?: AddOnCatalogItem[];
  selectedAddOns: SelectedAddOn[];
  addOnQuantities: Record<string, number>;
  addOnsLoading: boolean;
  addOnsError: boolean;
  submitting: boolean;
  onQuantityChange: (addOn: AddOnCatalogItem, quantity: number) => void;
  onRetryAddOns: () => void;
  onBack: () => void;
  onSubmit: () => void;
}

export function GuestInformationPage(props: GuestInformationPageProps) {
  const { token } = theme.useToken();
  const { roomType, stayDates, adults, children, addOns, selectedAddOns, addOnQuantities } = props;

  return (
    <section className="page-enter">
      <Typography.Text strong style={{ display: 'block', marginBottom: 28 }}>
        Thông tin khách hàng
      </Typography.Text>
      <Row gutter={[{ xs: 16, sm: 24, xl: 40 }, 32]}>
        <Col xs={24} xl={16}>
          <Card
            style={{ marginBottom: 30, borderColor: token.colorBorder }}
            title={
              <Space>
                <UserOutlined /> Thông tin khách chính
              </Space>
            }
          >
            <Row gutter={20}>
              <Col xs={24} md={12}>
                <Form.Item
                  label="Họ và tên"
                  name="fullName"
                  rules={[{ required: true, whitespace: true, message: 'Vui lòng nhập họ tên.' }]}
                >
                  <Input size="large" placeholder="VD: Nguyễn Văn A" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item
                  label="Địa chỉ email"
                  name="email"
                  rules={[
                    { required: true, message: 'Vui lòng nhập email.' },
                    { type: 'email', message: 'Email chưa đúng định dạng.' },
                  ]}
                >
                  <Input size="large" type="email" placeholder="email@vi-du.com" />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item
                  label="Số điện thoại"
                  name="phone"
                  rules={[
                    { pattern: /^0\d{9}$/, message: 'Số điện thoại gồm 10 số và bắt đầu bằng 0.' },
                  ]}
                >
                  <Input size="large" inputMode="tel" placeholder="090 000 0000" maxLength={10} />
                </Form.Item>
              </Col>
              <Col xs={24} md={12}>
                <Form.Item
                  label="CCCD / giấy tờ tùy thân"
                  name="identityNumber"
                  rules={[
                    {
                      required: true,
                      whitespace: true,
                      message: 'Vui lòng nhập giấy tờ tùy thân.',
                    },
                  ]}
                >
                  <Input size="large" placeholder="012345678901" />
                </Form.Item>
              </Col>
            </Row>
          </Card>

          <Card
            style={{ marginBottom: 30, borderColor: token.colorBorder }}
            title={
              <Space>
                <TeamOutlined /> Số lượng khách
              </Space>
            }
          >
            <Row gutter={24}>
              <Col xs={24} md={12}>
                <div className="guest-counter">
                  <div>
                    <strong>Người lớn</strong>
                    <span>Trên 12 tuổi</span>
                  </div>
                  <Form.Item name="adults" noStyle rules={[{ required: true }]}>
                    <InputNumber min={1} max={20} controls />
                  </Form.Item>
                </div>
              </Col>
              <Col xs={24} md={12}>
                <div className="guest-counter">
                  <div>
                    <strong>Trẻ em</strong>
                    <span>Từ 12 tuổi trở xuống · miễn phí tiền phòng</span>
                  </div>
                  <Form.Item name="children" noStyle rules={[{ required: true }]}>
                    <InputNumber min={0} max={20} controls />
                  </Form.Item>
                </div>
              </Col>
            </Row>
          </Card>

          <Space orientation="vertical" size={2} style={{ marginBottom: 20 }}>
            <Typography.Text strong>Nâng tầm kỳ nghỉ của bạn</Typography.Text>
            <Typography.Text type="secondary">Chọn dịch vụ bổ sung nếu cần.</Typography.Text>
          </Space>
          {props.addOnsLoading ? (
            <Skeleton active paragraph={{ rows: 3 }} />
          ) : props.addOnsError ? (
            <Alert
              type="error"
              showIcon
              message="Không tải được danh mục dịch vụ"
              action={<Button onClick={props.onRetryAddOns}>Thử lại</Button>}
            />
          ) : addOns?.length ? (
            <Row gutter={[14, 14]}>
              {addOns.map((addOn) => {
                const quantity = addOnQuantities[addOn.id] ?? 0;
                const selected = quantity > 0;
                return (
                  <Col xs={24} sm={12} lg={8} key={addOn.id}>
                    <Card
                      className="addon-card-interactive"
                      style={{
                        height: '100%',
                        minHeight: 190,
                        borderColor: selected ? token.colorPrimary : token.colorBorder,
                        background: selected ? token.colorFillAlter : token.colorBgContainer,
                      }}
                    >
                      <Flex vertical gap={8} style={{ minHeight: 142 }}>
                        <Flex justify="space-between" gap={8}>
                          <Typography.Text strong>{addOn.name}</Typography.Text>
                          <InputNumber
                            aria-label={`Số lượng ${addOn.name}`}
                            min={0}
                            max={20}
                            value={quantity}
                            onChange={(value) => props.onQuantityChange(addOn, value ?? 0)}
                          />
                        </Flex>
                        <Typography.Paragraph
                          type="secondary"
                          ellipsis={{ rows: 2 }}
                          style={{ margin: 0 }}
                        >
                          {addOn.description || addOn.code}
                        </Typography.Paragraph>
                        <Space size={4} style={{ marginTop: 'auto' }}>
                          <Typography.Text strong>{formatMoney(addOn.price)}</Typography.Text>
                          <Typography.Text type="secondary">/ đơn vị</Typography.Text>
                        </Space>
                      </Flex>
                    </Card>
                  </Col>
                );
              })}
            </Row>
          ) : (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description="Chưa có dịch vụ đang hoạt động"
            />
          )}
        </Col>

        <Col xs={24} xl={8}>
          <BookingSummary
            roomType={roomType}
            stayDates={stayDates}
            adults={adults}
            children={children}
            addOns={selectedAddOns}
            submitting={props.submitting}
            onSubmit={props.onSubmit}
          />
          <Typography.Paragraph
            type="secondary"
            style={{
              marginTop: 20,
              fontSize: 11,
              letterSpacing: '0.12em',
              textAlign: 'center',
              textTransform: 'uppercase',
            }}
          >
            <SafetyCertificateOutlined /> Thông tin của bạn được bảo mật
          </Typography.Paragraph>
        </Col>
      </Row>
      <Button
        style={{ marginTop: 20, paddingInline: 0 }}
        type="link"
        icon={<ArrowLeftOutlined />}
        onClick={props.onBack}
      >
        Thay đổi hạng phòng
      </Button>
    </section>
  );
}

function formatMoney(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
}
