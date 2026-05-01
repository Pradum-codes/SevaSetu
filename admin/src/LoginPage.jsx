import React, { useState } from 'react';
import { adminApi, setToken } from './api';

export default function LoginPage({ onLoginSuccess }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await adminApi.login(email, password);
      setToken(response.token);
      onLoginSuccess(response.admin);
    } catch (err) {
      setError(err.message || 'Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page">
      <div className="login-card">
        <div className="login-brand">
          <span className="brand-mark">SS</span>
          <div>
            <strong>SevaSetu</strong>
            <span>Admin Portal</span>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          <h1>Admin Login</h1>

          {error && <div className="error-banner">{error}</div>}

          <label htmlFor="email">
            Email
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@example.com"
              required
              disabled={loading}
            />
          </label>

          <label htmlFor="password">
            Password
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              disabled={loading}
            />
          </label>

          <button type="submit" disabled={loading} className="primary">
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <p className="login-footer">
          Demo credentials for testing<br />
          Email: admin@example.com<br />
          Password: admin123
        </p>
      </div>
    </main>
  );
}
