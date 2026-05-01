import React from 'react';

export default function IssueTimeline({ issueId, updates = [] }) {
  if (!updates || updates.length === 0) {
    return (
      <section className="timeline-section">
        <h3>Public Timeline</h3>
        <div className="empty-timeline">
          <p>No timeline events yet</p>
        </div>
      </section>
    );
  }

  const getDepartmentName = (dept) => {
    if (!dept) return '';
    return typeof dept === 'string' ? dept : dept.name || '';
  };

  return (
    <section className="timeline-section">
      <h3>Full Timeline</h3>
      <div className="timeline">
        {updates.map((update, idx) => (
          <article className="timeline-event" key={update.id || idx}>
            <div className="timeline-marker">
              <div className="timeline-dot"></div>
            </div>
            <div className="timeline-content">
              <div className="timeline-header">
                <strong className="event-type">
                  {update.type?.replaceAll('_', ' ') || 'Update'}
                </strong>
                <time className="event-time">
                  {new Date(update.createdAt).toLocaleString()}
                </time>
              </div>
              {update.remarks && (
                <p className="event-remarks">{update.remarks}</p>
              )}
              {update.oldStatus && update.newStatus && (
                <p className="event-status">
                  Status: <strong>{update.oldStatus}</strong> → <strong>{update.newStatus}</strong>
                </p>
              )}
              {(update.fromDepartment || update.toDepartment) && (
                <p className="event-departments">
                  {getDepartmentName(update.fromDepartment) && (
                    <>From <strong>{getDepartmentName(update.fromDepartment)}</strong></>
                  )}
                  {getDepartmentName(update.fromDepartment) && getDepartmentName(update.toDepartment) && <> to </>}
                  {getDepartmentName(update.toDepartment) && (
                    <><strong>{getDepartmentName(update.toDepartment)}</strong></>
                  )}
                </p>
              )}
              {update.actor && (
                <p className="event-actor">
                  By <strong>{update.actor.name || update.actor.email}</strong>
                </p>
              )}
              {update.proofImageUrl && (
                <div className="event-proof">
                  <a
                    href={update.proofImageUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="proof-link"
                  >
                    📎 View Proof Image
                  </a>
                </div>
              )}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
