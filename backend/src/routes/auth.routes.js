import { Router } from 'express';
import authMiddleware from '../middlewares/auth.middleware.js';
import {
  sendOtp,
  verifyOtp,
  registerUser,
  completeProfile
} from '../controllers/auth.controller.js';

const router = Router();

router.post('/send-otp', sendOtp);
router.post('/verify-otp', verifyOtp);
router.post('/register/onboarding', registerUser);
router.post('/register/profile', authMiddleware, completeProfile);

export default router;
