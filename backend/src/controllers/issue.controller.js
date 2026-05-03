import {
  createIssue as createIssueService,
  IssueServiceError,
  listNearbyIssuesByDistrict,
  getIssueTimeline as getIssueTimelineService,
  listUserReports,
  listIssues as listIssuesService,
  syncIssues as syncIssuesService,
  toggleVoteIssue as toggleVoteIssueService
} from '../services/IssueService.js';

const handleIssueError = (error, res) => {
  if (error instanceof IssueServiceError) {
    return res.status(error.statusCode).json({ error: error.message });
  }

  console.error('issue controller error:', error);
  return res.status(500).json({ error: 'Internal Server Error' });
};

export const createIssue = async (req, res) => {
  try {
    const userId = req.user?.userId;
    if (!userId) return res.sendStatus(401);

    const { issue, created } = await createIssueService({
      payload: req.body,
      userId
    });

    return res.status(created ? 201 : 200).json({
      message: created ? 'Issue reported successfully' : 'Issue already exists for this clientId',
      idempotent: !created,
      issue
    });
  } catch (error) {
    return handleIssueError(error, res);
  }
};

export const getIssues = async (req, res) => {
  try {
    const { issues, page, limit } = await listIssuesService({ query: req.query });
    return res.json({ issues, page, limit });
  } catch (error) {
    return handleIssueError(error, res);
  }
};

export const getIssueSync = async (req, res) => {
  try {
    const payload = await syncIssuesService({ query: req.query });
    return res.json(payload);
  } catch (error) {
    return handleIssueError(error, res);
  }
};

export const getNearbyIssues = async (req, res) => {
  try {
    const payload = await listNearbyIssuesByDistrict({ query: req.query });
    return res.json(payload);
  } catch (error) {
    return handleIssueError(error, res);
  }
};

export const getIssueTimeline = async (req, res) => {
  try {
    const payload = await getIssueTimelineService({ issueId: req.params.issueId });
    return res.json(payload);
  } catch (error) {
    return handleIssueError(error, res);
  }
};

export const getMyReports = async (req, res) => {
  try {
    const userId = req.user?.userId;
    if (!userId) return res.sendStatus(401);

    const payload = await listUserReports({
      userId,
      query: req.query
    });
    return res.json(payload);
  } catch (error) {
    return handleIssueError(error, res);
  }
};

export const toggleVoteIssue = async (req, res) => {
  try {
    const userId = req.user?.userId;
    if (!userId) {
        console.log('HIT toggleVoteIssue');
        return res.sendStatus(401);
    }

    const payload = await toggleVoteIssueService({
      issueId: req.params.issueId,
      userId
    });

    return res.json(payload);
  } catch (error) {
    return handleIssueError(error, res);
  }
};