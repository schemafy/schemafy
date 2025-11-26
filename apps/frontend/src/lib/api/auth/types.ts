export type SignUpRequest = {
  email: string;
  name: string;
  password: string;
};

export type SignInRequest = {
  email: string;
  password: string;
};

export type AuthResponse = {
  id: string;
  email: string;
  name: string;
};
