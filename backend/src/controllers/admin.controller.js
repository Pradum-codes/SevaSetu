import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import prisma from '../config/prisma.js';

const toTrimmedString = (value) => {
  if (value === undefined || value === null) return '';
  return String(value).trim();
};

const toNormalizedEmail = (value) => toTrimmedString(value).toLowerCase();

const buildAdminToken = (admin) => {
  return jwt.sign(
    {
      userId: admin.id,
      email: admin.email,
      authType: 'admin'
    },
    process.env.JWT_SECRET,
    { expiresIn: process.env.JWT_EXPIRY || '7d' }
  );
};

const sanitizeAdmin = (admin) => ({
  id: admin.id,
  email: admin.email,
  name: admin.name,
  isActive: admin.isActive,
  createdAt: admin.createdAt
});

export const loginAdmin = async (req, res) => {
  try {
    const email = toNormalizedEmail(req.body?.email);
    const password = toTrimmedString(req.body?.password);

    if (!email || !password) {
      return res.status(400).json({ error: 'email and password are required' });
    }

    if (!process.env.JWT_SECRET) {
      return res.status(500).json({ error: 'JWT secret is not configured' });
    }

    const admin = await prisma.user.findUnique({
      where: { email },
      select: {
        id: true,
        email: true,
        name: true,
        passwordHash: true,
        isActive: true,
        createdAt: true
      }
    });

    if (!admin || !admin.isActive || !admin.passwordHash) {
      return res.status(401).json({ error: 'Invalid admin credentials' });
    }

    const passwordMatches = await bcrypt.compare(password, admin.passwordHash);
    if (!passwordMatches) {
      return res.status(401).json({ error: 'Invalid admin credentials' });
    }

    const token = buildAdminToken(admin);

    return res.json({
      token,
      admin: sanitizeAdmin(admin)
    });
  } catch (error) {
    console.error('loginAdmin error:', error);
    return res.status(500).json({ error: 'Failed to sign in admin' });
  }
};

export const getAdminMe = async (req, res) => {
  try {
    const admin = req.admin;
    if (!admin) {
      return res.sendStatus(401);
    }

    return res.json({
      admin: sanitizeAdmin(admin),
      auth: {
        type: 'admin'
      }
    });
  } catch (error) {
    console.error('getAdminMe error:', error);
    return res.status(500).json({ error: 'Failed to load admin profile' });
  }
};