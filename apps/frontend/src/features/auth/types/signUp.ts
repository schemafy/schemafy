import type { SignUpFormValues, ValidationRules } from './form';

export interface UseSignUpEmailVerificationProps {
  emailError: string;
  setFormError: (message: string) => void;
  clearFormError: () => void;
}

export interface UseSignUpSubmissionProps {
  form: SignUpFormValues;
  errors: Partial<Record<keyof SignUpFormValues, string>>;
  validationRules: ValidationRules<SignUpFormValues>;
  emailVerificationRequired: boolean;
  signupVerificationToken: string;
  onSuccess: () => void;
  setFormError: (message: string) => void;
}
