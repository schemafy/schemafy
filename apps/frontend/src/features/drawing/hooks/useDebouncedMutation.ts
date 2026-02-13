import { useEffect, useRef } from 'react';
import { debounce } from 'lodash-es';
import type { UseMutationResult } from '@tanstack/react-query';

const DEBOUNCE_DELAY = 500;

export const useDebouncedMutation = <TData, TError, TVariables, TContext>(
  mutation: UseMutationResult<TData, TError, TVariables, TContext>,
) => {
  const mutationRef = useRef(mutation);
  mutationRef.current = mutation;

  const debouncedMutateRef = useRef(
    debounce((variables: TVariables) => {
      mutationRef.current.mutate(variables);
    }, DEBOUNCE_DELAY),
  );

  useEffect(() => {
    const debouncedFn = debouncedMutateRef.current;
    return () => {
      debouncedFn.cancel();
    };
  }, []);

  return {
    ...mutation,
    mutate: debouncedMutateRef.current,
  };
};
