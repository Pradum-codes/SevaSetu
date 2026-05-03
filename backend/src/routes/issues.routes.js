import { Router } from 'express';
import authMiddleware from '../middlewares/auth.middleware.js';
import profileCompletionMiddleware from '../middlewares/profile-completion.middleware.js';
import {
  createIssue,
  getIssueSync,
  getIssueTimeline,
  getIssues,
  getMyReports,
  getNearbyIssues,
  toggleVoteIssue
} from '../controllers/issue.controller.js';

const router = Router();

router.get('/nearby', getNearbyIssues);
router.get('/reports', authMiddleware, getMyReports);
router.post('/:issueId/vote', authMiddleware, toggleVoteIssue);
router.get('/:issueId/timeline', getIssueTimeline);
router.get('/', getIssues);
router.get('/sync', authMiddleware, getIssueSync);
router.post('/', authMiddleware, profileCompletionMiddleware, createIssue);

export default router;
