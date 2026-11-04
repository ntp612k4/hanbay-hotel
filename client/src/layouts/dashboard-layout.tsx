import {
  BankOutlined,
  AppstoreAddOutlined,
  DashboardOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  NotificationOutlined,
  PercentageOutlined,
  PlusCircleOutlined,
  ScheduleOutlined,
  SolutionOutlined,
  ToolOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { HomeOutlined, LoginOutlined } from '@ant-design/icons';
import {
  Avatar,
  Button,
  Dropdown,
  Drawer,
  Grid,
  Layout,
  Menu,
  Space,
  Typography,
  type MenuProps,
} from 'antd';
import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '../features/authentication/providers/auth-context';
import { ROUTE_PATHS } from '../routes/routePaths';
import { USER_ROLES } from '../types/common';
import './dashboard-layout.css';

const { Content, Header, Sider } = Layout;
const { Text } = Typography;

export function DashboardLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);
  const screens = Grid.useBreakpoint();
  const mobile = screens.md === false;
  const { logout, roles, session } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const canMonitorPayments = roles.some(
    (role) => role === USER_ROLES.admin || role === USER_ROLES.manager,
  );
  const canViewManagerDashboard = canMonitorPayments;
  const canViewTodayArrivals = roles.some(
    (role) =>
      role === USER_ROLES.receptionist || role === USER_ROLES.manager || role === USER_ROLES.admin,
  );
  const canViewTodayDepartures = roles.some(
    (role) =>
      role === USER_ROLES.admin || role === USER_ROLES.manager || role === USER_ROLES.receptionist,
  );

  const menuItems: MenuProps['items'] = [
    ...(roles.includes(USER_ROLES.admin)
      ? [{ key: ROUTE_PATHS.accounts, icon: <UserOutlined />, label: 'Tài khoản và phân quyền' }]
      : []),
    ...(canViewManagerDashboard
      ? [{ key: '/dashboard', icon: <DashboardOutlined />, label: 'Tổng quan' }]
      : []),
    { key: '/rooms/search', icon: <PlusCircleOutlined />, label: 'Tạo đặt phòng' },
    ...(canViewTodayArrivals
      ? [
          {
            key: '/front-desk/arrivals/today',
            icon: <LoginOutlined />,
            label: 'Khách đến hôm nay',
          },
        ]
      : []),
    ...(roles.includes(USER_ROLES.receptionist)
      ? [
          {
            key: '/front-desk/check-in',
            icon: <SolutionOutlined />,
            label: 'Nhận phòng',
          },
        ]
      : []),
    ...(canViewTodayDepartures
      ? [
          {
            key: '/front-desk/departures/today',
            icon: <ScheduleOutlined />,
            label: 'Khách trả phòng',
          },
        ]
      : []),
    ...(roles.includes(USER_ROLES.housekeeper)
      ? [{ key: '/housekeeping', icon: <HomeOutlined />, label: 'Phòng cần dọn' }]
      : []),
    ...(canMonitorPayments
      ? [
          ...(roles.includes(USER_ROLES.manager)
            ? [{ key: ROUTE_PATHS.maintenance, icon: <ToolOutlined />, label: 'Quản lý bảo trì' }]
            : []),
          {
            key: '/management/rooms',
            icon: <HomeOutlined />,
            label: 'Quản lý phòng',
          },
          {
            key: '/management/room-types',
            icon: <HomeOutlined />,
            label: 'Quản lý loại phòng',
          },
          {
            key: '/add-on-services',
            icon: <AppstoreAddOutlined />,
            label: 'Dịch vụ bổ sung',
          },
          {
            key: ROUTE_PATHS.priceRules,
            icon: <PercentageOutlined />,
            label: 'Quy tắc giá',
          },
          {
            key: 'payment-monitoring',
            icon: <BankOutlined />,
            label: 'Giám sát thanh toán',
            children: [
              { key: '/payment-monitoring/payments', label: 'Thanh toán' },
              { key: '/payment-monitoring/webhook-events', label: 'Sự kiện webhook' },
              ...(roles.includes(USER_ROLES.manager)
                ? [{ key: '/payment-monitoring/refunds', label: 'Yêu cầu hoàn tiền' }]
                : []),
            ],
          },
        ]
      : []),
  ];

  const userMenu: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Thông tin cá nhân',
      onClick: () => navigate(ROUTE_PATHS.profile),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: 'Đăng xuất',
      onClick: () => {
        logout();
        navigate('/login');
      },
    },
  ];

  return (
    <Layout className="dashboard-shell">
      {!mobile && (
        <Sider
          width={248}
          breakpoint="lg"
          collapsedWidth={72}
          collapsible
          collapsed={collapsed}
          trigger={null}
          onBreakpoint={setCollapsed}
          theme="dark"
          className="dashboard-shell__sider"
        >
          <div className="dashboard-shell__brand">
            <BankOutlined />
            {!collapsed && <span>HANBAY</span>}
          </div>
          <Menu
            theme="dark"
            mode="inline"
            items={menuItems}
            selectedKeys={[location.pathname]}
            defaultOpenKeys={canMonitorPayments ? ['payment-monitoring'] : []}
            onClick={({ key }) => {
              if (key.startsWith('/')) navigate(key);
            }}
          />
        </Sider>
      )}

      <Drawer
        title={
          <span className="dashboard-shell__drawer-brand">
            <BankOutlined /> HANBAY
          </span>
        }
        placement="left"
        open={mobile && mobileOpen}
        onClose={() => setMobileOpen(false)}
        size={280}
        className="dashboard-shell__mobile-menu"
        styles={{ body: { padding: 0 }, header: { borderBottomColor: '#1e293b' } }}
      >
        <Menu
          theme="dark"
          mode="inline"
          items={menuItems}
          selectedKeys={[location.pathname]}
          defaultOpenKeys={canMonitorPayments ? ['payment-monitoring'] : []}
          onClick={({ key }) => {
            if (key.startsWith('/')) navigate(key);
            setMobileOpen(false);
          }}
        />
      </Drawer>

      <Layout className="dashboard-shell__main">
        <Header className="dashboard-shell__header">
          <Button
            type="text"
            aria-label={mobile || collapsed ? 'Mở menu' : 'Thu gọn menu'}
            aria-expanded={mobile ? mobileOpen : !collapsed}
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() =>
              mobile ? setMobileOpen((value) => !value) : setCollapsed((value) => !value)
            }
          />
          <Space size="middle">
            <Button type="text" aria-label="Thông báo" icon={<NotificationOutlined />} />
            <Dropdown menu={{ items: userMenu }} trigger={['click']}>
              <Button type="text" className="dashboard-shell__account">
                <Avatar size="small" icon={<UserOutlined />} />
                <span className="dashboard-shell__user">
                  <Text strong>{session?.user?.fullName ?? 'Tài khoản'}</Text>
                  <Text type="secondary">
                    {roles.includes(USER_ROLES.admin) ? 'System Admin' : roles[0]}
                  </Text>
                </span>
              </Button>
            </Dropdown>
          </Space>
        </Header>

        <Content className="dashboard-shell__content">
          <div className="dashboard-shell__page">
            <Outlet />
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
