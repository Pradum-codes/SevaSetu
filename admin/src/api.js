const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';

// Get stored token from localStorage
export const getToken = () => localStorage.getItem('admin_token');

// Set token in localStorage
export const setToken = (token) => localStorage.setItem('admin_token', token);

// Clear token from localStorage
export const clearToken = () => localStorage.removeItem('admin_token');

// Check if user is authenticated
export const isAuthenticated = () => !!getToken();

// Make API request with auth headers
const apiCall = async (endpoint, options = {}) => {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    // Unauthorized - clear token and let app redirect to login
    clearToken();
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Unknown error' }));
    throw new Error(error.message || `HTTP ${response.status}`);
  }

  return response.json();
};

// Auth API calls
export const adminApi = {
  // Login with email and password
  login: (email, password) =>
    apiCall('/admin/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  // Get current admin profile
  getMe: () => apiCall('/admin/me', { method: 'GET' }),

  // Get issues with filters
  getIssues: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.jurisdictionId) params.append('jurisdictionId', filters.jurisdictionId);
    if (filters.departmentId) params.append('departmentId', filters.departmentId);
    if (filters.status) params.append('status', filters.status);

    const query = params.toString();
    const url = query ? `/admin/issues?${query}` : '/admin/issues';
    return apiCall(url, { method: 'GET' });
  },

  // Get single issue
  getIssue: (issueId) => apiCall(`/admin/issues/${issueId}`, { method: 'GET' }),

  // Get issue timeline
  getIssueTimeline: (issueId) =>
    apiCall(`/admin/issues/${issueId}/timeline`, { method: 'GET' }),

  // Assign issue to department
  assignIssue: (issueId, departmentId, remarks) =>
    apiCall(`/admin/issues/${issueId}/assign`, {
      method: 'PATCH',
      body: JSON.stringify({ departmentId, remarks }),
    }),

  // Forward issue to another department
  forwardIssue: (issueId, departmentId, remarks) =>
    apiCall(`/admin/issues/${issueId}/forward`, {
      method: 'PATCH',
      body: JSON.stringify({ departmentId, remarks }),
    }),

  // Add remarks to issue
  addRemarks: (issueId, remarks) =>
    apiCall(`/admin/issues/${issueId}/remarks`, {
      method: 'POST',
      body: JSON.stringify({ remarks }),
    }),

  // Update issue status
  updateIssueStatus: (issueId, newStatus, remarks) =>
    apiCall(`/admin/issues/${issueId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ newStatus, remarks }),
    }),

  // Close issue with proof
  closeIssue: (issueId, finalRemarks, proofImageUrl) =>
    apiCall(`/admin/issues/${issueId}/close`, {
      method: 'POST',
      body: JSON.stringify({ finalRemarks, proofImageUrl }),
    }),

  // Role-Based Issue Management

  // STATE_ADMIN: List issues and view available districts
  stateAdminListIssues: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.status) params.append('status', filters.status);

    const query = params.toString();
    const url = query ? `/admin/state/issues?${query}` : '/admin/state/issues';
    return apiCall(url, { method: 'GET' });
  },

  // STATE_ADMIN: Forward issue to district
  stateAdminForwardToDistrict: (issueId, toDistrictId, remarks) =>
    apiCall(`/admin/state/issues/${issueId}/forward-to-district`, {
      method: 'PATCH',
      body: JSON.stringify({ toDistrictId, remarks }),
    }),

  // DISTRICT_ADMIN: List issues and view available departments
  districtAdminListIssues: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.status) params.append('status', filters.status);
    if (filters.departmentId) params.append('departmentId', filters.departmentId);

    const query = params.toString();
    const url = query ? `/admin/district/issues?${query}` : '/admin/district/issues';
    return apiCall(url, { method: 'GET' });
  },

  // DISTRICT_ADMIN: Assign issue to department
  districtAdminAssignToDepartment: (issueId, departmentId, remarks) =>
    apiCall(`/admin/district/issues/${issueId}/assign-to-department`, {
      method: 'PATCH',
      body: JSON.stringify({ departmentId, remarks }),
    }),

  // DISTRICT_ADMIN: Close issue after reviewing proof
  districtAdminCloseIssue: (issueId, finalRemarks, proofImageUrl) =>
    apiCall(`/admin/district/issues/${issueId}/close`, {
      method: 'POST',
      body: JSON.stringify({ finalRemarks, proofImageUrl }),
    }),

  // DEPARTMENT_ADMIN: List issues assigned to their department
  departmentAdminListIssues: (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.status) params.append('status', filters.status);

    const query = params.toString();
    const url = query ? `/admin/department/issues?${query}` : '/admin/department/issues';
    return apiCall(url, { method: 'GET' });
  },

  // DEPARTMENT_ADMIN: Submit proof and send back to district
  departmentAdminSubmitProof: (issueId, remarks, proofImageUrl) =>
    apiCall(`/admin/department/issues/${issueId}/submit-proof`, {
      method: 'PATCH',
      body: JSON.stringify({ remarks, proofImageUrl }),
    }),

  // DEPARTMENT_ADMIN: Update issue status
  departmentAdminUpdateStatus: (issueId, newStatus, remarks) =>
    apiCall(`/admin/department/issues/${issueId}/update-status`, {
      method: 'PATCH',
      body: JSON.stringify({ newStatus, remarks }),
    }),
};
