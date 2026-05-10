import React, { useState } from 'react';
import { adminApi } from '../api';

const getPreviousMonth = () => {
  const now = new Date();
  const prevMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const year = prevMonthDate.getFullYear();
  const month = String(prevMonthDate.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
};

export default function StateReportPanel() {
  const [month, setMonth] = useState(getPreviousMonth());
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [result, setResult] = useState(null);

  const triggerMonthlySummary = async (event) => {
    event.preventDefault();
    setLoading(true);
    setMessage({ type: '', text: '' });
    setResult(null);

    try {
      const payload = await adminApi.stateAdminSendMonthlySummary(month);
      setResult(payload);
      setMessage({ type: 'success', text: 'Monthly summary generated and sent successfully' });
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="reports-grid" aria-label="Reports panel">
      {message.text && (
        <div className={`alert alert-${message.type}`}>
          {message.text}
        </div>
      )}

      <section className="management-card reports-card">
        <div className="section-heading compact reports-heading">
          <h2>Monthly Summary</h2>
          <p>Generate one statewide report and notify all active users instantly.</p>
        </div>

        <form className="management-form reports-form" onSubmit={triggerMonthlySummary}>
          <div className="reports-controls">
            <label>
              Month
              <input
                type="month"
                value={month}
                onChange={(event) => setMonth(event.target.value)}
                required
              />
            </label>

            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? 'Running Summary...' : 'Generate and Send'}
            </button>
          </div>
          <small>
            This action generates a fresh statewide report, uploads it to Cloudinary, and sends one notification per active user.
          </small>
        </form>

        {result && (
          <div className="reports-result">
            <div className={`reports-summary ${result.failedCount > 0 ? 'warn' : 'ok'}`}>
              {result.failedCount > 0
                ? `Completed with partial failures: ${result.sentCount} sent, ${result.failedCount} failed.`
                : `Completed successfully: ${result.sentCount} notifications sent.`}
            </div>

            <div className="reports-metrics">
              <article className="report-metric">
                <span>Month</span>
                <strong>{result.month}</strong>
              </article>
              <article className="report-metric">
                <span>Total Users</span>
                <strong>{result.totalUsers}</strong>
              </article>
              <article className="report-metric">
                <span>Reports Generated</span>
                <strong>{result.generatedCount}</strong>
              </article>
              <article className="report-metric">
                <span>Sent</span>
                <strong>{result.sentCount}</strong>
              </article>
              <article className="report-metric">
                <span>Failed</span>
                <strong>{result.failedCount}</strong>
              </article>
            </div>
          </div>
        )}
      </section>
    </section>
  );
}
