import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles.css';

const issues = [
  {
    id: 1024,
    title: 'Water leakage near school road',
    location: 'Dasuya Panchayat',
    department: 'Water',
    jurisdiction: 'Hoshiarpur District',
    status: 'forwarded',
    priority: 'high',
    reporter: 'Priya Singh',
    createdAt: '30 Apr 2026, 10:20 AM',
    timeline: [
      {
        id: 1,
        type: 'CREATED',
        actor: 'Citizen',
        remarks: 'Issue reported with location and photo evidence.',
        newStatus: 'open',
        createdAt: '10:20 AM'
      },
      {
        id: 2,
        type: 'ASSIGNED',
        actor: 'District Admin',
        remarks: 'Assigned to Water Department for inspection.',
        toDepartment: 'Water',
        oldStatus: 'open',
        newStatus: 'assigned',
        createdAt: '10:46 AM'
      },
      {
        id: 3,
        type: 'FORWARDED',
        actor: 'Water Department',
        remarks: 'Road surface repair is also required after pipeline work.',
        fromDepartment: 'Water',
        toDepartment: 'Road',
        oldStatus: 'assigned',
        newStatus: 'forwarded',
        createdAt: '12:15 PM'
      }
    ]
  },
  {
    id: 1025,
    title: 'Garbage pile beside market gate',
    location: 'Amritsar Ward 5',
    department: 'Sanitation',
    jurisdiction: 'Amritsar District',
    status: 'in_progress',
    priority: 'medium',
    reporter: 'Rahul Sharma',
    createdAt: '30 Apr 2026, 09:05 AM',
    timeline: [
      {
        id: 1,
        type: 'CREATED',
        actor: 'Citizen',
        remarks: 'Garbage has not been collected for three days.',
        newStatus: 'open',
        createdAt: '09:05 AM'
      },
      {
        id: 2,
        type: 'STATUS_CHANGED',
        actor: 'Sanitation Department',
        remarks: 'Pickup team has been dispatched.',
        oldStatus: 'assigned',
        newStatus: 'in_progress',
        createdAt: '11:30 AM'
      }
    ]
  },
  {
    id: 1026,
    title: 'Street light not working',
    location: 'Ludhiana Ward 8',
    department: 'Electricity',
    jurisdiction: 'Ludhiana District',
    status: 'resolved',
    priority: 'low',
    reporter: 'Aman Kaur',
    createdAt: '29 Apr 2026, 06:40 PM',
    timeline: [
      {
        id: 1,
        type: 'CREATED',
        actor: 'Citizen',
        remarks: 'Street light is out near the bus stop.',
        newStatus: 'open',
        createdAt: '06:40 PM'
      },
      {
        id: 2,
        type: 'CLOSED_WITH_PROOF',
        actor: 'Electricity Department',
        remarks: 'Bulb replaced and verified after sunset.',
        oldStatus: 'in_progress',
        newStatus: 'resolved',
        proofImageUrl: 'Proof image attached',
        createdAt: '08:15 PM'
      }
    ]
  }
];

const statusLabels = {
  open: 'Open',
  assigned: 'Assigned',
  in_progress: 'In Progress',
  forwarded: 'Forwarded',
  resolved: 'Resolved'
};

function App() {
  const [selectedId, setSelectedId] = React.useState(issues[0].id);
  const selectedIssue = issues.find((issue) => issue.id === selectedId) ?? issues[0];

  const counts = issues.reduce(
    (accumulator, issue) => {
      accumulator[issue.status] += 1;
      return accumulator;
    },
    { open: 0, assigned: 0, in_progress: 0, forwarded: 0, resolved: 0 }
  );

  return (
    <main className="app-shell">
      <aside className="sidebar" aria-label="Admin navigation">
        <div className="brand">
          <span className="brand-mark">SS</span>
          <div>
            <strong>SevaSetu</strong>
            <span>Admin</span>
          </div>
        </div>
        <nav>
          <button className="nav-item active">Issues</button>
          <button className="nav-item">Timeline</button>
          <button className="nav-item">Departments</button>
          <button className="nav-item">Staff</button>
        </nav>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">Punjab State</p>
            <h1>Issue Management</h1>
          </div>
          <div className="topbar-actions">
            <select aria-label="Jurisdiction">
              <option>All jurisdictions</option>
              <option>Amritsar District</option>
              <option>Hoshiarpur District</option>
              <option>Ludhiana District</option>
            </select>
            <select aria-label="Department">
              <option>All departments</option>
              <option>Water</option>
              <option>Road</option>
              <option>Sanitation</option>
              <option>Electricity</option>
            </select>
          </div>
        </header>

        <section className="stats" aria-label="Issue status counters">
          {Object.entries(counts).map(([status, count]) => (
            <div className="stat" key={status}>
              <span>{statusLabels[status]}</span>
              <strong>{count}</strong>
            </div>
          ))}
        </section>

        <section className="content-grid">
          <div className="issue-list">
            <div className="section-heading">
              <h2>Tickets</h2>
              <button>Export</button>
            </div>
            <div className="table" role="table" aria-label="Issues">
              <div className="table-row table-head" role="row">
                <span>Issue</span>
                <span>Department</span>
                <span>Status</span>
                <span>Priority</span>
              </div>
              {issues.map((issue) => (
                <button
                  className={`table-row issue-row ${issue.id === selectedId ? 'selected' : ''}`}
                  key={issue.id}
                  onClick={() => setSelectedId(issue.id)}
                  role="row"
                >
                  <span>
                    <strong>#{issue.id}</strong>
                    <small>{issue.title}</small>
                  </span>
                  <span>{issue.department}</span>
                  <span className={`pill ${issue.status}`}>{statusLabels[issue.status]}</span>
                  <span className={`priority ${issue.priority}`}>{issue.priority}</span>
                </button>
              ))}
            </div>
          </div>

          <aside className="detail-panel" aria-label="Selected issue details">
            <div className="detail-header">
              <span className={`pill ${selectedIssue.status}`}>
                {statusLabels[selectedIssue.status]}
              </span>
              <h2>{selectedIssue.title}</h2>
              <p>
                #{selectedIssue.id} by {selectedIssue.reporter} in {selectedIssue.location}
              </p>
            </div>

            <div className="action-grid">
              <button>Assign</button>
              <button>Forward</button>
              <button>Add Remarks</button>
              <button className="primary">Close With Proof</button>
            </div>

            <section className="timeline">
              <h3>Public Timeline</h3>
              {selectedIssue.timeline.map((event) => (
                <article className="timeline-event" key={event.id}>
                  <div>
                    <strong>{event.type.replaceAll('_', ' ')}</strong>
                    <time>{event.createdAt}</time>
                  </div>
                  <p>{event.remarks}</p>
                  <span>{event.actor}</span>
                  {event.toDepartment ? <span>To: {event.toDepartment}</span> : null}
                  {event.proofImageUrl ? <span>{event.proofImageUrl}</span> : null}
                </article>
              ))}
            </section>
          </aside>
        </section>
      </section>
    </main>
  );
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