import { Router } from 'express';
import authMiddleware from '../middlewares/auth.middleware.js';
import {
  sendOtp,
  verifyOtp,
  registerUser
} from '../controllers/auth.controller.js';

const router = Router();

router.post('/send-otp', sendOtp);
router.post('/verify-otp', verifyOtp);
router.post('/register/onboarding', registerUser);

export default router;
