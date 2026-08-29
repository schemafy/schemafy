import type { SignUpFormValues } from './form';

export interface UseSignUpEmailVerificationProps {
  emailError: string;
  setFormError: (message: string) => void;
  clearFormError: () => void;
}

export interface UseSignUpSubmissionProps {
  form: SignUpFormValues;
  runAllValidations: (form: SignUpFormValues) => boolean;
  emailVerificationRequired: boolean;
  signupVerificationToken: string;
  onSuccess: () => void;
  setFormError: (message: string) => void;
}
