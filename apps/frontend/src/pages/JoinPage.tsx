import { useEffect, useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { observer } from 'mobx-react-lite';
import { AuthStore } from '@/store';
import { useShareLinkStore } from '@/hooks';
import { Button } from '@/components';

const JoinPageComponent = () => {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const authStore = AuthStore.getInstance();
  const { joinByShareLink, error } = useShareLinkStore();

  const [status, setStatus] = useState<'loading' | 'success' | 'error'>(
    'loading',
  );
  const [errorMessage, setErrorMessage] = useState<string>('');

  useEffect(() => {
    const joinProject = async () => {
      if (!token) {
        setStatus('error');
        setErrorMessage('Invalid share link');
        return;
      }

      if (!authStore.user) {
        navigate('/signin', { state: { from: location } });
        return;
      }

      try {
        const result = await joinByShareLink(token);
        if (result) {
          setStatus('success');
          navigate('/projects');
        } else {
          setStatus('error');
          setErrorMessage(error || 'Failed to join project');
        }
      } catch {
        setStatus('error');
        setErrorMessage('Failed to join project');
      }
    };

    if (!authStore.isAuthLoading) {
      joinProject();
    }
  }, [
    authStore.isAuthLoading,
    authStore.user,
    navigate,
    joinByShareLink,
    error,
    token,
    location,
  ]);

  if (status === 'loading') {
    return (
      <div className="flex flex-col w-full items-center justify-center min-h-[60vh] gap-4">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-schemafy-primary"></div>
        <p className="font-body-md text-schemafy-text">Joining project...</p>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="flex flex-col w-full items-center justify-center min-h-[60vh] gap-6">
        <h1 className="font-heading-lg text-schemafy-text">
          Unable to join project
        </h1>
        <p className="font-body-md text-schemafy-dark-gray">{errorMessage}</p>
        <Button to="/projects">Go to Projects</Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col w-full items-center justify-center min-h-[60vh] gap-6">
      <div className="text-green-500 text-5xl">✓</div>
      <h1 className="font-heading-lg text-schemafy-text">
        Successfully joined!
      </h1>
      <p className="font-body-md text-schemafy-dark-gray">
        Redirecting to projects...
      </p>
    </div>
  );
};

export const JoinPage = observer(JoinPageComponent);
