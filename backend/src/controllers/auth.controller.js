import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import nodemailer from 'nodemailer';
import prisma from '../config/prisma.js';
import { generateOtp } from '../utils/otp.js';

const OTP_EXPIRY_MS = 5 * 60 * 1000;

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASS
  }
});

const isEmailValid = (email) =>
  typeof email === 'string' && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

export const sendOtp = async (req, res) => {
  try {
    const email = req.body?.email?.trim()?.toLowerCase();

    if (!isEmailValid(email)) {
      return res.status(400).json({ error: 'Valid email is required' });
    }

    if (!process.env.EMAIL_USER || !process.env.EMAIL_PASS) {
      return res.status(500).json({ error: 'Email service is not configured' });
    }

    const otp = generateOtp();
    const otpHash = await bcrypt.hash(otp, 10);

    const otpRecord = await prisma.otp.create({
      data: {
        email,
        otpHash,
        expiresAt: new Date(Date.now() + OTP_EXPIRY_MS)
      }
    });

    try {
      await transporter.sendMail({
        from: process.env.EMAIL_USER,
        to: email,
        subject: 'Your SevaSetu OTP',
        text: `Your OTP is ${otp}. It will expire in 5 minutes.`
      });
    } catch (mailError) {
      await prisma.otp.delete({ where: { id: otpRecord.id } });
      throw mailError;
    }

    return res.json({ message: 'OTP sent' });
  } catch (error) {
    console.error('sendOtp error:', error);
    return res.status(500).json({ error: 'Failed to send OTP' });
  }
};

export const verifyOtp = async (req, res) => {
  try {
    const email = req.body?.email?.trim()?.toLowerCase();
    const otp = req.body?.otp?.trim();

    if (!isEmailValid(email) || !/^\d{6}$/.test(otp || '')) {
      return res.status(400).json({ error: 'Email and 6-digit OTP are required' });
    }

    if (!process.env.JWT_SECRET) {
      return res.status(500).json({ error: 'JWT secret is not configured' });
    }

    const record = await prisma.otp.findFirst({
      where: { email },
      orderBy: { createdAt: 'desc' }
    });

    if (!record) {
      return res.status(400).json({ error: 'No OTP found' });
    }

    if (record.expiresAt < new Date()) {
      return res.status(400).json({ error: 'OTP expired' });
    }

    const valid = await bcrypt.compare(otp, record.otpHash);

    if (!valid) {
      return res.status(400).json({ error: 'Invalid OTP' });
    }

    let user = await prisma.user.findUnique({ where: { email } });

    if (!user) {
      user = await prisma.user.create({ data: { email } });
    }

    await prisma.otp.deleteMany({ where: { email } });

    const token = jwt.sign(
      { userId: user.id, email: user.email },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRY || '7d' }
    );

    return res.json({ token, user });
  } catch (error) {
    console.error('verifyOtp error:', error);
    return res.status(500).json({ error: 'Failed to verify OTP' });
  }
};
