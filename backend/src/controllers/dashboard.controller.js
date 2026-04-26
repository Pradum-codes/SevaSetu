import {
  DashboardServiceError,
  getDashboardData
} from '../services/DashboardService.js';

export const getDashboard = async (req, res) => {
  try {
    const userId = req.user?.userId;
    if (!userId) {
      return res.sendStatus(401);
    }

    const payload = await getDashboardData({
      userId,
      query: req.query
    });

    return res.json(payload);
  } catch (error) {
    if (error instanceof DashboardServiceError) {
      return res.status(error.statusCode).json({ message: error.message });
    }

    console.error('dashboard controller error:', error);
    return res.status(500).json({ message: 'Internal Server Error' });
  }
};
