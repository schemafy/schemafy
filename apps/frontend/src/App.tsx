import { ThemeProvider } from '@/lib';
import { ReactFlowProvider } from '@xyflow/react';
import { Layout } from './components';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { TooltipProvider } from '@/components';
import { LandingPage, SignInPage, SignUpPage, CanvasPage } from '@/pages';
import { useEffect } from 'react';
import { authStore, AuthProvider } from '@/store/auth.store';
import { getMyInfo, refreshToken } from '@/lib/api';
import { RequireAuth } from '@/features/auth';

function App() {
  useEffect(() => {
    const bootstrapAuth = async () => {
      try {
        authStore.setAuthLoading(true);
        await refreshToken();

        const res = await getMyInfo();
        if (res.success && res.result) {
          authStore.setUser(res.result);
        } else {
          authStore.clearAuth();
        }
      } catch {
        authStore.clearAuth();
      } finally {
        authStore.setAuthLoading(false);
        authStore.setInitialized(true);
      }
    };
    bootstrapAuth();
  }, []);

  return (
    <ThemeProvider defaultTheme="system" storageKey="schemafy-theme">
      <AuthProvider value={authStore}>
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
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
