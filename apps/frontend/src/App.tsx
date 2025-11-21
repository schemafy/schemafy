import { ThemeProvider } from '@/lib';
import { ReactFlowProvider } from '@xyflow/react';
import { Layout } from './components';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { LandingPage, SignInPage, SignUpPage, CanvasPage } from '@/pages';

function App() {
  return (
    <ThemeProvider defaultTheme="system" storageKey="schemafy-theme">
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
    </ThemeProvider>
  );
}

export default App;
