import { ThemeProvider } from '@/lib';
import { ReactFlowProvider } from '@xyflow/react';
import { Layout } from './components';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { TooltipProvider } from '@/components';
import { LandingPage, SignInPage, SignUpPage, CanvasPage } from '@/pages';
import { useEffect } from 'react';
import { AuthStore } from '@/store/auth.store';
import { getMyInfo, refreshToken } from '@/lib/api';
import { RequireAuth } from '@/features/auth';

function App() {
  const authStore = AuthStore.getInstance();

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
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
                    // <RequireAuth>
                    <CanvasPage />
                    // </RequireAuth>
                  }
                />
              </Routes>
            </Layout>
          </Router>
        </ReactFlowProvider>
      </TooltipProvider>
    </ThemeProvider>
  );
}

export default App;
