import jwt from 'jsonwebtoken';
import prisma from '../config/prisma.js';

const ADMIN_AUTH_CACHE_TTL_MS = 60 * 1000;
const adminAuthCache = new Map();

export default async (req, res, next) => {
  const token = req.headers.authorization?.split(' ')[1];

  if (!token) {
    return res.sendStatus(401);
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    if (decoded.authType !== 'admin') {
      return res.sendStatus(403);
    }

    const cached = adminAuthCache.get(decoded.userId);
    let admin = cached?.expiresAt > Date.now() ? cached.admin : null;

    if (!admin) {
      admin = await prisma.user.findUnique({
        where: { id: decoded.userId },
        select: {
          id: true,
          email: true,
          name: true,
          isActive: true,
          createdAt: true
        }
      });

      if (admin?.isActive) {
        adminAuthCache.set(decoded.userId, {
          admin,
          expiresAt: Date.now() + ADMIN_AUTH_CACHE_TTL_MS
        });
      } else {
        adminAuthCache.delete(decoded.userId);
      }
    }

    if (!admin || !admin.isActive) {
      return res.sendStatus(401);
    }

    req.user = decoded;
    req.admin = admin;
    return next();
  } catch {
    return res.sendStatus(403);
  }
};
