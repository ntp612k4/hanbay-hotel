import { ArrowLeftOutlined, LockOutlined } from '@ant-design/icons';
import { Alert, App, Button, Flex, Form, Input, Result, Spin, Typography } from 'antd';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';

import { AuthLayout } from '../../../../layouts/auth';
import { useResetPassword, useVerifyResetToken } from '../hooks/password-recovery-hooks';
import { mapPasswordRecoveryError } from '../utils/password-recovery-error-mapper';
import './password-recovery.css';

interface ResetPasswordFormValues {
  newPassword: string;
  confirmPassword: string;
}

export function ResetPasswordPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [form] = Form.useForm<ResetPasswordFormValues>();
  const verifyQuery = useVerifyResetToken(token);
  const resetPasswordMutation = useResetPassword();

  const submitForm = (values: ResetPasswordFormValues) => {
    if (!token) return;

    resetPasswordMutation.mutate(
      { token, newPassword: values.newPassword, confirmPassword: values.confirmPassword },
      {
        onSuccess: () => {
          void message.success('Đặt lại mật khẩu thành công. Vui lòng đăng nhập.');
          navigate('/login', { replace: true, state: { passwordReset: true } });
        },
      },
    );
  };

  if (!token) {
    return (
      <AuthLayout title="Liên kết không hợp lệ">
        <Result
          status="error"
          title="Thiếu mã đặt lại mật khẩu"
          subTitle="Vui lòng mở lại liên kết đặt lại mật khẩu trong email."
          extra={<Link to="/forgot-password">Yêu cầu liên kết mới</Link>}
        />
      </AuthLayout>
    );
  }

  if (verifyQuery.isPending) {
    return (
      <AuthLayout title="Đang kiểm tra liên kết">
        <Flex className="password-recovery-state" align="center" justify="center" vertical gap={16}>
          <Spin size="large" />
          <Typography.Text type="secondary">Đang kiểm tra mã đặt lại mật khẩu...</Typography.Text>
        </Flex>
      </AuthLayout>
    );
  }

  if (verifyQuery.isError) {
    return (
      <AuthLayout title="Liên kết không còn hiệu lực">
        <Result
          status="error"
          title={mapPasswordRecoveryError(verifyQuery.error)}
          subTitle="Bạn có thể yêu cầu một liên kết đặt lại mật khẩu mới."
          extra={<Link to="/forgot-password">Yêu cầu liên kết mới</Link>}
        />
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Đặt lại mật khẩu" subtitle="Tạo mật khẩu mới cho tài khoản của bạn.">
      <Form<ResetPasswordFormValues>
        form={form}
        layout="vertical"
        requiredMark={false}
        validateTrigger="onBlur"
        className="password-recovery-form"
        onFinish={submitForm}
        onValuesChange={() => resetPasswordMutation.reset()}
      >
        <div className="password-recovery-feedback" aria-live="polite">
          {resetPasswordMutation.isError ? (
            <Alert
              type="error"
              showIcon
              title={mapPasswordRecoveryError(resetPasswordMutation.error)}
            />
          ) : null}
        </div>

        <Form.Item
          label="Mật khẩu mới"
          name="newPassword"
          rules={[
            { required: true, message: 'Vui lòng nhập mật khẩu mới.' },
            { min: 8, max: 72, message: 'Mật khẩu phải có từ 8 đến 72 ký tự.' },
          ]}
        >
          <Input.Password
            size="large"
            prefix={<LockOutlined />}
            placeholder="Nhập mật khẩu mới"
            autoComplete="new-password"
          />
        </Form.Item>

        <Form.Item
          label="Xác nhận mật khẩu"
          name="confirmPassword"
          dependencies={['newPassword']}
          rules={[
            { required: true, message: 'Vui lòng xác nhận mật khẩu mới.' },
            { min: 8, max: 72, message: 'Mật khẩu xác nhận phải có từ 8 đến 72 ký tự.' },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                return Promise.reject(new Error('Mật khẩu xác nhận không khớp.'));
              },
            }),
          ]}
        >
          <Input.Password
            size="large"
            prefix={<LockOutlined />}
            placeholder="Nhập lại mật khẩu mới"
            autoComplete="new-password"
          />
        </Form.Item>

        <Button
          type="primary"
          htmlType="submit"
          size="large"
          block
          className="password-recovery-submit"
          loading={resetPasswordMutation.isPending}
          disabled={resetPasswordMutation.isPending}
        >
          Đặt lại mật khẩu
        </Button>

        <Typography.Paragraph className="password-recovery-nav">
          <Link to="/login">
            <ArrowLeftOutlined /> Quay lại đăng nhập
          </Link>
        </Typography.Paragraph>
      </Form>
    </AuthLayout>
  );
}
