import { MailOutlined, PhoneOutlined, UserOutlined } from '@ant-design/icons';
import { Alert, App, Button, Card, Col, Flex, Form, Input, Row } from 'antd';
import { useEffect, useMemo } from 'react';

import { useAuth } from '../../authentication/providers/auth-context';
import { useUpdateCurrentProfile } from '../hooks/use-profile';
import type { UpdateUserRequest, User } from '../types/profile';
import { getProfileErrorMessage, getProfileFieldErrors } from '../utils/profile-error-mapper';

interface ProfileFormValues {
  fullName: string;
  phone: string;
}

export function PersonalInformationForm({ profile }: { profile: User }) {
  const [form] = Form.useForm<ProfileFormValues>();
  const updateProfile = useUpdateCurrentProfile();
  const { message } = App.useApp();
  const { updateUser } = useAuth();
  const initialValues = useMemo(
    () => ({ fullName: profile.fullName, phone: profile.phone ?? '' }),
    [profile.fullName, profile.phone],
  );

  useEffect(() => form.setFieldsValue(initialValues), [form, initialValues]);

  const submitForm = (values: ProfileFormValues) => {
    const normalized = { fullName: values.fullName.trim(), phone: values.phone.trim() };
    const payload: UpdateUserRequest = {};
    if (normalized.fullName !== profile.fullName) payload.fullName = normalized.fullName;
    if (normalized.phone !== (profile.phone ?? '')) payload.phone = normalized.phone;
    if (!Object.keys(payload).length) return void message.info('Thông tin chưa có thay đổi.');

    updateProfile.mutate(payload, {
      onSuccess: (updated) => {
        form.setFieldsValue({ fullName: updated.fullName, phone: updated.phone ?? '' });
        updateUser({
          id: updated.id,
          email: updated.email,
          fullName: updated.fullName,
          phone: updated.phone,
        });
        void message.success('Cập nhật hồ sơ thành công.');
      },
      onError: (error) => {
        const errors = getProfileFieldErrors(error);
        form.setFields(
          Object.entries(errors).map(([name, value]) => ({
            name: name as keyof ProfileFormValues,
            errors: value ? [value] : [],
          })),
        );
      },
    });
  };

  return (
    <Card className="profile-form-card" title="Thông tin cá nhân">
      {updateProfile.error && (
        <Alert
          type="error"
          showIcon
          closable
          message={getProfileErrorMessage(updateProfile.error)}
          onClose={updateProfile.reset}
        />
      )}
      <Form
        form={form}
        layout="vertical"
        requiredMark={false}
        initialValues={initialValues}
        onFinish={submitForm}
      >
        <Row gutter={16}>
          <Col xs={24} md={12}>
            <Form.Item
              label="Họ và tên"
              name="fullName"
              rules={[
                {
                  validator: (_, value: string) => {
                    const normalized = value?.trim() ?? '';
                    return normalized.length >= 2 && normalized.length <= 100
                      ? Promise.resolve()
                      : Promise.reject(new Error('Họ và tên phải có từ 2 đến 100 ký tự.'));
                  },
                },
              ]}
            >
              <Input prefix={<UserOutlined />} autoComplete="name" />
            </Form.Item>
          </Col>
          <Col xs={24} md={12}>
            <Form.Item
              label="Số điện thoại"
              name="phone"
              rules={[
                {
                  validator: (_, value: string) =>
                    /^\+?[0-9]{8,15}$/.test(value?.trim() ?? '')
                      ? Promise.resolve()
                      : Promise.reject(
                          new Error(
                            'Số điện thoại phải có từ 8 đến 15 chữ số và có thể bắt đầu bằng +.',
                          ),
                        ),
                },
              ]}
            >
              <Input prefix={<PhoneOutlined />} inputMode="tel" autoComplete="tel" />
            </Form.Item>
          </Col>
        </Row>
        <Form.Item label="Email">
          <Input prefix={<MailOutlined />} value={profile.email} disabled />
        </Form.Item>
        <Flex justify="flex-end" gap={8} className="profile-form-card__actions">
          <Button
            disabled={updateProfile.isPending}
            onClick={() => form.setFieldsValue(initialValues)}
          >
            Hủy
          </Button>
          <Button type="primary" htmlType="submit" loading={updateProfile.isPending}>
            Lưu thay đổi
          </Button>
        </Flex>
      </Form>
    </Card>
  );
}
