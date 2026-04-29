import { Router } from 'express';
import adminAuthMiddleware from '../middlewares/admin-auth.middleware.js';
import { getAdminMe, loginAdmin } from '../controllers/admin.controller.js';

const router = Router();

router.post('/auth/login', loginAdmin);
router.get('/me', adminAuthMiddleware, getAdminMe);

export default router;