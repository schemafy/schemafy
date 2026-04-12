import { queryClient, ThemeProvider } from '@/lib';
import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from '@tanstack/react-router';
import { ReactFlowProvider } from '@xyflow/react';
import { Toaster, TooltipProvider } from '@/components';
import { useAuthBootstrap } from '@/features/auth';
import { authStore } from '@/store/auth.store';
import { router } from '@/router';

function App() {
  useAuthBootstrap();

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
