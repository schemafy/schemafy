import { queryClient, ThemeProvider } from '@/lib';
import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from '@tanstack/react-router';
import { ReactFlowProvider } from '@xyflow/react';
import { Toaster, TooltipProvider } from '@/components';
import { useAuthBootstrap } from '@/features/auth';
import { authStore } from '@/store/auth.store';
import { router } from '@/router';
import { reaction } from 'mobx';
import { useEffect } from 'react';

const isProtectedPath = (pathname: string) => {
  return pathname === '/workspace' || pathname.startsWith('/project/');
};

function App() {
  useAuthBootstrap();

  useEffect(() => {
    const dispose = reaction(
      () => ({
        isInitialized: authStore.isInitialized,
        isAuthLoading: authStore.isAuthLoading,
        isAuthenticated: Boolean(authStore.accessToken && authStore.user),
      }),
      async ({ isInitialized, isAuthLoading, isAuthenticated }) => {
        if (!isInitialized || isAuthLoading || isAuthenticated) {
          return;
        }

        const { pathname } = router.state.location;
        if (!isProtectedPath(pathname)) {
          return;
        }

        await router.navigate({
          to: '/signin',
          replace: true,
          search: { oauthError: null, authRequired: true },
        });
      },
    );

    return dispose;
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider defaultTheme="system" storageKey="schemafy-theme">
        <TooltipProvider>
          <ReactFlowProvider>
            <RouterProvider
              router={router}
              context={{ queryClient, auth: authStore }}
            />
            <Toaster />
          </ReactFlowProvider>
        </TooltipProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export default App;
