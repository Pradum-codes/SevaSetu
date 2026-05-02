import React from 'react';

export default function IssueDetails({
  issue,
  departments,
  statusLabels,
  onOpenModal,
  adminRole,
}) {
  // Determine which action buttons to show based on role
  const renderActionButtons = () => {
    switch (adminRole) {
      case 'STATE':
        return (
          <span className="action-note">Automatically routed to district</span>
        );

      case 'DISTRICT':
        return (
          <>
            <button
              className="action-btn"
              onClick={() => onOpenModal('assign', issue.id)}
            >
              Assign to Department
            </button>
            <button
              className="action-btn primary"
              onClick={() => onOpenModal('close', issue.id)}
            >
              Close After Review
            </button>
          </>
        );

      case 'AUTHORITY':
        // Department admin
        return (
          <>
            <button
              className="action-btn"
              onClick={() => onOpenModal('status', issue.id)}
            >
              Update Status
            </button>
            <button
              className="action-btn primary"
              onClick={() => onOpenModal('submit-proof', issue.id)}
            >
              Submit Proof to District
            </button>
          </>
        );

      default:
        // Generic admin (backward compatibility)
        return (
          <>
            <button
              className="action-btn"
              onClick={() => onOpenModal('assign', issue.id)}
            >
              Assign
            </button>
            <button
              className="action-btn"
              onClick={() => onOpenModal('forward', issue.id)}
            >
              Forward
            </button>
            <button
              className="action-btn"
              onClick={() => onOpenModal('remarks', issue.id)}
            >
              Add Remarks
            </button>
            <button
              className="action-btn"
              onClick={() => onOpenModal('status', issue.id)}
            >
              Change Status
            </button>
            <button
              className="action-btn primary"
              onClick={() => onOpenModal('close', issue.id)}
            >
              Close with Proof
            </button>
          </>
        );
    }
  };

  const getRoleInfo = () => {
    switch (adminRole) {
      case 'STATE':
        return 'State Admin - Read and monitor district routing';
      case 'DISTRICT':
        return 'District Admin - Assign to departments and close after review';
      case 'AUTHORITY':
        return 'Department Admin - Update status and submit proof';
      default:
        return 'Admin';
    }
  };

  return (
    <aside className="detail-panel" aria-label="Issue details panel">
      <div className="detail-header">
        <div className="header-top">
          <span className={`pill ${issue.status}`}>
            {statusLabels[issue.status] || issue.status}
          </span>
          <span className={`priority-badge ${issue.priority || 'medium'}`}>
            {issue.priority || 'medium'} Priority
          </span>
        </div>
        <h2>{issue.title}</h2>
        <p className="issue-meta">
          #{issue.id} • {issue.category?.name || 'General'}
        </p>
        <small style={{ color: '#666', marginTop: '4px' }}>{getRoleInfo()}</small>
      </div>

      <div className="detail-section">
        <h3>Description</h3>
        <p>{issue.description}</p>
        {issue.imageUrl && (
          <div className="issue-image">
            <img src={issue.imageUrl} alt="Issue" />
          </div>
        )}
      </div>

      <div className="detail-section">
        <h3>Location</h3>
        <p>
          <strong>{issue.addressText}</strong>
        </p>
        {issue.landmark && <p className="text-muted">Landmark: {issue.landmark}</p>}
        {issue.locality && <p className="text-muted">Locality: {issue.locality}</p>}
        {issue.lat && issue.lng && (
          <p className="text-muted">
            Coordinates: {issue.lat.toFixed(4)}, {issue.lng.toFixed(4)}
          </p>
        )}
      </div>

      <div className="detail-section">
        <h3>Current Assignment</h3>
        <p>
          <strong>Department:</strong>{' '}
          {issue.department?.name || 'Unassigned'}
        </p>
        <p>
          <strong>Status:</strong> {statusLabels[issue.status] || issue.status}
        </p>
        <p>
          <strong>Created:</strong>{' '}
          {new Date(issue.createdAt).toLocaleString()}
        </p>
      </div>

      <div className="action-grid">
        {renderActionButtons()}
      </div>

      {issue.updates && issue.updates.length > 0 && (
        <div className="detail-section">
          <h3>Recent Activity</h3>
          <div className="activity-list">
            {issue.updates.slice(0, 3).map((update) => (
              <div key={update.id} className="activity-item">
                <strong>{update.type?.replaceAll('_', ' ')}</strong>
                <small>{new Date(update.createdAt).toLocaleString()}</small>
                {update.remarks && <p>{update.remarks}</p>}
              </div>
            ))}
          </div>
        </div>
      )}

    </aside>
  );
}
