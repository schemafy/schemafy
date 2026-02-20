import { ThemeProvider, queryClient } from '@/lib';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactFlowProvider } from '@xyflow/react';
import { Layout } from './components';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { TooltipProvider } from '@/components';
import { LandingPage, SignInPage, SignUpPage, CanvasPage } from '@/pages';
import { RequireAuth, useAuthBootstrap } from '@/features/auth';

function App() {
  useAuthBootstrap();

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider defaultTheme="system" storageKey="schemafy-theme">
        <TooltipProvider>
          <ReactFlowProvider>
            <Router>
              <Layout>
                <Routes>
                  <Route path="/" element={<LandingPage />} />
                  <Route path="/signup" element={<SignUpPage />} />
                  <Route path="/signin" element={<SignInPage />} />
                  <Route
                    path="/canvas"
                    element={
                      <RequireAuth>
                        <CanvasPage />
                      </RequireAuth>
                    }
                  />
                </Routes>
              </Layout>
            </Router>
          </ReactFlowProvider>
        </TooltipProvider>
      </ThemeProvider>
    </QueryClientProvider>
  );
}

export default App;
