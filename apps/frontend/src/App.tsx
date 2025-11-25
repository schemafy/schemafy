import { ThemeProvider } from '@/lib';
import { ReactFlowProvider } from '@xyflow/react';
import { Layout } from './components';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { TooltipProvider } from '@/components';
import { LandingPage, SignInPage, SignUpPage, CanvasPage } from '@/pages';
import { useEffect } from 'react';
import { useAuthStore } from '@/store';
import { getMyInfo, refreshToken } from '@/lib/api';

function App() {
  const setUser = useAuthStore((s) => s.setUser);

  useEffect(() => {
    const bootstrapAuth = async () => {
      try {
        await refreshToken();

        const token = useAuthStore.getState().accessToken;
        if (!token) {
          setUser(null);
          return;
        }

        const res = await getMyInfo();
        if (res.success && res.result) {
          setUser(res.result);
        } else {
          setUser(null);
        }
      } catch {
        setUser(null);
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
                <Route path="/canvas" element={<CanvasPage />} />
              </Routes>
            </Layout>
          </Router>
        </ReactFlowProvider>
      </TooltipProvider>
    </ThemeProvider>
  );
}

export default App;
