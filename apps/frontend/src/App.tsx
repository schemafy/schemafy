import { ThemeProvider } from '@/lib';
import { Layout } from './components';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { LandingPage } from './pages';

function App() {
  return (
    <ThemeProvider defaultTheme="system" storageKey="schemafy-theme">
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<LandingPage />} />
          </Routes>
        </Layout>
      </Router>
    </ThemeProvider>
  );
}

export default App;
