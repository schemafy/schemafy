import { ThemeProvider } from '@/lib';
import { ReactFlowProvider } from '@xyflow/react';
import { Layout } from './components';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { LandingPage } from './pages';
import { ERDDiagramTool } from '@/features/drawing';

function App() {
  return (
    <ThemeProvider defaultTheme="system" storageKey="schemafy-theme">
      <ReactFlowProvider>
        <Router>
          <Layout>
            <Routes>
              <Route path="/" element={<LandingPage />} />
              <Route path="/canvas" element={<ERDDiagramTool />} />
            </Routes>
          </Layout>
        </Router>
      </ReactFlowProvider>
    </ThemeProvider>
  );
}

export default App;
