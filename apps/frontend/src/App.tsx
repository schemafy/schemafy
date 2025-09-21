import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import { ThemeProvider } from '@/lib';
import { Layout } from '@/components';
import { LandingPage, SignInPage, SignUpPage } from '@/pages';

function App() {
  return (
    <ThemeProvider defaultTheme="system" storageKey="schemafy-theme">
      <Router>
        <Layout>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/signup" element={<SignUpPage />} />
            <Route path="/signin" element={<SignInPage />} />
          </Routes>
        </Layout>
      </Router>
    </ThemeProvider>
  );
}

export default App;
