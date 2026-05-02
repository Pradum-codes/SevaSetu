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

  const formatEventType = (type) => type?.replaceAll('_', ' ') || 'Update';

  const formatDate = (date) => {
    if (!date) return '-';
    const parsed = new Date(date);
    return Number.isNaN(parsed.getTime()) ? '-' : parsed.toLocaleString();
  };

  const formatDepartments = (update) => {
    const fromDepartment = getDepartmentName(update.fromDepartment);
    const toDepartment = getDepartmentName(update.toDepartment);

    if (fromDepartment && toDepartment) return `${fromDepartment} to ${toDepartment}`;
    return fromDepartment || toDepartment || '-';
  };

  const formatActor = (actor) => {
    if (!actor) return '-';
    return actor.name || actor.email || '-';
  };

  return (
    <section className="timeline-section">
      <h3>Full Timeline</h3>
      <div className="timeline-table-wrap">
        <table className="timeline-table">
          <thead>
            <tr>
              <th>Event</th>
              <th>Date & Time</th>
              <th>Status</th>
              <th>Departments</th>
              <th>Actor</th>
              <th>Remarks</th>
              <th>Proof</th>
            </tr>
          </thead>
          <tbody>
            {updates.map((update, idx) => (
              <tr key={update.id || idx}>
                <td>
                  <strong className="event-type">{formatEventType(update.type)}</strong>
                </td>
                <td>
                  <time dateTime={update.createdAt || undefined}>
                    {formatDate(update.createdAt)}
                  </time>
                </td>
                <td>
                  {update.oldStatus && update.newStatus ? (
                    <span className="status-transition">
                      <strong>{update.oldStatus}</strong>
                      <span aria-hidden="true"> → </span>
                      <strong>{update.newStatus}</strong>
                    </span>
                  ) : (
                    '-'
                  )}
                </td>
                <td>{formatDepartments(update)}</td>
                <td>{formatActor(update.actor)}</td>
                <td className="timeline-remarks">{update.remarks || '-'}</td>
                <td>
                  {update.proofImageUrl ? (
                    <a
                      href={update.proofImageUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="proof-link"
                    >
                      View proof
                    </a>
                  ) : (
                    '-'
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
