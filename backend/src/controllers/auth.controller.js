import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { Resend } from 'resend';
import prisma from '../config/prisma.js';
import { generateOtp } from '../utils/otp.js';

const OTP_EXPIRY_MS = 5 * 60 * 1000;
const DEFAULT_EMAIL_FROM = 'SevaSetu <onboarding@resend.dev>';
const OTP_EXPIRY_MINUTES = OTP_EXPIRY_MS / 60 / 1000;

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
    user?.idNumber &&
      user?.addressDistrict &&
      user?.addressAreaType &&
      user?.addressCityOrPanchayat &&
      user?.addressText
  ),
  locationCaptured: hasCoordinates(user)
});

const buildOtpEmailHtml = ({ otp }) => `
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Your SevaSetu OTP</title>
  </head>
  <body style="margin:0;background:#f3f7f4;font-family:Arial,Helvetica,sans-serif;color:#17231d;">
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background:#f3f7f4;padding:32px 12px;">
      <tr>
        <td align="center">
          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #dce8df;border-radius:24px;overflow:hidden;box-shadow:0 18px 48px rgba(23,35,29,0.12);">
            <tr>
              <td style="background:#0f6b45;padding:28px 28px 24px;text-align:center;">
                <div style="width:76px;height:76px;margin:0 auto 14px;border-radius:20px;border:3px solid rgba(255,255,255,0.72);background:#ffffff;color:#0f6b45;font-size:28px;line-height:76px;font-weight:800;text-align:center;">SS</div>
                <div style="font-size:24px;line-height:32px;font-weight:700;color:#ffffff;">SevaSetu</div>
                <div style="font-size:14px;line-height:22px;color:#d8f4e5;">Secure sign-in verification</div>
              </td>
            </tr>
            <tr>
              <td style="padding:34px 30px 10px;text-align:center;">
                <div style="font-size:16px;line-height:24px;color:#506158;">Use this one-time password to continue signing in.</div>
                <div style="margin:24px auto 18px;display:inline-block;background:#f7fbf8;border:1px solid #cfe3d5;border-radius:18px;padding:18px 24px;">
                  <div style="font-size:38px;line-height:44px;font-weight:800;letter-spacing:8px;color:#0f6b45;font-family:'Courier New',Courier,monospace;">${otp}</div>
                </div>
                <div style="font-size:15px;line-height:23px;color:#506158;">This code expires in <strong style="color:#17231d;">${OTP_EXPIRY_MINUTES} minutes</strong>.</div>
              </td>
            </tr>
            <tr>
              <td style="padding:20px 30px 34px;">
                <div style="background:#fff8e8;border:1px solid #f1d49a;border-radius:16px;padding:14px 16px;font-size:14px;line-height:22px;color:#6b4b10;text-align:center;">
                  Do not share this code with anyone. SevaSetu will never ask for your OTP outside the app.
                </div>
              </td>
            </tr>
            <tr>
              <td style="background:#f8fbf9;border-top:1px solid #e4eee7;padding:18px 28px;text-align:center;font-size:12px;line-height:18px;color:#708078;">
                If you did not request this code, you can safely ignore this email.
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </body>
</html>`;

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

    if (!process.env.RESEND_API_KEY) {
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
      const resend = new Resend(process.env.RESEND_API_KEY);
      const { error: emailError } = await resend.emails.send({
        from: process.env.EMAIL_FROM || DEFAULT_EMAIL_FROM,
        to: email,
        subject: 'Your SevaSetu verification code',
        text: `Your SevaSetu OTP is ${otp}. It will expire in ${OTP_EXPIRY_MINUTES} minutes. Do not share this code with anyone.`,
        html: buildOtpEmailHtml({ otp })
      });
      if (emailError) throw emailError;
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
