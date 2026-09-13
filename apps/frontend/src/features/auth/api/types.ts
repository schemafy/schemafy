export type SignUpRequest = {
  email: string;
  name: string;
  password: string;
  signupVerificationToken?: string;
};

export type SignInRequest = {
  email: string;
  password: string;
};

export type SendSignUpEmailCodeRequest = {
  email: string;
};

export type SignUpEmailVerificationResponse = {
  email: string;
  expiresAt: string;
};

export type VerifySignUpEmailRequest = {
  email: string;
  code: string;
};

export type VerifySignUpEmailResponse = {
  email: string;
  signupVerificationToken: string;
  expiresAt: string;
};

export type AuthResponse = {
  id: string;
  email: string;
  name: string;
};
