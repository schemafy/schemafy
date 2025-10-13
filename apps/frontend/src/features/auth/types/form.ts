export type FormValues = Record<string, string | number | boolean>;

export type Validator<T extends FormValues> = (value: T[keyof T], form?: T) => string;

export type ValidationRules<T extends FormValues> = {
  [K in keyof T]?: Validator<T>;
};

export interface SignUpFormValues {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
  [key: string]: string;
}

export interface SignInFormValues {
  email: string;
  password: string;
  [key: string]: string;
}
