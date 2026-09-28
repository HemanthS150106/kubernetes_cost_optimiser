import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from './context/AuthContext';
import { getTheme } from './theme';
import { Layout } from './components/Layout';
import { Login } from './pages/Login';
import { Dashboard } from './pages/Dashboard';
import { Recommendations } from './pages/Recommendations';
import { ClusterVisualizer } from './pages/ClusterVisualizer';
import { AuditLogs } from './pages/AuditLogs';
import { TemporalProfiles } from './pages/TemporalProfiles';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

const AppContent: React.FC = () => {
  const [themeMode, setThemeMode] = useState<'light' | 'dark'>(() => {
    return (localStorage.getItem('themeMode') as 'light' | 'dark') || 'dark';
  });

  const toggleTheme = () => {
    const nextMode = themeMode === 'light' ? 'dark' : 'light';
    localStorage.setItem('themeMode', nextMode);
    setThemeMode(nextMode);
  };

  const theme = getTheme(themeMode);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout mode={themeMode} toggleTheme={toggleTheme} />
              </ProtectedRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="recommendations" element={<Recommendations />} />
            <Route path="visualizer" element={<ClusterVisualizer />} />
            <Route path="audit-logs" element={<AuditLogs />} />
            <Route path="temporal-profiles" element={<TemporalProfiles />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
};

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
