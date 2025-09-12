import { useActionState } from 'react';

async function authAction() {
  // TODO
}

export const useAuthAction = () => {
  const [state, formAction] = useActionState(authAction, null);
  return { state, formAction };
};
