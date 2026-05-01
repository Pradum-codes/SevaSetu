import React, { useState, useEffect } from 'react';
import ReactDOM from 'react-dom/client';
import './styles.css';
import LoginPage from './LoginPage';
import Dashboard from './Dashboard';
import { isAuthenticated } from './api';

function App() {
  const [admin, setAdmin] = useState(null);
  const [initialized, setInitialized] = useState(false);

  // Check if user is already logged in on app load
  useEffect(() => {
    if (isAuthenticated()) {
      // In a real app, we would fetch the admin profile from the API
      // For now, we'll just set a placeholder
      setAdmin({
        id: 'admin-placeholder',
        name: 'Admin User',
        email: 'admin@example.com',
        authorityProfile: {
          jurisdiction: {
            name: 'Admin Scope'
          }
        }
      });
    }
    setInitialized(true);
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