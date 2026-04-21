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
const isPhoneValid = (phone) => typeof phone === 'string' && /^\d{10}$/.test(phone);
const isAadhaarValid = (aadhaar) => typeof aadhaar === 'string' && /^\d{12}$/.test(aadhaar);
const toTrimmedString = (value) => {
  if (value === undefined || value === null) return '';
  return String(value).trim();
};
const toOptionalTrimmedString = (value) => {
  const result = toTrimmedString(value);
  return result || null;
};
const isAreaTypeValid = (areaType) => areaType === 'URBAN' || areaType === 'RURAL';
const parseCoordinate = (value) => {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value.trim());
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
};
const isLatitudeValid = (lat) => typeof lat === 'number' && lat >= -90 && lat <= 90;
const isLongitudeValid = (lng) => typeof lng === 'number' && lng >= -180 && lng <= 180;
const hasCoordinates = (user) =>
  typeof user?.addressLat === 'number' && typeof user?.addressLng === 'number';
const getRegistrationStatus = (user) => ({
  onboardingCompleted: Boolean(user?.name && user?.email && user?.phone),
  profileCompleted: Boolean(
    user?.aadhaarNumber &&
      user?.addressDistrict &&
      user?.addressAreaType &&
      user?.addressCityOrPanchayat &&
      user?.addressText
  ),
  locationCaptured: hasCoordinates(user)
});

export const sendOtp = async (req, res) => {
  try {
    const email = req.body?.email?.trim()?.toLowerCase();

    if (!isEmailValid(email)) {
      return res.status(400).json({ error: 'Valid email is required' });
    }

    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) {
      return res.status(404).json({ error: 'User not found. Please register first' });
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

    const user = await prisma.user.findUnique({ where: { email } });
    if (!user) {
      return res.status(404).json({ error: 'User not found. Please register first' });
    }

    await prisma.otp.deleteMany({ where: { email } });

    const token = jwt.sign(
      { userId: user.id, email: user.email },
      process.env.JWT_SECRET,
      { expiresIn: process.env.JWT_EXPIRY || '7d' }
    );

    return res.json({
      token,
      user,
      registrationStatus: getRegistrationStatus(user)
    });
  } catch (error) {
    console.error('verifyOtp error:', error);
    return res.status(500).json({ error: 'Failed to verify OTP' });
  }
};

export const registerUser = async (req, res) => {
  try {
    const name = req.body?.name?.trim();
    const email = req.body?.email?.trim()?.toLowerCase();
    const phone = req.body?.phone?.trim();

    if (!name || !isEmailValid(email) || !isPhoneValid(phone)) {
      return res.status(400).json({
        error: 'Valid name, email, and 10-digit phone are required'
      });
    }

    const existingUser = await prisma.user.findUnique({ where: { email } });
    if (existingUser) {
      return res.status(409).json({ error: 'User already exists. Please login with OTP' });
    }

    const user = await prisma.user.create({
      data: { name, email, phone }
    });

    return res.json({
      message: 'Registration completed successfully. Please verify OTP to login',
      user,
      registrationStatus: getRegistrationStatus(user)
    });
  } catch (error) {
    console.error('registerUser error:', error);
    return res.status(500).json({ error: 'Failed to register user' });
  }
};

export const completeOnboarding = async (req, res) => {
  try {
    const name = req.body?.name?.trim();
    const email = req.body?.email?.trim()?.toLowerCase();
    const phone = req.body?.phone?.trim();

    if (!name || !isEmailValid(email) || !isPhoneValid(phone)) {
      return res.status(400).json({
        error: 'Valid name, email, and 10-digit phone are required'
      });
    }

    if (req.user?.email !== email) {
      return res.status(403).json({
        error: 'Authenticated user email does not match request email'
      });
    }

    const user = await prisma.user.update({
      where: { id: req.user.userId },
      data: { name, email, phone }
    });

    return res.json({
      message: 'Onboarding completed successfully',
      user,
      registrationStatus: getRegistrationStatus(user)
    });
  } catch (error) {
    console.error('completeOnboarding error:', error);
    return res.status(500).json({ error: 'Failed to complete onboarding' });
  }
};

export const completeProfile = async (req, res) => {
  try {
    const addressDistrict = toTrimmedString(req.body?.district);
    const addressAreaType = toTrimmedString(req.body?.areaType).toUpperCase();
    const addressCityOrPanchayat = toTrimmedString(req.body?.cityOrPanchayat);
    const addressWard = toOptionalTrimmedString(req.body?.ward);
    const addressLocality = toOptionalTrimmedString(req.body?.locality);
    const addressLandmark = toOptionalTrimmedString(req.body?.landmark);
    const addressText = toTrimmedString(req.body?.fullAddress);
    const aadhaarNumber = toTrimmedString(req.body?.aadhaarNumber);
    const addressLat = parseCoordinate(req.body?.latitude);
    const addressLng = parseCoordinate(req.body?.longitude);
    const jurisdictionId = toOptionalTrimmedString(req.body?.jurisdictionId);
    const hasLatitudeInput = req.body?.latitude !== undefined && req.body?.latitude !== null;
    const hasLongitudeInput = req.body?.longitude !== undefined && req.body?.longitude !== null;
    const hasAnyCoordinateInput = hasLatitudeInput || hasLongitudeInput;
    const hasBothCoordinateInput = hasLatitudeInput && hasLongitudeInput;

    if (hasAnyCoordinateInput && !hasBothCoordinateInput) {
      return res.status(400).json({
        error: 'Send both latitude and longitude together, or omit both'
      });
    }

    if (hasBothCoordinateInput && (!isLatitudeValid(addressLat) || !isLongitudeValid(addressLng))) {
      return res.status(400).json({
        error: 'Latitude/longitude are invalid'
      });
    }

    const fieldErrors = [];
    if (!addressDistrict) fieldErrors.push('district is required');
    if (!isAreaTypeValid(addressAreaType)) fieldErrors.push('areaType must be URBAN or RURAL');
    if (!addressCityOrPanchayat) fieldErrors.push('cityOrPanchayat is required');
    if (!addressText) fieldErrors.push('fullAddress is required');
    if (!isAadhaarValid(aadhaarNumber)) {
      fieldErrors.push('aadhaarNumber must be a 12-digit string/number');
    }

    if (fieldErrors.length > 0) {
      return res.status(400).json({
        error:
          'District, areaType(URBAN/RURAL), cityOrPanchayat, fullAddress, and valid 12-digit Aadhaar number are required',
        details: fieldErrors
      });
    }

    const user = await prisma.user.update({
      where: { id: req.user.userId },
      data: {
        aadhaarNumber,
        jurisdictionId,
        addressDistrict,
        addressAreaType,
        addressCityOrPanchayat,
        addressWard,
        addressLocality,
        addressLandmark,
        addressText,
        addressLat: hasBothCoordinateInput ? addressLat : null,
        addressLng: hasBothCoordinateInput ? addressLng : null
      }
    });

    return res.json({
      message: 'Profile completed successfully',
      user,
      registrationStatus: getRegistrationStatus(user)
    });
  } catch (error) {
    console.error('completeProfile error:', error);
    return res.status(500).json({ error: 'Failed to complete profile' });
  }
};
