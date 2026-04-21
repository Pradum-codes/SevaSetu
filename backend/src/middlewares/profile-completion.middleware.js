import prisma from '../config/prisma.js';

export default async (req, res, next) => {
  try {
    const userId = req.user?.userId;

    if (!userId) {
      return res.sendStatus(401);
    }

    const user = await prisma.user.findUnique({
      where: { id: userId },
      select: {
        aadhaarNumber: true,
        addressDistrict: true,
        addressAreaType: true,
        addressCityOrPanchayat: true,
        addressText: true
      }
    });

    if (!user) {
      return res.sendStatus(401);
    }

    if (
      !user.aadhaarNumber ||
      !user.addressDistrict ||
      !user.addressAreaType ||
      !user.addressCityOrPanchayat ||
      !user.addressText
    ) {
      return res.status(403).json({
        error:
          'Complete profile with structured address and Aadhaar number before posting an issue'
      });
    }

    return next();
  } catch (error) {
    console.error('profile completion middleware error:', error);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
};
