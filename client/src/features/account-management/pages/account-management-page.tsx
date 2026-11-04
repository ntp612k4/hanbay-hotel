import { PlusOutlined, ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  App,
  Button,
  Card,
  Flex,
  Form,
  Grid,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography,
} from 'antd';
import type { TableProps } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuth } from '../../authentication/providers/auth-context';
import { ApiClientError } from '../../../types/api';
import type { UserRole } from '../../../types/common';
import {
  createAccount,
  getManagedAccounts,
  updateAccountRoles,
  updateAccountStatus,
  type AccountStatus,
  type CreateAccountPayload,
  type ManagedAccount,
} from '../api/account-management-api';
import './account-management-page.css';

const ROLE_OPTIONS: Array<{ value: UserRole; label: string }> = [
  { value: 'ADMIN', label: 'Quản trị viên' },
  { value: 'MANAGER', label: 'Quản lý khách sạn' },
  { value: 'RECEPTIONIST', label: 'Lễ tân' },
  { value: 'HOUSEKEEPER', label: 'Buồng phòng' },
];
const ROLE_LABELS = Object.fromEntries(ROLE_OPTIONS.map((role) => [role.value, role.label]));
const STATUS_OPTIONS: Array<{ value: AccountStatus; label: string }> = [
  { value: 'ACTIVE', label: 'Đang hoạt động' },
  { value: 'LOCKED', label: 'Đã khóa' },
  { value: 'INACTIVE', label: 'Ngừng hoạt động' },
];
function errorMessage(error: unknown) {
  if (error instanceof ApiClientError) {
    const details = Object.values(error.data?.errors ?? {}).flat();
    return details.length ? details.join('. ') : error.message;
  }
  return 'Không thể thực hiện thao tác. Vui lòng thử lại.';
}

