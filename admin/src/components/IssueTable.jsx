import React from 'react';

export default function IssueTable({
  issues,
  selectedId,
  onSelectIssue,
  statusLabels,
}) {
  return (
    <div className="issue-list">
      <div className="section-heading">
        <h2>Tickets ({issues.length})</h2>
        <button className="export-btn">Export</button>
      </div>
      <div className="table" role="table" aria-label="Issues table">
        <div className="table-row table-head" role="row">
          <span className="col-issue">Issue</span>
          <span className="col-department">Department</span>
          <span className="col-status">Status</span>
          <span className="col-priority">Priority</span>
          <span className="col-date">Created</span>
        </div>
        {issues.map((issue) => (
          <button
            className={`table-row issue-row ${issue.id === selectedId ? 'selected' : ''}`}
            key={issue.id}
            onClick={() => onSelectIssue(issue.id)}
            role="row"
            aria-selected={issue.id === selectedId}
          >
            <span className="col-issue">
              <strong>#{issue.id}</strong>
              <small>{issue.title}</small>
            </span>
            <span className="col-department">
              {issue.department?.name || 'Unassigned'}
            </span>
            <span className="col-status">
              <span className={`pill ${issue.status}`}>
                {statusLabels[issue.status] || issue.status}
              </span>
            </span>
            <span className="col-priority">
              <span className={`priority-badge ${issue.priority || 'medium'}`}>
                {issue.priority || 'medium'}
              </span>
            </span>
            <span className="col-date">
              {new Date(issue.createdAt).toLocaleDateString()}
            </span>
          </button>
        ))}
      </div>
    </div>
  );
}
