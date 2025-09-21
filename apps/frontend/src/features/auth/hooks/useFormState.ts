import { useState, useCallback, useRef } from 'react';
import type { FormValues, ValidationRules } from '../types';

export const useFormState = <T extends FormValues>(
  initialForm: T,
  validationRules: ValidationRules<T> = {},
) => {
  const [form, setForm] = useState<T>(initialForm);
  const [errors, setErrors] = useState<Partial<Record<keyof T, string>>>({});
  const touchedRef = useRef<Partial<Record<keyof T, boolean>>>({});

  const validateField = useCallback(
    (name: keyof T, value: T[keyof T], currentForm: T) => {
      const validator = validationRules[name];
      if (validator) {
        const error = validator(value, currentForm);
        setErrors((prev) => ({ ...prev, [name]: error }));
      }
    },
    [validationRules],
  );

  const handleBlur = useCallback(
    (e: React.FocusEvent<HTMLInputElement>) => {
      const { name, value } = e.target;
      touchedRef.current = { ...touchedRef.current, [name as keyof T]: true };

      setForm((prev) => {
        const currentForm = prev;
        validateField(name as keyof T, value as T[keyof T], currentForm);
        return prev;
      });
    },
    [validateField],
  );

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const { name, value } = e.target;
      setForm((prev) => {
        const updatedForm = { ...prev, [name]: value as T[keyof T] };

        if (touchedRef.current[name as keyof T]) {
          validateField(name as keyof T, value as T[keyof T], updatedForm);
        } else {
          setErrors((prevErrors) => ({
            ...prevErrors,
            [name as keyof T]: undefined,
          }));
        }
        return updatedForm;
      });
    },
    [validateField],
  );

  const resetForm = useCallback(() => {
    setForm(initialForm);
    setErrors({});
  }, [initialForm]);

  return {
    form,
    errors,
    handleChange,
    handleBlur,
    resetForm,
  };
};
