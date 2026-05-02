import React, { useState, useEffect } from 'react';
import ReactDOM from 'react-dom/client';
import './styles.css';
import LoginPage from './LoginPage';
import Dashboard from './Dashboard';
import { adminApi, clearToken, isAuthenticated } from './api';

function App() {
  const [admin, setAdmin] = useState(null);
  const [initialized, setInitialized] = useState(false);

  // Check if user is already logged in on app load
  useEffect(() => {
    const initializeAdmin = async () => {
      if (!isAuthenticated()) {
        setInitialized(true);
        return;
      }

      try {
        const response = await adminApi.getMe();
        setAdmin(response.admin);
      } catch (error) {
        console.error('Failed to restore admin session:', error);
        clearToken();
        setAdmin(null);
      } finally {
        setInitialized(true);
      }
    };

    initializeAdmin();
  }, []);

  const handleLoginSuccess = (adminData) => {
    setAdmin(adminData);
  };

  const handleLogout = () => {
    setAdmin(null);
  };

  if (!initialized) {
    return <div className="loading">Loading...</div>;
  }

  if (!admin) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }

  return <Dashboard admin={admin} onLogout={handleLogout} />;
}

const rootElement = document.getElementById('root');

if (!rootElement) {
  throw new Error('Root element not found');
}

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
