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
import {
  stateAdminListIssues,
  stateAdminForwardToDistrict,
  districtAdminListIssues,
  districtAdminAssignToDepartment,
  districtAdminCloseIssue,
  departmentAdminListIssues,
  departmentAdminSubmitProof,
  departmentAdminUpdateStatus
} from '../controllers/roleBasedIssueManagement.controller.js';
import {
  listStaffUsers,
  createStaffUser,
  updateStaffUser,
  deactivateStaffUser,
  getStaffUser
} from '../controllers/staff.controller.js';
import {
  listDepartments,
  createDepartment,
  listDesignations,
  createDesignation
} from '../controllers/department.controller.js';

const router = Router();

// Admin Authentication
router.post('/auth/login', loginAdmin);
router.get('/me', adminAuthMiddleware, getAdminMe);

// Legacy Admin Issue Management (for backward compatibility)
router.get('/issues', adminAuthMiddleware, listAdminIssues);
router.get('/issues/:issueId', adminAuthMiddleware, getAdminIssue);
router.get('/issues/:issueId/timeline', adminAuthMiddleware, getAdminIssueTimeline);
router.patch('/issues/:issueId/assign', adminAuthMiddleware, assignIssue);
router.patch('/issues/:issueId/forward', adminAuthMiddleware, forwardIssue);
router.post('/issues/:issueId/remarks', adminAuthMiddleware, addRemarks);
router.patch('/issues/:issueId/status', adminAuthMiddleware, updateIssueStatus);
router.post('/issues/:issueId/close', adminAuthMiddleware, closeIssue);

// Role-Based Issue Management Routes

// STATE_ADMIN Routes
router.get('/state/issues', adminAuthMiddleware, stateAdminListIssues);
router.patch('/state/issues/:issueId/forward-to-district', adminAuthMiddleware, stateAdminForwardToDistrict);

// DISTRICT_ADMIN Routes
router.get('/district/issues', adminAuthMiddleware, districtAdminListIssues);
router.patch('/district/issues/:issueId/assign-to-department', adminAuthMiddleware, districtAdminAssignToDepartment);
router.post('/district/issues/:issueId/close', adminAuthMiddleware, districtAdminCloseIssue);

// DEPARTMENT_ADMIN Routes
router.get('/department/issues', adminAuthMiddleware, departmentAdminListIssues);
router.patch('/department/issues/:issueId/submit-proof', adminAuthMiddleware, departmentAdminSubmitProof);
router.patch('/department/issues/:issueId/update-status', adminAuthMiddleware, departmentAdminUpdateStatus);

// Staff Management
router.get('/staff', adminAuthMiddleware, listStaffUsers);
router.get('/staff/:id', adminAuthMiddleware, getStaffUser);
router.post('/staff', adminAuthMiddleware, createStaffUser);
router.patch('/staff/:id', adminAuthMiddleware, updateStaffUser);
router.patch('/staff/:id/deactivate', adminAuthMiddleware, deactivateStaffUser);

// Department Management
router.get('/departments', adminAuthMiddleware, listDepartments);
router.post('/departments', adminAuthMiddleware, createDepartment);

// Designation Management
router.get('/designations', adminAuthMiddleware, listDesignations);
router.post('/designations', adminAuthMiddleware, createDesignation);

export default router;