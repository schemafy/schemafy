import { ThemeProvider } from '@/lib';
import { ReactFlowProvider } from '@xyflow/react';
import { Layout } from './components';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { TooltipProvider } from '@/components';
import { LandingPage, SignInPage, SignUpPage, CanvasPage } from '@/pages';

function App() {
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
