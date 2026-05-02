import React, { useState, useEffect } from 'react';
import { adminApi, clearToken } from './api';
import IssueTable from './components/IssueTable';
import IssueDetails from './components/IssueDetails';
import IssueTimeline from './components/IssueTimeline';
import ActionModal from './components/ActionModal';
import ManagementPanel from './components/ManagementPanel';

const statusLabels = {
  open: 'Open',
  assigned: 'Assigned',
  in_progress: 'In Progress',
  forwarded: 'Forwarded',
  resolved: 'Resolved',
  closed: 'Closed',
};

// Determine admin role based on jurisdiction type
const getAdminRole = (admin) => {
  if (admin?.authorityProfile?.designation === 'Department Head') {
    return 'AUTHORITY';
  }
  if (!admin?.authorityProfile?.jurisdiction) return null;
  return admin.authorityProfile.jurisdiction.type;
};

export default function Dashboard({ admin, onLogout }) {
  const [issues, setIssues] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filters, setFilters] = useState({
    status: '',
    targetId: '', // district for state, department for district, etc.
  });
  const [availableTargets, setAvailableTargets] = useState([]);
  const [timeline, setTimeline] = useState([]);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [modal, setModal] = useState({ type: null, issueId: null });
  const [actingLoading, setActingLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState({ type: '', text: '' });
  const [activeView, setActiveView] = useState('issues');

  const adminRole = getAdminRole(admin);

  // Fetch issues based on admin role
  useEffect(() => {
    const fetchIssues = async () => {
      setLoading(true);
      setError('');
      try {
        let response;

        if (adminRole === 'STATE') {
          response = await adminApi.stateAdminListIssues({
            status: filters.status === '__pending' ? '' : filters.status,
            statusGroup: filters.status === '__pending' ? 'pending' : '',
            districtId: filters.targetId,
          });
          setAvailableTargets(response.availableDistricts || []);
        } else if (adminRole === 'DISTRICT') {
          response = await adminApi.districtAdminListIssues({
            status: filters.status === '__pending' ? '' : filters.status,
            statusGroup: filters.status === '__pending' ? 'pending' : '',
            departmentId: filters.targetId,
          });
          setAvailableTargets(response.availableDepartments || []);
        } else if (adminRole === 'AUTHORITY' || !admin?.authorityProfile?.jurisdictionId) {
          // Department admin - no jurisdiction but has department assignment
          response = await adminApi.departmentAdminListIssues({
            status: filters.status === '__pending' ? '' : filters.status,
            statusGroup: filters.status === '__pending' ? 'pending' : '',
          });
        } else {
          // Fallback
          response = await adminApi.getIssues(filters);
        }

        setIssues(response.issues || []);
        if (response.issues?.length > 0 && !selectedId) {
          setSelectedId(response.issues[0].id);
        }
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    fetchIssues();
  }, [filters, selectedId, adminRole]);

  // Fetch timeline when issue is selected
  useEffect(() => {
    if (!selectedId) {
      setTimeline([]);
      return;
    }

    const fetchTimeline = async () => {
      setTimelineLoading(true);
      try {
        const response = await adminApi.getIssueTimeline(selectedId);
        setTimeline(response.timeline || []);
      } catch (err) {
        console.error('Failed to fetch timeline:', err);
        setTimeline([]);
      } finally {
        setTimelineLoading(false);
      }
    };

    fetchTimeline();
  }, [selectedId]);

  const selectedIssue = issues.find((issue) => issue.id === selectedId);

  const counts = issues.reduce(
    (acc, issue) => {
      acc[issue.status] = (acc[issue.status] || 0) + 1;
      return acc;
    },
    { open: 0, assigned: 0, in_progress: 0, forwarded: 0, resolved: 0, closed: 0 }
  );

  const handleLogout = () => {
    clearToken();
    onLogout();
  };

  const handleFilterChange = (type, value) => {
    setFilters((prev) => ({ ...prev, [type]: value }));
    setSelectedId(null);
  };

  const handleOpenModal = (type, issueId) => {
    setModal({ type, issueId });
    setActionMessage({ type: '', text: '' });
  };

  const handleCloseModal = () => {
    setModal({ type: null, issueId: null });
    setActionMessage({ type: '', text: '' });
  };

  const handleAction = async (action) => {
    setActingLoading(true);
    setActionMessage({ type: '', text: '' });

    try {
      const issueId = modal.issueId;

      switch (adminRole) {
        case 'STATE':
          if (action.type === 'forward-to-district') {
            await adminApi.stateAdminForwardToDistrict(issueId, action.toDistrictId, action.remarks);
            setActionMessage({ type: 'success', text: 'Issue forwarded to district successfully' });
          }
          break;

        case 'DISTRICT':
          if (action.type === 'assign') {
            await adminApi.districtAdminAssignToDepartment(issueId, action.departmentId, action.remarks);
            setActionMessage({ type: 'success', text: 'Issue assigned to department successfully' });
          } else if (action.type === 'close') {
            await adminApi.districtAdminCloseIssue(issueId, action.finalRemarks, action.proofImageUrl);
            setActionMessage({ type: 'success', text: 'Issue closed successfully after review' });
          }
          break;

        case 'AUTHORITY':
        default:
          // Department admin
          if (action.type === 'submit-proof') {
            await adminApi.departmentAdminSubmitProof(issueId, action.remarks, action.proofImageUrl);
            setActionMessage({ type: 'success', text: 'Proof submitted to district admin' });
          } else if (action.type === 'status') {
            await adminApi.departmentAdminUpdateStatus(issueId, action.newStatus, action.remarks);
            setActionMessage({ type: 'success', text: 'Issue status updated successfully' });
          }
          break;
      }

      // Refresh issues after action
      setTimeout(async () => {
        if (adminRole === 'STATE') {
          const response = await adminApi.stateAdminListIssues({
            status: filters.status === '__pending' ? '' : filters.status,
            statusGroup: filters.status === '__pending' ? 'pending' : '',
            districtId: filters.targetId,
          });
          setIssues(response.issues || []);
        } else if (adminRole === 'DISTRICT') {
          const response = await adminApi.districtAdminListIssues({
            status: filters.status === '__pending' ? '' : filters.status,
            statusGroup: filters.status === '__pending' ? 'pending' : '',
            departmentId: filters.targetId,
          });
          setIssues(response.issues || []);
        } else {
          const response = await adminApi.departmentAdminListIssues({
            status: filters.status === '__pending' ? '' : filters.status,
            statusGroup: filters.status === '__pending' ? 'pending' : '',
          });
          setIssues(response.issues || []);
        }
        handleCloseModal();
      }, 1000);
    } catch (err) {
      setActionMessage({ type: 'error', text: err.message });
    } finally {
      setActingLoading(false);
    }
  };

  const getRoleLabel = () => {
    switch (adminRole) {
      case 'STATE':
        return 'State Admin';
      case 'DISTRICT':
        return 'District Admin';
      case 'AUTHORITY':
        return 'Department Admin';
      default:
        return 'Admin';
    }
  };

  const getStatusOptions = () => {
    switch (adminRole) {
      case 'STATE':
        return ['open', 'forwarded', 'assigned', 'in_progress', 'resolved', 'closed'];
      case 'DISTRICT':
        return ['open', 'assigned', 'in_progress', 'resolved', 'closed'];
      case 'AUTHORITY':
        return ['assigned', 'in_progress', 'resolved'];
      default:
        return ['open', 'assigned', 'in_progress', 'forwarded', 'resolved', 'closed'];
    }
  };

  return (
    <main className="app-shell">
      <aside className="sidebar" aria-label="Admin navigation">
        <div className="brand">
          <span className="brand-mark">SS</span>
          <div>
            <strong>SevaSetu</strong>
            <span>{getRoleLabel()}</span>
          </div>
        </div>
        <nav>
          <button
            className={`nav-item ${activeView === 'issues' ? 'active' : ''}`}
            onClick={() => setActiveView('issues')}
          >
            Issues
          </button>
          {(adminRole === 'STATE' || adminRole === 'DISTRICT') && (
            <>
              <button
                className={`nav-item ${activeView === 'management' ? 'active' : ''}`}
                onClick={() => setActiveView('management')}
              >
                {adminRole === 'STATE' ? 'Districts' : 'Departments'}
              </button>
            </>
          )}
        </nav>
        <div className="sidebar-footer">
          <div className="admin-info">
            <p>{admin?.name || 'Admin'}</p>
            <small>{admin?.email}</small>
          </div>
          <button className="logout-btn" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">
              {admin?.authorityProfile?.jurisdiction?.name || 'Administration'}
            </p>
            <h1>
              {activeView === 'issues' ? 'Issue Management' : 'Administration'} - {getRoleLabel()}
            </h1>
          </div>
          {activeView === 'issues' && (
            <div className="topbar-actions">
              <select
                value={filters.status}
                onChange={(e) => handleFilterChange('status', e.target.value)}
                className="filter-select"
              >
                <option value="">All statuses</option>
                <option value="__pending">Pending</option>
                {getStatusOptions().map((status) => (
                  <option key={status} value={status}>
                    {statusLabels[status]}
                  </option>
                ))}
              </select>
              {availableTargets.length > 0 && (
                <select
                  value={filters.targetId}
                  onChange={(e) => handleFilterChange('targetId', e.target.value)}
                  className="filter-select"
                >
                  <option value="">
                    {adminRole === 'STATE' ? 'All districts' : 'All departments'}
                  </option>
                  {availableTargets.map((target) => (
                    <option key={target.id} value={target.id}>
                      {target.name}
                    </option>
                  ))}
                </select>
              )}
            </div>
          )}
        </header>

        {activeView === 'management' ? (
          <ManagementPanel
            adminRole={adminRole}
            districts={adminRole === 'STATE' ? availableTargets : []}
            departments={adminRole === 'DISTRICT' ? availableTargets : []}
          />
        ) : (
          <>
            <section className="stats" aria-label="Issue status counters">
              {Object.entries(counts).map(([status, count]) => (
                <div className="stat" key={status}>
                  <span className="stat-label">{statusLabels[status]}</span>
                  <strong className="stat-count">{count}</strong>
                </div>
              ))}
            </section>

            {error && (
              <div className="alert alert-error">
                <strong>Error:</strong> {error}
              </div>
            )}

            {loading ? (
              <div className="loading-state">
                <div className="spinner"></div>
                <p>Loading issues...</p>
              </div>
            ) : issues.length === 0 ? (
              <div className="empty-state">
                <p>No issues found</p>
              </div>
            ) : (
              <section className="content-grid">
                <IssueTable
                  issues={issues}
                  selectedId={selectedId}
                  onSelectIssue={setSelectedId}
                  statusLabels={statusLabels}
                />

                {selectedIssue && (
                  <IssueDetails
                    issue={selectedIssue}
                    departments={availableTargets}
                    statusLabels={statusLabels}
                    onOpenModal={handleOpenModal}
                    adminRole={adminRole}
                    timeline={timeline}
                    timelineLoading={timelineLoading}
                  />
                )}
              </section>
            )}
          </>
        )}
      </section>

      {modal.type && (
        <ActionModal
          type={modal.type}
          issue={issues.find((i) => i.id === modal.issueId)}
          departments={availableTargets}
          onAction={handleAction}
          onClose={handleCloseModal}
          loading={actingLoading}
          message={actionMessage}
          adminRole={adminRole}
        />
      )}
    </main>
  );
}
