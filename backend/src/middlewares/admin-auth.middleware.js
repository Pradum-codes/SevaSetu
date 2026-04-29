import jwt from 'jsonwebtoken';
import prisma from '../config/prisma.js';

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

    const admin = await prisma.user.findUnique({
      where: { id: decoded.userId },
      select: {
        id: true,
        email: true,
        name: true,
        isActive: true,
        createdAt: true
      }
    });

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