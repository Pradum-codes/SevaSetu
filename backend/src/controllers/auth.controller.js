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
    // Extract and validate fields from request
    const name = toTrimmedString(req.body?.name);
    const email = req.body?.email?.trim()?.toLowerCase();
    const phone = toTrimmedString(req.body?.phone);
    const gender = toOptionalTrimmedString(req.body?.gender);
    const idType = toOptionalTrimmedString(req.body?.idType);
    const idNumber = toOptionalTrimmedString(req.body?.idNumber);
    
    // Jurisdiction fields
    const jurisdictionId = toOptionalTrimmedString(req.body?.jurisdictionId);
    const addressDistrict = toOptionalTrimmedString(req.body?.addressDistrict);
    const addressAreaType = toOptionalTrimmedString(req.body?.addressAreaType);
    const addressCity = toOptionalTrimmedString(req.body?.addressCity);
    const addressWard = toOptionalTrimmedString(req.body?.addressWard);
    const addressBlock = toOptionalTrimmedString(req.body?.addressBlock);
    const addressPanchayat = toOptionalTrimmedString(req.body?.addressPanchayat);
    const addressLocality = toOptionalTrimmedString(req.body?.addressLocality);
    const addressLandmark = toOptionalTrimmedString(req.body?.addressLandmark);
    const addressText = toOptionalTrimmedString(req.body?.addressText);
    const pinCode = toOptionalTrimmedString(req.body?.pinCode);

    // Validate required fields
    const fieldErrors = [];
    if (!name) fieldErrors.push('name is required');
    if (!isEmailValid(email)) fieldErrors.push('valid email is required');
    if (!isPhoneValid(phone)) fieldErrors.push('10-digit phone number is required');
    if (!gender) fieldErrors.push('gender is required');
    if (!idType) fieldErrors.push('idType is required');
    if (!idNumber) fieldErrors.push('idNumber is required');
    if (!jurisdictionId) fieldErrors.push('jurisdictionId is required');
    if (!addressDistrict) fieldErrors.push('addressDistrict is required');
    if (!addressAreaType) fieldErrors.push('addressAreaType is required');
    if (!isAreaTypeValid(addressAreaType)) fieldErrors.push('addressAreaType must be URBAN or RURAL');
    if (!addressText) fieldErrors.push('addressText is required');
    if (!pinCode) fieldErrors.push('pinCode is required');

    if (fieldErrors.length > 0) {
      return res.status(400).json({
        error: 'All required fields must be provided',
        details: fieldErrors
      });
    }

    // Validate jurisdiction hierarchy based on area type
    if (addressAreaType === 'URBAN') {
      if (!addressCity) fieldErrors.push('addressCity is required for URBAN area');
      if (!addressWard) fieldErrors.push('addressWard is required for URBAN area');
    } else if (addressAreaType === 'RURAL') {
      if (!addressBlock) fieldErrors.push('addressBlock is required for RURAL area');
      if (!addressPanchayat) fieldErrors.push('addressPanchayat is required for RURAL area');
    }

    if (fieldErrors.length > 0) {
      return res.status(400).json({
        error: 'Invalid jurisdiction hierarchy',
        details: fieldErrors
      });
    }

    // Check if user already exists
    const existingUser = await prisma.user.findUnique({ where: { email } });
    if (existingUser) {
      return res.status(409).json({ error: 'User already exists. Please login' });
    }

    // Create user with all jurisdiction data
    const user = await prisma.user.create({
      data: {
        name,
        email,
        phone,
        gender,
        idType,
        idNumber,
        jurisdictionId,
        addressDistrict,
        addressAreaType: addressAreaType === 'URBAN' ? 'URBAN' : 'RURAL',
        addressWard: addressAreaType === 'URBAN' ? addressWard : null,
        addressCityOrPanchayat: addressAreaType === 'URBAN' ? addressCity : addressPanchayat,
        addressLocality,
        addressLandmark,
        addressText,
        pinCode
      }
    });

    return res.json({
      message: 'Registration completed successfully. Please verify OTP to login',
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        phone: user.phone,
        gender: user.gender,
        idType: user.idType,
        idNumber: user.idNumber,
        addressText: user.addressText,
        addressCityOrPanchayat: user.addressCityOrPanchayat,
        pinCode: user.pinCode
      },
      registrationStatus: getRegistrationStatus(user)
    });
  } catch (error) {
    console.error('registerUser error:', error);
    return res.status(500).json({ error: 'Failed to register user' });
  }
};
