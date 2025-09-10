import { ThemeProvider } from '@/lib';
import { Layout } from './components';
import { BrowserRouter as Router, Routes } from 'react-router-dom';

function App() {
  return (
    <ThemeProvider defaultTheme="system" storageKey="schemafy-theme">
      <Router>
        <Layout>
          <Routes></Routes>
        </Layout>
      </Router>
    </ThemeProvider>
  );
}

export default App;
