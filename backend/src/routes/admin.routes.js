import { Router } from 'express';
import adminAuthMiddleware from '../middlewares/admin-auth.middleware.js';
import {
  getAdminMe,
  loginAdmin,
  listAdminIssues,
  getAdminIssue,
  getAdminIssueTimeline,
  assignIssue,
  forwardIssue,
  addRemarks,
  updateIssueStatus,
  closeIssue
} from '../controllers/admin.controller.js';

const router = Router();

// Phase 2: Admin Authentication
router.post('/auth/login', loginAdmin);
router.get('/me', adminAuthMiddleware, getAdminMe);

// Phase 3: Admin Issue Management
router.get('/issues', adminAuthMiddleware, listAdminIssues);
router.get('/issues/:issueId', adminAuthMiddleware, getAdminIssue);
router.get('/issues/:issueId/timeline', adminAuthMiddleware, getAdminIssueTimeline);
router.patch('/issues/:issueId/assign', adminAuthMiddleware, assignIssue);
router.patch('/issues/:issueId/forward', adminAuthMiddleware, forwardIssue);
router.post('/issues/:issueId/remarks', adminAuthMiddleware, addRemarks);
router.patch('/issues/:issueId/status', adminAuthMiddleware, updateIssueStatus);
router.post('/issues/:issueId/close', adminAuthMiddleware, closeIssue);

export default router;