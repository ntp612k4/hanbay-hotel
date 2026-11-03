import { useLoginCooldown } from '../hooks/use-login-cooldown';
import { useEffect, useMemo, useRef } from 'react';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { ArrowRightOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';
import {
  Alert,
  App,
  Button,
  Checkbox,
  Col,
  Divider,
  Flex,
  Form,
  Input,
  Row,
  Space,
  Typography,
} from 'antd';

import { ApiClientError } from '../../../types/api';
import { useAuth } from '../providers/auth-context';
import { login } from '../services/auth-service';
import { getPostLoginRedirect, type LoginRedirectLocation } from '../utils/post-login-redirect';
import './login-page.css';

type LoginFormValues = {
  identifier: string;
  password: string;
  remember: boolean;
};

type FromState = {
  from?: LoginRedirectLocation;
};

const { Link, Paragraph, Text, Title } = Typography;

export function LoginPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const location = useLocation();
  const [form] = Form.useForm<LoginFormValues>();
  const { isAuthenticated, isInitializing, roles, startSession } = useAuth();
  const loginSubmitted = useRef(false);
  const { seconds: retryAfterSeconds, start: startCooldown } = useLoginCooldown();

  const requestedLocation = useMemo(() => {
    const state = location.state as FromState | null;
    return state?.from;
  }, [location.state]);

  useEffect(() => {
    if (!isInitializing && isAuthenticated && !loginSubmitted.current) {
      navigate(getPostLoginRedirect(roles, requestedLocation), { replace: true });
    }
  }, [isAuthenticated, isInitializing, navigate, requestedLocation, roles]);

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (session) => {
      startSession(
        session.accessToken,
        session.refreshToken,
        session.userRoles,
        session.expiresIn,
        form.getFieldValue('remember'),
        session.user,
      );
      message.success(`Chào mừng ${session.user.fullName} quay trở lại!`);
      navigate(getPostLoginRedirect(session.userRoles, requestedLocation), { replace: true });
    },
    onError: (error) => {
      loginSubmitted.current = false;
      if (error instanceof ApiClientError && error.status === 429) {
        startCooldown(error.retryAfterSeconds ?? 60);
      }
    },
  });

  const onFinish = (values: LoginFormValues) => {
    if (isInitializing || loginMutation.isPending || retryAfterSeconds > 0) return;
    loginSubmitted.current = true;
    loginMutation.mutate({
      identifier: values.identifier.trim(),
      password: values.password,
    });
  };
  const submitOnEnter = () => {
    if (!isInitializing && !loginMutation.isPending && retryAfterSeconds === 0) form.submit();
  };

  const errorMessage =
    loginMutation.error instanceof ApiClientError
      ? loginMutation.error.status === 401
        ? 'Email, số điện thoại hoặc mật khẩu không đúng.'
        : loginMutation.error.status === 429
          ? retryAfterSeconds > 0
            ? `Bạn đã thử đăng nhập quá nhiều lần. Vui lòng thử lại sau ${retryAfterSeconds} giây.`
            : 'Bạn có thể thử đăng nhập lại.'
          : 'Không thể đăng nhập lúc này. Vui lòng thử lại sau.'
      : 'Không thể đăng nhập. Vui lòng thử lại.';

  return (
    <main className="login-page">
      <Row className="login-shell" wrap={false}>
        <Col
          xs={0}
          md={13}
          xl={15}
          className="login-visual"
          aria-label="Không gian nội thất khách sạn HANBAY"
        />

        <Col xs={24} md={11} xl={9} className="login-panel">
          <Flex vertical className="login-card">
            <Space orientation="vertical" align="center" size={4} className="login-intro">
              <div className="login-logo-frame">
                <img className="login-logo" src="/logo.png" alt="Hệ thống quản lý HANBAY" />
              </div>
              <Title level={1} className="login-heading">
                Đăng nhập hệ thống quản lý HANBAY
              </Title>
              <Paragraph type="secondary" className="login-subheading">
                Vui lòng nhập thông tin tài khoản để tiếp tục.
              </Paragraph>
            </Space>

            <Form<LoginFormValues>
              form={form}
              layout="vertical"
              requiredMark={false}
              className="login-form"
              initialValues={{ remember: true }}
              onFinish={onFinish}
              onValuesChange={() => {
                if (retryAfterSeconds === 0) loginMutation.reset();
              }}
              validateTrigger="onBlur"
              autoComplete="off"
            >
              <div className="login-error-slot" aria-live="polite">
                {loginMutation.isError ? (
                  <Alert type="error" showIcon title={errorMessage} />
                ) : null}
              </div>

              <Form.Item
                className="login-field"
                label="Email hoặc số điện thoại"
                name="identifier"
                rules={[
                  { required: true, message: 'Vui lòng nhập email hoặc số điện thoại.' },
                  { min: 4, message: 'Email hoặc số điện thoại phải có ít nhất 4 ký tự.' },
                  { max: 255, message: 'Email hoặc số điện thoại không được vượt quá 255 ký tự.' },
                ]}
              >
                <Input
                  prefix={
                    <span className="login-input-icon">
                      <MailOutlined />
                    </span>
                  }
                  placeholder="Nhập email hoặc số điện thoại"
                  size="large"
                  onPressEnter={submitOnEnter}
                />
              </Form.Item>

              <Form.Item
                className="login-field"
                label="Mật khẩu"
                name="password"
                rules={[
                  { required: true, message: 'Vui lòng nhập mật khẩu.' },
                  { min: 8, message: 'Mật khẩu phải có ít nhất 8 ký tự.' },
                  { max: 72, message: 'Mật khẩu không được vượt quá 72 ký tự.' },
                ]}
              >
                <Input.Password
                  prefix={
                    <span className="login-input-icon">
                      <LockOutlined />
                    </span>
                  }
                  placeholder="Nhập mật khẩu"
                  size="large"
                  onPressEnter={submitOnEnter}
                />
              </Form.Item>

              <Flex justify="space-between" align="center" className="login-options">
                <Form.Item name="remember" valuePropName="checked" noStyle>
                  <Checkbox>Ghi nhớ đăng nhập</Checkbox>
                </Form.Item>
                <RouterLink className="login-link" to="/forgot-password">
                  Quên mật khẩu?
                </RouterLink>
              </Flex>

              <Form.Item>
                <Button
                  type="primary"
                  htmlType="submit"
                  block
                  size="large"
                  className="login-submit"
                  loading={loginMutation.isPending || isInitializing}
                  disabled={isInitializing || retryAfterSeconds > 0}
                >
                  <span>ĐĂNG NHẬP</span>
                  <ArrowRightOutlined className="login-arrow" />
                </Button>
              </Form.Item>
            </Form>

            <Paragraph className="login-signup">
              Chưa có tài khoản? <RouterLink to="/register">Đăng ký</RouterLink>
            </Paragraph>

            <footer className="login-footer">
              <Divider />
              <Space size={[20, 8]} wrap>
                <Link href="#privacy">
                  <Text>Chính sách bảo mật</Text>
                </Link>
                <Link href="#terms">
                  <Text>Điều khoản sử dụng</Text>
                </Link>
                <Link href="#support">
                  <Text>Liên hệ hỗ trợ</Text>
                </Link>
              </Space>
            </footer>
          </Flex>
        </Col>
      </Row>
    </main>
  );
}
