import { Router } from 'express';
import authMiddleware from '../middlewares/auth.middleware.js';
import {
  getMe,
  patchMe,
  getMyActivitySummary,
  getMyActivity
} from '../controllers/user.controller.js';

const router = Router();

router.get('/me', authMiddleware, getMe);
router.patch('/me', authMiddleware, patchMe);
router.get('/me/activity-summary', authMiddleware, getMyActivitySummary);
router.get('/me/activity', authMiddleware, getMyActivity);

export default router;