export function AccountManagementPage() {
  const { message, modal } = App.useApp();
  const { session, logout } = useAuth();
  const screens = Grid.useBreakpoint();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [role, setRole] = useState<UserRole | 'GUEST'>();
  const [status, setStatus] = useState<AccountStatus>();
  const [creating, setCreating] = useState(false);
  const [editing, setEditing] = useState<ManagedAccount | null>(null);
  const [selectedRoles, setSelectedRoles] = useState<UserRole[]>([]);
  const [form] = Form.useForm<CreateAccountPayload & { confirmPassword: string }>();
  const accounts = useQuery({
    queryKey: ['account-management', { page, search, role, status }],
    queryFn: () => getManagedAccounts({ page, size: 10, search, role, status }),
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['account-management'] });
  const created = useMutation({ mutationFn: createAccount, onSuccess: refresh });
  const changedRoles = useMutation({
    mutationFn: ({ id, roles }: { id: string; roles: UserRole[] }) => updateAccountRoles(id, roles),
    onSuccess: refresh,
  });
  const changedStatus = useMutation({
    mutationFn: ({ id, status }: { id: string; status: AccountStatus }) =>
      updateAccountStatus(id, status),
    onSuccess: refresh,
  });

  const submitCreate = async () => {
    try {
      const values = await form.validateFields();
      await created.mutateAsync({
        fullName: values.fullName.trim(),
        email: values.email.trim(),
        phone: values.phone.trim(),
        password: values.password,
        roles: values.roles,
      });
      setCreating(false);
      form.resetFields();
      setPage(0);
      void message.success(
        'Đã tạo tài khoản. Nhân viên có thể đăng nhập bằng thông tin vừa thiết lập.',
      );
    } catch (error) {
      if (error && typeof error === 'object' && 'errorFields' in error) return;
      void message.error(errorMessage(error));
    }
  };
  const submitRoles = async () => {
    if (!editing) return;
    try {
      await changedRoles.mutateAsync({ id: editing.id, roles: selectedRoles });
      setEditing(null);
      if (editing.id === session?.user?.id) {
        logout();
        navigate('/login', { replace: true });
        void message.success('Đã cập nhật quyền. Vui lòng đăng nhập lại.');
      } else {
        void message.success('Đã cập nhật vai trò. Tài khoản này cần đăng nhập lại.');
      }
    } catch (error) {
      void message.error(errorMessage(error));
    }
  };
  const changeStatus = (account: ManagedAccount, next: AccountStatus) => {
    if (account.status === next) return;
    modal.confirm({
      title: 'Cập nhật trạng thái tài khoản?',
      content: `${account.fullName} sẽ chuyển sang “${STATUS_OPTIONS.find((item) => item.value === next)?.label}”. Phiên đăng nhập hiện tại sẽ kết thúc.`,
      okText: 'Xác nhận',
      cancelText: 'Hủy',
      okButtonProps: { danger: next !== 'ACTIVE' },
      onOk: async () => {
        try {
          await changedStatus.mutateAsync({ id: account.id, status: next });
          void message.success('Đã cập nhật trạng thái tài khoản.');
        } catch (error) {
          void message.error(errorMessage(error));
          throw error;
        }
      },
    });
  };
  const columns: TableProps<ManagedAccount>['columns'] = [
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      width: 210,
      render: (name, account) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{name}</Typography.Text>
          {account.id === session?.user?.id && <Tag color="blue">Tài khoản của bạn</Tag>}
        </Space>
      ),
    },
    { title: 'Email', dataIndex: 'email', width: 260 },
    { title: 'Điện thoại', dataIndex: 'phone', width: 140, render: (phone) => phone || '—' },
    {
      title: 'Vai trò',
      dataIndex: 'roles',
      width: 230,
      render: (roles: UserRole[]) => (
        <Space size={[0, 4]} wrap>
          {roles.length ? (
            roles.map((item) => (
              <Tag key={item} color={item === 'ADMIN' ? 'purple' : 'blue'}>
                {ROLE_LABELS[item]}
              </Tag>
            ))
          ) : (
            <Tag>Khách hàng</Tag>
          )}
        </Space>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      width: 165,
      render: (value: AccountStatus) => (
        <Tag color={value === 'ACTIVE' ? 'green' : value === 'LOCKED' ? 'red' : 'default'}>
          {STATUS_OPTIONS.find((item) => item.value === value)?.label}
        </Tag>
      ),
    },
    {
      title: 'Thao tác',
      key: 'actions',
      width: 350,
      fixed: screens.md ? 'right' : undefined,
      render: (_, account) => (
        <Space>
          <Button
            icon={<SafetyCertificateOutlined />}
            onClick={() => {
              setEditing(account);
              setSelectedRoles(account.roles);
            }}
          >
            Phân quyền
          </Button>
          <Select
            aria-label={`Trạng thái ${account.email}`}
            style={{ width: 160 }}
            value={account.status}
            disabled={account.id === session?.user?.id || changedStatus.isPending}
            options={STATUS_OPTIONS}
            onChange={(next) => changeStatus(account, next)}
          />
        </Space>
      ),
    },
  ];
  return (
    <Space direction="vertical" size="large" className="account-management-page">
      <Flex justify="space-between" align="center" gap="middle" wrap>
        <div>
          <Typography.Title level={2}>Quản lý tài khoản và phân quyền</Typography.Title>
          <Typography.Text type="secondary">
            Tạo tài khoản nhân viên, cấp vai trò và quản lý trạng thái truy cập.
          </Typography.Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            form.resetFields();
            setCreating(true);
          }}
        >
          Thêm tài khoản
        </Button>
      </Flex>
      <Card>
        <Flex gap="middle" wrap className="account-management-toolbar">
          <Input.Search
            aria-label="Tìm tài khoản"
            placeholder="Họ tên, email hoặc số điện thoại"
            value={searchInput}
            maxLength={150}
            allowClear
            onChange={(event) => setSearchInput(event.target.value)}
            onSearch={(value) => {
              setSearch(value.trim());
              setPage(0);
            }}
          />
          <Select
            aria-label="Lọc vai trò"
            placeholder="Tất cả vai trò"
            allowClear
            value={role}
            options={[{ value: 'GUEST', label: 'Khách hàng' }, ...ROLE_OPTIONS]}
            onChange={(value) => {
              setRole(value);
              setPage(0);
            }}
          />
          <Select
            aria-label="Lọc trạng thái"
            placeholder="Tất cả trạng thái"
            allowClear
            value={status}
            options={STATUS_OPTIONS}
            onChange={(value) => {
              setStatus(value);
              setPage(0);
            }}
          />
          <Button icon={<ReloadOutlined />} onClick={() => void accounts.refetch()}>
            Làm mới
          </Button>
        </Flex>
        {accounts.isError && (
          <Alert
            type="error"
            showIcon
            title="Không tải được danh sách tài khoản"
            description={errorMessage(accounts.error)}
          />
        )}
        <Table<ManagedAccount>
          rowKey="id"
          columns={columns}
          dataSource={accounts.data?.content ?? []}
          loading={accounts.isLoading}
          scroll={{ x: 1400 }}
          locale={{ emptyText: 'Không tìm thấy tài khoản phù hợp' }}
          pagination={{
            current: page + 1,
            pageSize: 10,
            total: accounts.data?.totalElements ?? 0,
            showSizeChanger: false,
            onChange: (next) => setPage(next - 1),
            showTotal: (total) => `${total} tài khoản`,
          }}
        />
      </Card>
      <Modal
        title={`Phân quyền: ${editing?.fullName ?? ''}`}
        open={editing !== null}
        onCancel={() => setEditing(null)}
        okText="Lưu vai trò"
        cancelText="Hủy"
        confirmLoading={changedRoles.isPending}
        onOk={() => void submitRoles()}
      >
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Typography.Text>{editing?.email}</Typography.Text>
          <Select
            aria-label="Vai trò tài khoản"
            mode="multiple"
            style={{ width: '100%' }}
            value={selectedRoles}
            onChange={setSelectedRoles}
            placeholder="Không chọn vai trò: khách hàng"
            options={ROLE_OPTIONS.map((item) => ({
              ...item,
              disabled: item.value === 'ADMIN' && editing?.id === session?.user?.id,
            }))}
          />
          <Alert
            type="info"
            showIcon
            title="Mỗi tài khoản có thể có nhiều vai trò"
            description="Để trống vai trò để chuyển thành khách hàng. Sau khi quyền thay đổi, người dùng cần đăng nhập lại."
          />
        </Space>
      </Modal>
      <Modal
        title="Thêm tài khoản"
        open={creating}
        onCancel={() => setCreating(false)}
        okText="Tạo tài khoản"
        cancelText="Hủy"
        confirmLoading={created.isPending}
        onOk={() => void submitCreate()}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{ roles: ['RECEPTIONIST'] }}
          preserve={false}
        >
          <Form.Item
            name="fullName"
            label="Họ và tên"
            rules={[{ required: true, whitespace: true, min: 2, max: 150 }]}
          >
            <Input maxLength={150} />
          </Form.Item>
          <Form.Item
            name="email"
            label="Email"
            rules={[{ required: true, type: 'email', max: 255 }]}
          >
            <Input autoComplete="off" />
          </Form.Item>
          <Form.Item
            name="phone"
            label="Số điện thoại"
            rules={[
              { required: true, pattern: /^[0-9]{9,15}$/, message: 'Nhập từ 9 đến 15 chữ số' },
            ]}
          >
            <Input maxLength={15} />
          </Form.Item>
          <Form.Item
            name="password"
            label="Mật khẩu"
            rules={[{ required: true, min: 8, max: 72, message: 'Mật khẩu từ 8 đến 72 ký tự' }]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="Xác nhận mật khẩu"
            dependencies={['password']}
            rules={[
              { required: true },
              ({ getFieldValue }) => ({
                validator: (_, value) =>
                  value === getFieldValue('password')
                    ? Promise.resolve()
                    : Promise.reject(new Error('Mật khẩu xác nhận không khớp')),
              }),
            ]}
          >
            <Input.Password autoComplete="new-password" />
          </Form.Item>
          <Form.Item name="roles" label="Vai trò">
            <Select
              mode="multiple"
              options={ROLE_OPTIONS}
              placeholder="Không chọn vai trò: khách hàng"
            />
          </Form.Item>
        </Form>
      </Modal>
    </Space>
  );
}
