# SevaSetu Admin Portal - Phase 4

## Overview

The Admin Portal is a React + Vite application for managing civic issues with authentication, jurisdiction-based filtering, and real-time issue tracking.

**Status**: Phase 4 MVP - Authentication & Component Structure Complete

## Architecture

### Components

- **`LoginPage.jsx`** - Admin login form with email/password authentication
- **`Dashboard.jsx`** - Main admin interface with issues, filters, and details panel
- **`main.jsx`** - App root with authentication state management
- **`api.js`** - Centralized API client with automatic JWT handling

### Authentication Flow

1. User enters email/password on login form
2. Credentials sent to `POST /admin/auth/login`
3. Backend validates and returns JWT token + admin profile
4. Token stored in localStorage for persistence
5. All subsequent API requests include `Authorization: Bearer <token>` header
6. Failed requests (401) automatically clear token and redirect to login

### API Integration

All backend endpoints from Phase 3 are integrated:

```javascript
// Authentication
adminApi.login(email, password)          // Login
adminApi.getMe()                         // Get admin profile

// Issues
adminApi.getIssues(filters)              // List issues
adminApi.getIssue(issueId)               // Get single issue
adminApi.getIssueTimeline(issueId)       // Get issue timeline

// Actions
adminApi.assignIssue(issueId, deptId, remarks)
adminApi.forwardIssue(issueId, deptId, remarks)
adminApi.addRemarks(issueId, remarks)
adminApi.updateIssueStatus(issueId, status, remarks)
adminApi.closeIssue(issueId, remarks, proof)
```

## Getting Started

### Prerequisites

- Node.js 18+
- Backend server running on http://localhost:3000

### Installation

```bash
# Install dependencies
npm install

# Copy environment template
cp .env.example .env
```

### Development

```bash
# Start dev server with hot reload
npm run dev

# Server runs on http://localhost:5174
# API requests proxied to http://localhost:3000/admin
```

### Production Build

```bash
# Build optimized bundle
npm run build

# Preview production build locally
npm run preview
```

## Configuration

### Environment Variables

**`.env` file:**
```
VITE_API_URL=http://localhost:3000
```

When using Vite dev server, the proxy configuration in `vite.config.js` automatically routes API calls.

### Vite Proxy Configuration

Configured in `vite.config.js`:
- Routes `/admin/*` requests to backend
- Enables cookies for session management
- Only active during development

## File Structure

```
/admin
├── src/
│   ├── main.jsx           # App entry point with auth routing
│   ├── LoginPage.jsx      # Login form component
│   ├── Dashboard.jsx      # Main admin interface
│   ├── api.js             # API client with JWT handling
│   ├── styles.css         # All component styles
│   └── index.html         # HTML template
├── vite.config.js         # Vite configuration with proxy
├── package.json           # Dependencies
├── .env                   # Environment variables (git-ignored)
├── .env.example           # Environment template
└── README.md              # This file
```

## User Flow

### Login Page
```
1. User visits http://localhost:5174
2. LoginPage renders with email/password form
3. User enters credentials and clicks "Sign In"
4. API validates and returns JWT token + admin profile
5. Token stored in localStorage
6. Redirected to Dashboard
```

### Dashboard
```
1. Shows admin's accessible issues based on jurisdiction
2. Status counters at top (Open, Assigned, In Progress, etc.)
3. Filters for jurisdiction and department
4. Issue table with click to select
5. Selected issue details panel on right
6. Timeline of all issue movements
7. Action buttons: Assign, Forward, Add Remarks, Close With Proof
8. Logout button in sidebar footer
```

## Testing

### Login Credentials (from backend seeding)

```
Email: admin@example.com
Password: admin123
```

### Test Scenarios

1. **Invalid credentials** → Error banner displays
2. **Network error** → Connection error shows
3. **Expired token** → Auto-redirects to login
4. **Cross-district access** → API returns 403, shows error
5. **Issue filters** → Jurisdiction/department filters work
6. **Logout** → Token cleared, redirects to login

## Next Steps

### Phase 4 Continuation
- [ ] Implement action buttons (assign, forward, remarks, close)
- [ ] Add modal/drawer for action forms
- [ ] Real-time issue updates
- [ ] Export functionality
- [ ] Notification system

### Phase 5: Staff Management
- [ ] Staff list and creation UI
- [ ] Department management
- [ ] Role assignment interface
- [ ] Jurisdiction hierarchy visualization

## Known Limitations

- Mock data for jurisdictions/departments (should be fetched from API)
- Action buttons are placeholders (not yet connected to API)
- No pagination for large issue lists
- No offline support
- No image upload for proof images yet

## Troubleshooting

### "Cannot find module" errors
```bash
# Reinstall dependencies
rm -rf node_modules
npm install
```

### Port 5174 already in use
```bash
# Change port in vite.config.js
server: {
  port: 5175  // Change to available port
}
```

### API request failures
1. Verify backend is running: `curl http://localhost:3000/health`
2. Check `.env` file VITE_API_URL matches backend server
3. Verify CORS is enabled on backend
4. Check browser console for detailed error messages

### Token not persisting
- Check if localStorage is enabled in browser
- Verify browser isn't in private/incognito mode
- Check DevTools → Application → LocalStorage for admin_token

## Security Notes

- JWT tokens stored in localStorage (XSS vulnerable - consider using httpOnly cookies with backend)
- Passwords never stored or logged
- All API calls require valid token
- CORS configured in backend

## Related Documentation

- [Backend API Docs](../backend/docs/ADMIN_API.md)
- [Admin Portal Plan](../backend/docs/ADMIN_PORTAL_IMPLEMENTATION_PLAN.md)
- [Database Schema](../backend/docs/database-schema.md)
