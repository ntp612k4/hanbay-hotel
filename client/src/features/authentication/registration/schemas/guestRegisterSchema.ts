import { z } from 'zod';

const VIETNAM_PHONE_REGEX = /^(0[35789][0-9]{8}|\+84[35789][0-9]{8})$/;

function normalizeVietnamPhone(phone: string) {
  if (phone.startsWith('+84')) {
    return `0${phone.slice(3)}`;
  }

  return phone;
}

export const guestRegisterSchema = z
  .object({
    fullName: z.string().trim().min(2, 'Tên phải có ít nhất 2 ký tự').max(150),
    email: z.string().trim().email('Email không hợp lệ').max(255),
    phone: z
      .string()
      .trim()
      .regex(
        VIETNAM_PHONE_REGEX,
        'Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam hợp lệ',
      )
      .transform(normalizeVietnamPhone),
    password: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số')
      .max(72),
    confirmPassword: z
      .string()
      .min(8, 'Xác nhận mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số')
      .max(72),
  })
  .refine((values) => values.password === values.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Mật khẩu và xác nhận mật khẩu không khớp',
  });

export type GuestRegisterFormValues = z.infer<typeof guestRegisterSchema>;
