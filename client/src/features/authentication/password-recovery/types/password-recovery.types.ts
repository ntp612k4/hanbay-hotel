export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordResponse {
  status: 'RESET_INSTRUCTIONS_ACCEPTED';
}

export interface ResetTokenVerificationResponse {
  status: 'RESET_TOKEN_VALID';
  expiresAt: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword: string;
}

export interface ResetPasswordResponse {
  status: 'PASSWORD_RESET';
}
