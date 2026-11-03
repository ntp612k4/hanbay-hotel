import {
  LoginOutlined,
  LogoutOutlined,
  UserOutlined,
  DashboardOutlined,
  BankOutlined,
  CreditCardOutlined,
  CalendarOutlined,
  HomeOutlined,
} from '@ant-design/icons';
import { Avatar, Button, Dropdown, Flex, Grid, Layout, theme, Typography, Space } from 'antd';
import { Link, useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '../../features/authentication/providers/auth-context';
import { getLatestPaymentResultId } from '../../features/booking/utils/payment-result-storage';
import { ROUTE_PATHS } from '../../routes/routePaths';

import './main-layout.css';

export function MainHeader() {
  const { token } = theme.useToken();
  const screens = Grid.useBreakpoint();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { isAuthenticated, roles, logout, session } = useAuth();
  const activeNavKey = getActiveNavKey(pathname);
  const latestPaymentId = getLatestPaymentResultId();

  return (
    <Layout.Header
      className="main-header"
      style={{
        height: 72,
        paddingInline: screens.md ? token.paddingXL : token.padding,
        borderBottom: `${token.lineWidth}px solid ${token.colorBorderSecondary}`,
        background: token.colorBgElevated,
        lineHeight: 'normal',
      }}
    >
      <Flex
        align="center"
        justify="space-between"
        gap={token.marginLG}
        style={{ width: '100%', maxWidth: 1440, height: '100%', marginInline: 'auto' }}
      >
        <Link to="/" aria-label="HANBAY Hotel - Trang chủ">
          <Typography.Text
            strong
            style={{
              color: token.colorPrimary,
              fontSize: token.fontSizeHeading4,
              letterSpacing: '0.12em',
            }}
          >
            HANBAY
          </Typography.Text>
        </Link>

        {screens.md && (
          <nav aria-label="Điều hướng chính" className="main-header__nav">
            <Flex justify="center" gap={token.marginXS}>
              {[
                { key: 'home', label: 'Tổng quan', route: '/' },
                { key: 'booking', label: 'Đặt phòng', route: '/rooms/search' },
                { key: 'room-types', label: 'Hạng phòng', route: '/room-types' },
              ].map((item) => (
                <Button
                  key={item.key}
                  type={activeNavKey === item.key ? 'primary' : 'text'}
                  aria-current={activeNavKey === item.key ? 'page' : undefined}
                  onClick={() => navigate(item.route)}
                >
                  {item.label}
                </Button>
              ))}
            </Flex>
          </nav>
        )}

        {isAuthenticated ? (
          <Dropdown
            trigger={['click']}
            placement="bottomRight"
            menu={{
              items: [
                {
                  key: 'profile',
                  label: (
                    <div style={{ padding: '8px 12px', maxWidth: 260 }}>
                      <Space align="start" size={12}>
                        <Avatar
                          size={40}
                          icon={<UserOutlined />}
                          style={{ backgroundColor: token.colorPrimary }}
                        />
                        <div style={{ overflow: 'hidden' }}>
                          <Typography.Text strong style={{ display: 'block' }}>
                            {session?.user?.fullName ?? 'Tài khoản'}
                          </Typography.Text>
                          <Typography.Text
                            type="secondary"
                            style={{
                              display: 'block',
                              whiteSpace: 'nowrap',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                            }}
                          >
                            {session?.user?.email ?? ''}
                          </Typography.Text>
                        </div>
                      </Space>
                    </div>
                  ),
                  disabled: true,
                },
                { type: 'divider' },
                ...(roles.some((r) => r === 'ADMIN' || r === 'MANAGER')
                  ? [
                      { key: 'nav_dashboard', label: 'Dashboard', icon: <DashboardOutlined /> },
                      {
                        key: 'nav_payment_monitoring',
                        label: 'Giám sát thanh toán',
                        icon: <BankOutlined />,
                      },
                    ]
                  : []),
                ...(roles.includes('RECEPTIONIST')
                  ? [
                      {
                        key: 'nav_bookings',
                        label: 'Quản lý đặt phòng',
                        icon: <CalendarOutlined />,
                      },
                    ]
                  : []),
                ...(!roles.length
                  ? [
                      {
                        key: 'guest_bookings',
                        label: 'Lịch sử đặt phòng',
                        icon: <CalendarOutlined />,
                      },
                    ]
                  : []),
                ...(roles.includes('HOUSEKEEPER')
                  ? [
                      {
                        key: 'nav_housekeeping',
                        label: 'Quản lý dọn phòng',
                        icon: <HomeOutlined />,
                      },
                    ]
                  : []),
                ...(latestPaymentId
                  ? [
                      {
                        key: 'resume_payment',
                        label: 'Tiếp tục thanh toán',
                        icon: <CreditCardOutlined />,
                      },
                    ]
                  : []),
                { type: 'divider' },
                { key: 'nav_profile', label: 'Thông tin cá nhân', icon: <UserOutlined /> },
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  danger: true,
                  label: 'Đăng xuất',
                },
              ],
              onClick: ({ key }) => {
                if (key === 'nav_dashboard') navigate('/dashboard');
                if (key === 'nav_payment_monitoring') navigate('/payment-monitoring/payments');
                if (key === 'nav_bookings') navigate('/front-desk/check-in');
                if (key === 'guest_bookings') navigate(ROUTE_PATHS.guestBookings);
                if (key === 'nav_housekeeping') navigate('/housekeeping');
                if (key === 'nav_profile') navigate(ROUTE_PATHS.profile);
                if (key === 'resume_payment') navigate(`/payments/${latestPaymentId}/result`);
                if (key === 'logout') {
                  logout();
                  navigate('/', { replace: true });
                }
              },
            }}
            classNames={{ root: 'main-header__account-menu' }}
          >
            <Button
              type="text"
              aria-label="Mở menu tài khoản"
              className="main-header__account-button"
            >
              <Avatar
                size="small"
                icon={<UserOutlined />}
                style={{ backgroundColor: token.colorPrimary }}
              />
              {screens.md && (
                <Typography.Text style={{ marginInlineStart: token.marginSM }}>
                  {session?.user?.fullName ?? 'Tài khoản'}
                </Typography.Text>
              )}
            </Button>
          </Dropdown>
        ) : (
          <Flex gap="small">
            {latestPaymentId && (
              <Button
                icon={<CreditCardOutlined />}
                onClick={() => navigate(`/payments/${latestPaymentId}/result`)}
              >
                {screens.md ? 'Tiếp tục thanh toán' : 'Thanh toán'}
              </Button>
            )}
            <Button type="primary" icon={<LoginOutlined />}>
              <Link to="/login">Đăng nhập</Link>
            </Button>
          </Flex>
        )}
      </Flex>
    </Layout.Header>
  );
}

function getActiveNavKey(pathname: string) {
  if (pathname === '/') return 'home';
  if (pathname.startsWith('/rooms/search') || pathname.startsWith('/bookings/new')) {
    return 'booking';
  }
  if (pathname.startsWith('/room-types')) return 'room-types';
  return undefined;
}
