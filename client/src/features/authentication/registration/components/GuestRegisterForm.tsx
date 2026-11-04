import {
  ArrowRightOutlined,
  EyeInvisibleOutlined,
  EyeOutlined,
  LockOutlined,
  MailOutlined,
  PhoneOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { Alert, Button, Checkbox, Form, Input, Typography } from 'antd';
import type { Rule } from 'antd/es/form';
import { useState } from 'react';
import { Link } from 'react-router-dom';

import { useGuestRegister } from '../hooks/useGuestRegister';
import type { GuestRegisterRequest } from '../types/guestRegister.types';
import {
  getGuestRegisterFieldErrors,
  mapGuestRegisterError,
} from '../utils/guestRegisterErrorMapper';

type GuestRegisterFormValues = GuestRegisterRequest & {
  agreedTerms: boolean;
};

const VIETNAM_PHONE_REGEX = /^(0[35789][0-9]{8}|\+84[35789][0-9]{8})$/;

function normalizeVietnamPhone(phone: string) {
  const trimmedPhone = phone.trim();

  if (trimmedPhone.startsWith('+84')) {
    return `0${trimmedPhone.slice(3)}`;
  }

  return trimmedPhone;
}

function requiredRule(message: string): Rule {
  return { required: true, whitespace: true, message };
}

export function GuestRegisterForm() {
  const [form] = Form.useForm<GuestRegisterFormValues>();
  const guestRegister = useGuestRegister();
  const [successMessage, setSuccessMessage] = useState<string>();

  const clearServerFeedback = () => {
    if (guestRegister.error) {
      guestRegister.reset();
    }

    if (successMessage) {
      setSuccessMessage(undefined);
    }
  };

  const submitForm = (values: GuestRegisterFormValues) => {
    clearServerFeedback();

    const payload: GuestRegisterRequest = {
      fullName: values.fullName.trim(),
      email: values.email.trim().toLowerCase(),
      phone: normalizeVietnamPhone(values.phone),
      password: values.password,
      confirmPassword: values.confirmPassword,
    };

    guestRegister.mutate(payload, {
      onSuccess: () => {
        form.resetFields();
        setSuccessMessage('Tài khoản đã được đăng ký thành công.');
      },
      onError: (error) => {
        const fieldErrors = getGuestRegisterFieldErrors(error);

        form.setFields(
          Object.entries(fieldErrors).map(([name, message]) => ({
            name: name as keyof GuestRegisterRequest,
            errors: message ? [message] : [],
          })),
        );
      },
    });
  };

  const errorMessage = guestRegister.error ? mapGuestRegisterError(guestRegister.error) : undefined;
  const submitOnEnter = () => {
    if (!guestRegister.isPending) form.submit();
  };

  return (
    <Form<GuestRegisterFormValues>
      form={form}
      className="auth-register-form"
      layout="vertical"
      requiredMark={false}
      validateTrigger="onBlur"
      initialValues={{
        fullName: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
        agreedTerms: false,
      }}
      onFinish={submitForm}
      onValuesChange={clearServerFeedback}
    >
      {errorMessage && (
        <Alert
          className="auth-register-form__wide"
          type="error"
          showIcon
          message={errorMessage}
          style={{ marginBottom: 16 }}
        />
      )}

      {successMessage && (
        <Alert
          className="auth-register-form__wide"
          type="success"
          showIcon
          message={successMessage}
          style={{ marginBottom: 16 }}
        />
      )}

      <Form.Item
        className="auth-register-form__wide"
        label="Họ và tên"
        name="fullName"
        rules={[
          requiredRule('Vui lòng nhập họ và tên.'),
          { min: 2, max: 150, message: 'Họ tên phải có từ 2 đến 150 ký tự.' },
        ]}
      >
        <Input
          id="reg-fullname"
          size="large"
          prefix={<UserOutlined />}
          placeholder="Ví dụ: Nguyễn Văn A"
          autoComplete="name"
          onPressEnter={submitOnEnter}
        />
      </Form.Item>

      <Form.Item
        label="Email"
        name="email"
        rules={[
          requiredRule('Vui lòng nhập email.'),
          { type: 'email', message: 'Email không hợp lệ.' },
          { max: 255, message: 'Email không được vượt quá 255 ký tự.' },
        ]}
      >
        <Input
          id="reg-email"
          size="large"
          type="email"
          prefix={<MailOutlined />}
          placeholder="name@example.com"
          autoComplete="email"
          onPressEnter={submitOnEnter}
        />
      </Form.Item>

      <Form.Item
        label="Số điện thoại"
        name="phone"
        normalize={(value: string) => value?.trim()}
        rules={[
          requiredRule('Vui lòng nhập số điện thoại.'),
          {
            pattern: VIETNAM_PHONE_REGEX,
            message: 'Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam hợp lệ.',
          },
        ]}
      >
        <Input
          id="reg-phone"
          size="large"
          inputMode="tel"
          prefix={<PhoneOutlined />}
          placeholder="Ví dụ: 0901234567"
          autoComplete="tel"
          onPressEnter={submitOnEnter}
        />
      </Form.Item>

      <Form.Item
        label="Mật khẩu"
        name="password"
        extra="Mật khẩu phải có ít nhất 8 ký tự"
        rules={[
          requiredRule('Vui lòng nhập mật khẩu.'),
          { min: 8, max: 72, message: 'Mật khẩu phải có từ 8 đến 72 ký tự.' },
        ]}
      >
        <Input.Password
          id="reg-password"
          size="large"
          prefix={<LockOutlined />}
          placeholder="••••••••"
          autoComplete="new-password"
          onPressEnter={submitOnEnter}
          iconRender={(visible) => (visible ? <EyeInvisibleOutlined /> : <EyeOutlined />)}
        />
      </Form.Item>

      <Form.Item
        label="Xác nhận mật khẩu"
        name="confirmPassword"
        dependencies={['password']}
        rules={[
          requiredRule('Vui lòng xác nhận mật khẩu.'),
          { min: 8, max: 72, message: 'Xác nhận mật khẩu phải có từ 8 đến 72 ký tự.' },
          ({ getFieldValue }) => ({
            validator(_, value) {
              if (!value || getFieldValue('password') === value) {
                return Promise.resolve();
              }

              return Promise.reject(new Error('Mật khẩu và xác nhận mật khẩu không khớp.'));
            },
          }),
        ]}
      >
        <Input.Password
          id="reg-confirm-password"
          size="large"
          prefix={<LockOutlined />}
          placeholder="••••••••"
          autoComplete="new-password"
          onPressEnter={submitOnEnter}
          iconRender={(visible) => (visible ? <EyeInvisibleOutlined /> : <EyeOutlined />)}
        />
      </Form.Item>

      <Form.Item
        className="auth-register-form__wide"
        name="agreedTerms"
        valuePropName="checked"
        rules={[
          {
            validator: (_, value) =>
              value
                ? Promise.resolve()
                : Promise.reject(new Error('Vui lòng đồng ý với điều khoản dịch vụ.')),
          },
        ]}
      >
        <Checkbox>
          Tôi đã đọc và đồng ý với{' '}
          <a href="/terms" target="_blank" rel="noreferrer">
            Điều khoản Dịch vụ
          </a>{' '}
          và{' '}
          <a href="/privacy" target="_blank" rel="noreferrer">
            Chính sách Bảo mật.
          </a>
        </Checkbox>
      </Form.Item>

      <Button
        className="auth-register-form__wide"
        id="reg-submit-btn"
        htmlType="submit"
        type="primary"
        size="large"
        loading={guestRegister.isPending}
        disabled={guestRegister.isPending}
        block
      >
        Tạo tài khoản <ArrowRightOutlined />
      </Button>

      <div className="auth-register-form__wide auth-register-form__footer">
        <Typography.Text type="secondary">Đã có tài khoản?</Typography.Text>
        <Link to="/login" id="reg-signin-link">
          Đăng nhập
        </Link>
      </div>
    </Form>
  );
}
