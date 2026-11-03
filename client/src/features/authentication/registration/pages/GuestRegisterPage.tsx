import { AuthLayout } from '../../../../layouts/auth';
import { GuestRegisterForm } from '../components/GuestRegisterForm';

export function GuestRegisterPage() {
  return (
    <AuthLayout title="Đăng ký tài khoản">
      <GuestRegisterForm />
    </AuthLayout>
  );
}
