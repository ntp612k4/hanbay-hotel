import { ArrowLeftOutlined, MailOutlined, SendOutlined } from '@ant-design/icons';
import { Alert, App, Button, Form, Input, Typography } from 'antd';
import { Link } from 'react-router-dom';

import { AuthLayout } from '../../../../layouts/auth';
import { useForgotPassword } from '../hooks/password-recovery-hooks';
import { mapPasswordRecoveryError } from '../utils/password-recovery-error-mapper';
import './password-recovery.css';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

interface ForgotPasswordFormValues {
  email: string;
}

const GENERIC_SUCCESS_MESSAGE = 'Nếu email tồn tại, hướng dẫn đặt lại mật khẩu sẽ được gửi.';

export function ForgotPasswordPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm<ForgotPasswordFormValues>();
  const forgotPasswordMutation = useForgotPassword();

  const submitForm = (values: ForgotPasswordFormValues) => {
    const normalizedEmail = values.email.trim().toLowerCase();
    if (!EMAIL_PATTERN.test(normalizedEmail)) {
      form.setFields([{ name: 'email', errors: ['Email không hợp lệ.'] }]);
      return;
    }

    forgotPasswordMutation.mutate(
      { email: normalizedEmail },
      {
        onSuccess: () => {
          void message.success(GENERIC_SUCCESS_MESSAGE);
        },
      },
    );
  };

  const submitOnEnter = () => {
    if (!forgotPasswordMutation.isPending) form.submit();
  };

  return (
    <AuthLayout
      title="Quên mật khẩu"
      subtitle="Nhập email tài khoản để nhận hướng dẫn đặt lại mật khẩu."
    >
      <Form<ForgotPasswordFormValues>
        form={form}
        layout="vertical"
        requiredMark={false}
        validateTrigger="onBlur"
        className="password-recovery-form"
        onFinish={submitForm}
        onValuesChange={() => forgotPasswordMutation.reset()}
      >
        <div className="password-recovery-feedback" aria-live="polite">
          {forgotPasswordMutation.isSuccess ? (
            <Alert type="success" showIcon title={GENERIC_SUCCESS_MESSAGE} />
          ) : null}
          {forgotPasswordMutation.isError ? (
            <Alert
              type="error"
              showIcon
              title={mapPasswordRecoveryError(
                forgotPasswordMutation.error,
                'Không thể gửi yêu cầu lúc này. Vui lòng thử lại.',
              )}
            />
          ) : null}
        </div>

        <Form.Item
          label="Email"
          name="email"
          normalize={(value: string) => value?.trim()}
          rules={[
            { required: true, message: 'Vui lòng nhập email.' },
            {
              pattern: EMAIL_PATTERN,
              message: 'Email không hợp lệ.',
            },
            { max: 255, message: 'Email không được vượt quá 255 ký tự.' },
          ]}
        >
          <Input
            inputMode="email"
            size="large"
            prefix={<MailOutlined />}
            placeholder="name@example.com"
            autoComplete="email"
            onPressEnter={submitOnEnter}
          />
        </Form.Item>

        <Button
          type="primary"
          htmlType="submit"
          size="large"
          block
          className="password-recovery-submit"
          loading={forgotPasswordMutation.isPending}
          disabled={forgotPasswordMutation.isPending}
          icon={<SendOutlined />}
        >
          Gửi hướng dẫn
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
