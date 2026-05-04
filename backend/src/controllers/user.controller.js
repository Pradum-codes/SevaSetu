import {
  getCurrentUserProfile,
  getCurrentUserActivitySummary,
  getCurrentUserActivityFeed,
  updateCurrentUserProfile,
  UserServiceError
} from '../services/user.service.js';

const handleUserError = (error, res) => {
  if (error instanceof UserServiceError) {
    const payload = { error: error.message };
    if (Array.isArray(error.details) && error.details.length) {
      payload.details = error.details;
    }
    return res.status(error.statusCode).json(payload);
  }

  console.error('user controller error:', error);
  return res.status(500).json({ error: 'Internal Server Error' });
};

export const getMe = async (req, res) => {
  try {
    const userId = req.user?.userId;
    if (!userId) return res.sendStatus(401);

    const user = await getCurrentUserProfile({ userId });
    return res.json({ user });
  } catch (error) {
    return handleUserError(error, res);
  }
};

export const patchMe = async (req, res) => {
  try {
    const userId = req.user?.userId;
    if (!userId) return res.sendStatus(401);

    const user = await updateCurrentUserProfile({
      userId,
      payload: req.body
    });
    return res.json({
      message: 'Profile updated successfully',
      user
    });
  } catch (error) {
    return handleUserError(error, res);
  }
};

export const getMyActivitySummary = async (req, res) => {
  try {
    const userId = req.user?.userId;
    if (!userId) return res.sendStatus(401);

    const summary = await getCurrentUserActivitySummary({ userId });
    return res.json({ summary });
  } catch (error) {
    return handleUserError(error, res);
  }
};

export const getMyActivity = async (req, res) => {
  try {
    const userId = req.user?.userId;
    if (!userId) return res.sendStatus(401);

    const payload = await getCurrentUserActivityFeed({
      userId,
      query: req.query
    });
    return res.json(payload);
  } catch (error) {
    return handleUserError(error, res);
  }
};
