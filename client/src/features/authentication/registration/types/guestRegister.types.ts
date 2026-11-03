export interface GuestRegisterRequest {
  fullName: string;
  email: string;
  phone: string;
  password: string;
  confirmPassword: string;
}

export interface GuestRegisterResponse {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  role: string;
  status: string;
}
