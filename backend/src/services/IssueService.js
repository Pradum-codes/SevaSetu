import {
  createIssueWithTimeline,
  findCategoryById,
  findIssueByClientId,
  findIssueTimelineById,
  findIssues,
  findJurisdictionById,
  findJurisdictionsByParentIds,
  findVote,
  createVote,
  deleteVote,
  countVotesByIssue,
  findIssueById
} from '../repositories/IssueRepository.js';

const MAX_PAGE_SIZE = 100;
const DEFAULT_PAGE_SIZE = 50;
const ALLOWED_PRIORITIES = new Set(['low', 'normal', 'medium', 'high']);
const STATUS_MAP = {
  open: 'open',
  in_progress: 'in_progress',
  resolved: 'resolved',
  rejected: 'rejected'
};
const PRIORITY_MAP = {
  low: 'low',
  normal: 'medium',
  medium: 'medium',
  high: 'high'
};

export class IssueServiceError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.name = 'IssueServiceError';
    this.statusCode = statusCode;
  }
}

const TIMELINE_TYPE_MAP = new Set([
  'CREATED',
  'ROUTED_TO_DISTRICT',
  'ASSIGNED',
  'ASSIGNED_TO_DEPARTMENT',
  'FORWARDED',
  'FORWARDED_TO_DISTRICT',
  'REMARK_ADDED',
  'STATUS_CHANGED',
  'RESOLUTION_SUBMITTED',
  'PROOF_SUBMITTED',
  'CLOSED_WITH_PROOF',
  'CLOSED',
  'REJECTED'
]);

const toTrimmedString = (value) => {
  if (value === undefined || value === null) return '';
  return String(value).trim();
};

const toOptionalTrimmedString = (value) => {
  const trimmed = toTrimmedString(value);
  return trimmed || null;
};

const hasValue = (value) => Boolean(toOptionalTrimmedString(value));

const parseCoordinate = (value) => {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value.trim());
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
};

const parsePositiveOptionalInt = (value, fieldName) => {
  if (value === undefined || value === null || value === '') return null;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw new IssueServiceError(`${fieldName} must be a positive integer`);
  }
  return parsed;
};

const normalizeStatus = (value) => {
  if (!value) return null;
  const normalized = String(value).trim().toLowerCase().replace(/\s+/g, '_');
  if (!STATUS_MAP[normalized]) {
    throw new IssueServiceError(
      'status must be one of OPEN, IN_PROGRESS, RESOLVED, REJECTED'
    );
  }
  return STATUS_MAP[normalized];
};

const parseImageUrls = (payload) => {
  const single = toOptionalTrimmedString(payload?.imageUrl || payload?.image_url);
  const arrayInput = Array.isArray(payload?.imageUrls)
    ? payload.imageUrls
    : Array.isArray(payload?.images)
      ? payload.images
      : [];

  const arrayValues = arrayInput
    .map((value) => toOptionalTrimmedString(value))
    .filter(Boolean);

  const urls = [...new Set([single, ...arrayValues].filter(Boolean))];
  if (urls.length === 0) {
    throw new IssueServiceError('At least one image URL is required');
  }

  return urls;
};

const parsePositiveInt = (value, defaultValue, max) => {
  if (value === undefined || value === null || value === '') return defaultValue;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw new IssueServiceError('Query parameter must be a positive integer');
  }
  return Math.min(parsed, max);
};

const parseIssueId = (value) => {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw new IssueServiceError('issueId must be a positive integer');
  }
  return parsed;
};

const parseOptionalDate = (value) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    throw new IssueServiceError('lastSync must be a valid ISO date');
  }
  return date;
};

const parseOptionalFilterDate = (value, fieldName) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    throw new IssueServiceError(`${fieldName} must be a valid ISO date`);
  }
  return date;
};

const mapIssueVoteCount = (issue) => {
  if (!issue) return issue;
  const voteCount = issue?._count?.votes ?? 0;
  const { _count, ...rest } = issue;
  return {
    ...rest,
    voteCount
  };
};

const parseBbox = (bbox) => {
  if (!bbox) return null;
  const rawParts = String(bbox).split(',').map((part) => part.trim());
  if (rawParts.length !== 4) {
    throw new IssueServiceError('bbox must be minLat,minLng,maxLat,maxLng');
  }

  const [minLat, minLng, maxLat, maxLng] = rawParts.map((part) => Number(part));
  if ([minLat, minLng, maxLat, maxLng].some((value) => !Number.isFinite(value))) {
    throw new IssueServiceError('bbox values must be numeric');
  }
  if (minLat < -90 || maxLat > 90 || minLng < -180 || maxLng > 180) {
    throw new IssueServiceError('bbox values are out of valid lat/lng bounds');
  }
  if (minLat > maxLat || minLng > maxLng) {
    throw new IssueServiceError('bbox min values must be less than max values');
  }

  return { minLat, minLng, maxLat, maxLng };
};

const buildRadiusBounds = ({ lat, lng, radiusKm }) => {
  const latDelta = radiusKm / 111;
  const lngDenominator = Math.cos((lat * Math.PI) / 180);
  const safeLngDenominator = Math.max(Math.abs(lngDenominator), 0.01);
  const lngDelta = radiusKm / (111 * safeLngDenominator);

  return {
    minLat: Math.max(-90, lat - latDelta),
    maxLat: Math.min(90, lat + latDelta),
    minLng: Math.max(-180, lng - lngDelta),
    maxLng: Math.min(180, lng + lngDelta)
  };
};

const buildWhereClause = ({ bbox, status }) => {
  const where = {};

  if (bbox) {
    where.lat = { gte: bbox.minLat, lte: bbox.maxLat };
    where.lng = { gte: bbox.minLng, lte: bbox.maxLng };
  }

  if (status) {
    where.status = status;
  }

  return where;
};

const requireJurisdiction = async (id, expectedType, fieldName) => {
  const record = await findJurisdictionById(id);
  if (!record) {
    throw new IssueServiceError(`${fieldName} is invalid: no matching jurisdiction found`);
  }
  if (record.type !== expectedType) {
    throw new IssueServiceError(`${fieldName} must reference a ${expectedType} jurisdiction`);
  }
  return record;
};

const resolveIssueJurisdiction = async (payload) => {
  const districtId = toOptionalTrimmedString(payload?.districtId || payload?.district);
  const cityId = toOptionalTrimmedString(payload?.cityId || payload?.city);
  const wardId = toOptionalTrimmedString(payload?.wardId || payload?.ward);
  const blockId = toOptionalTrimmedString(payload?.blockId || payload?.block);
  const panchayatId = toOptionalTrimmedString(payload?.panchayatId || payload?.panchayat);

  if (!districtId) {
    throw new IssueServiceError('districtId is required');
  }

  const district = await requireJurisdiction(districtId, 'DISTRICT', 'districtId');

  if (district.category === 'URBAN') {
    if (!cityId) throw new IssueServiceError('cityId is required for urban districts');
    if (!wardId) throw new IssueServiceError('wardId is required for urban districts');
    if (hasValue(blockId) || hasValue(panchayatId)) {
      throw new IssueServiceError(
        'blockId and panchayatId are not allowed for urban districts'
      );
    }

    const city = await requireJurisdiction(cityId, 'CITY', 'cityId');
    if (city.parentId !== district.id) {
      throw new IssueServiceError('cityId must belong to the provided districtId');
    }

    const ward = await requireJurisdiction(wardId, 'WARD', 'wardId');
    if (ward.parentId !== city.id) {
      throw new IssueServiceError('wardId must belong to the provided cityId');
    }

    return {
      jurisdictionId: ward.id,
      district: {
        id: district.id,
        name: district.name
      }
    };
  }

  if (district.category === 'RURAL') {
    if (!blockId) throw new IssueServiceError('blockId is required for rural districts');
    if (!panchayatId) {
      throw new IssueServiceError('panchayatId is required for rural districts');
    }
    if (hasValue(cityId) || hasValue(wardId)) {
      throw new IssueServiceError('cityId and wardId are not allowed for rural districts');
    }

    const block = await requireJurisdiction(blockId, 'BLOCK', 'blockId');
    if (block.parentId !== district.id) {
      throw new IssueServiceError('blockId must belong to the provided districtId');
    }

    const panchayat = await requireJurisdiction(panchayatId, 'PANCHAYAT', 'panchayatId');
    if (panchayat.parentId !== block.id) {
      throw new IssueServiceError('panchayatId must belong to the provided blockId');
    }

    return {
      jurisdictionId: panchayat.id,
      district: {
        id: district.id,
        name: district.name
      }
    };
  }

  throw new IssueServiceError('district category is unsupported');
};

const collectDescendantJurisdictionIds = async (rootId) => {
  const visited = new Set([rootId]);
  let frontier = [rootId];

  while (frontier.length > 0) {
    const children = await findJurisdictionsByParentIds(frontier);
    const nextFrontier = [];

    for (const child of children) {
      if (!visited.has(child.id)) {
        visited.add(child.id);
        nextFrontier.push(child.id);
      }
    }

    frontier = nextFrontier;
  }

  return [...visited];
};

const inferTimelineType = (update) => {
  if (update.type && TIMELINE_TYPE_MAP.has(update.type)) {
    return update.type;
  }

  if (update.proofImageUrl) {
    return update.newStatus === 'resolved' ? 'PROOF_SUBMITTED' : 'CLOSED_WITH_PROOF';
  }

  if (update.newStatus === 'open' && update.type === 'ROUTED_TO_DISTRICT') {
    return 'ROUTED_TO_DISTRICT';
  }

  if (update.oldStatus && update.newStatus && update.oldStatus !== update.newStatus) {
    return 'STATUS_CHANGED';
  }

  if (update.newStatus === 'assigned') {
    return 'ASSIGNED_TO_DEPARTMENT';
  }

  if (update.newStatus === 'forwarded') {
    return 'FORWARDED';
  }

  if (update.newStatus === 'resolved') {
    return 'RESOLUTION_SUBMITTED';
  }

  return 'REMARK_ADDED';
};

const mapTimelineUpdate = (update) => ({
  type: inferTimelineType(update),
  remarks: update.remarks || update.message || '',
  fromDepartment: update.fromDepartment?.name || null,
  toDepartment: update.toDepartment?.name || null,
  oldStatus: update.oldStatus || null,
  newStatus: update.newStatus || null,
  proofImageUrl: update.proofImageUrl || null,
  actor: update.actor?.name || null,
  createdAt: update.createdAt
});

export const createIssue = async ({ payload, userId }) => {
  const clientId = toTrimmedString(payload?.clientId || payload?.client_id);
  const categoryId = parsePositiveOptionalInt(payload?.categoryId || payload?.category_id, 'categoryId');
  const title = toTrimmedString(payload?.title);
  const description = toTrimmedString(payload?.description);
  const addressText = toTrimmedString(payload?.addressText || payload?.fullAddress);
  const landmark = toOptionalTrimmedString(payload?.landmark);
  const locality = toOptionalTrimmedString(payload?.locality);
  const lat = parseCoordinate(payload?.lat);
  const lng = parseCoordinate(payload?.lng);
  const priority = toOptionalTrimmedString(payload?.priority)?.toLowerCase() || 'medium';
  const imageUrls = parseImageUrls(payload);

  if (!clientId) throw new IssueServiceError('clientId is required');
  if (!categoryId) throw new IssueServiceError('categoryId is required');
  if (!title) throw new IssueServiceError('title is required');
  if (!description) throw new IssueServiceError('description is required');
  if (!addressText) throw new IssueServiceError('addressText is required');
  const hasLat = lat !== null;
  const hasLng = lng !== null;
  if (!hasLat || !hasLng) {
    throw new IssueServiceError('lat and lng are required');
  }
  if (lat < -90 || lat > 90) {
    throw new IssueServiceError('lat must be a valid latitude between -90 and 90');
  }
  if (lng < -180 || lng > 180) {
    throw new IssueServiceError('lng must be a valid longitude between -180 and 180');
  }
  if (!ALLOWED_PRIORITIES.has(priority)) {
    throw new IssueServiceError('priority must be one of low, normal, medium, high');
  }

  const category = await findCategoryById(categoryId);
  if (!category) {
    throw new IssueServiceError('categoryId is invalid: no matching category found');
  }
  const { jurisdictionId, district } = await resolveIssueJurisdiction(payload);

  const existingIssue = await findIssueByClientId(clientId);
  if (existingIssue) {
    if (existingIssue.userId !== userId) {
      throw new IssueServiceError(
        'This clientId already exists for another user. Please regenerate and retry',
        409
      );
    }

    return {
      issue: mapIssueVoteCount(existingIssue),
      created: false
    };
  }

  const issueData = {
    clientId,
    userId,
    categoryId,
    title,
    description,
    addressText,
    landmark,
    locality,
    imageUrl: imageUrls[0],
    lat,
    lng,
    priority: PRIORITY_MAP[priority],
    departmentId: null,
    jurisdictionId,
    images: {
      create: imageUrls.map((imageUrl) => ({ imageUrl }))
    }
  };

  const issue = await createIssueWithTimeline({
    issueData,
    timelineUpdates: [
      {
        type: 'CREATED',
        oldStatus: null,
        newStatus: 'open',
        remarks: 'Issue reported by citizen.',
        visibleToCitizen: true
      },
      {
        type: 'ROUTED_TO_DISTRICT',
        oldStatus: 'open',
        newStatus: 'open',
        remarks: `Issue routed automatically to ${district.name} based on submitted jurisdiction.`,
        visibleToCitizen: true
      }
    ]
  });

  return {
    issue: mapIssueVoteCount(issue),
    created: true
  };
};

export const listIssues = async ({ query }) => {
  const limit = parsePositiveInt(query?.limit, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
  const page = parsePositiveInt(query?.page, 1, Number.MAX_SAFE_INTEGER);
  const skip = (page - 1) * limit;
  const bbox = parseBbox(query?.bbox);
  const status = normalizeStatus(query?.status);

  const where = buildWhereClause({ bbox, status });
  const issues = await findIssues({
    where,
    take: limit,
    skip,
    orderBy: { createdAt: 'desc' }
  });

  return { issues: issues.map(mapIssueVoteCount), page, limit };
};

export const syncIssues = async ({ query }) => {
  const lastSync = parseOptionalDate(query?.lastSync);
  if (!lastSync) {
    throw new IssueServiceError('lastSync query parameter is required');
  }

  const bbox = parseBbox(query?.bbox);
  const commonWhere = buildWhereClause({ bbox, status: null });

  const newIssues = await findIssues({
    where: {
      ...commonWhere,
      createdAt: { gt: lastSync }
    },
    take: MAX_PAGE_SIZE,
    skip: 0,
    orderBy: { createdAt: 'asc' }
  });

  const updatedIssues = await findIssues({
    where: {
      ...commonWhere,
      updatedAt: { gt: lastSync },
      createdAt: { lte: lastSync }
    },
    take: MAX_PAGE_SIZE,
    skip: 0,
    orderBy: { updatedAt: 'asc' }
  });

  return {
    new: newIssues.map(mapIssueVoteCount),
    updated: updatedIssues.map(mapIssueVoteCount),
    deleted: []
  };
};

export const listNearbyIssuesByDistrict = async ({ query }) => {
  const lat = parseCoordinate(query?.lat);
  const lng = parseCoordinate(query?.lng);
  const hasLat = lat !== null;
  const hasLng = lng !== null;
  const radiusKm = Number(query?.radiusKm ?? query?.radius ?? 5);
  const limit = parsePositiveInt(query?.limit, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
  const page = parsePositiveInt(query?.page, 1, Number.MAX_SAFE_INTEGER);
  const skip = (page - 1) * limit;
  const status = normalizeStatus(query?.status);

  if (hasLat !== hasLng) {
    throw new IssueServiceError('lat and lng must be provided together');
  }

  if (hasLat && hasLng) {
    if (lat < -90 || lat > 90) {
      throw new IssueServiceError('lat must be a valid latitude between -90 and 90');
    }
    if (lng < -180 || lng > 180) {
      throw new IssueServiceError('lng must be a valid longitude between -180 and 180');
    }
    if (!Number.isFinite(radiusKm) || radiusKm <= 0) {
      throw new IssueServiceError('radiusKm must be a positive number');
    }

    const bounds = buildRadiusBounds({ lat, lng, radiusKm });
    const where = {
      lat: { gte: bounds.minLat, lte: bounds.maxLat },
      lng: { gte: bounds.minLng, lte: bounds.maxLng }
    };
    if (status) where.status = status;

    const issues = await findIssues({
      where,
      take: limit,
      skip,
      orderBy: { createdAt: 'desc' }
    });

    return {
      searchMode: 'location',
      location: {
        lat,
        lng,
        radiusKm
      },
      page,
      limit,
      issues: issues.map(mapIssueVoteCount)
    };
  }

  const districtId = toOptionalTrimmedString(query?.districtId || query?.district);
  if (!districtId) {
    throw new IssueServiceError('Provide lat/lng or districtId query parameters');
  }

  const district = await requireJurisdiction(districtId, 'DISTRICT', 'districtId');
  const jurisdictionIds = await collectDescendantJurisdictionIds(district.id);

  const where = {
    jurisdictionId: { in: jurisdictionIds }
  };
  if (status) where.status = status;

  const issues = await findIssues({
    where,
    take: limit,
    skip,
    orderBy: { createdAt: 'desc' }
  });

  return {
    searchMode: 'district',
    district: {
      id: district.id,
      name: district.name,
      category: district.category
    },
    page,
    limit,
    issues: issues.map(mapIssueVoteCount)
  };
};

export const listUserReports = async ({ userId, query }) => {
  if (!userId) {
    throw new IssueServiceError('Authenticated user is required', 401);
  }

  const limit = parsePositiveInt(query?.limit, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
  const page = parsePositiveInt(query?.page, 1, Number.MAX_SAFE_INTEGER);
  const skip = (page - 1) * limit;
  const status = normalizeStatus(query?.status);
  const fromDate = parseOptionalFilterDate(query?.fromDate, 'fromDate');
  const toDate = parseOptionalFilterDate(query?.toDate, 'toDate');

  if (fromDate && toDate && fromDate > toDate) {
    throw new IssueServiceError('fromDate cannot be greater than toDate');
  }

  const where = { userId };
  if (status) where.status = status;
  if (fromDate || toDate) {
    where.createdAt = {};
    if (fromDate) where.createdAt.gte = fromDate;
    if (toDate) where.createdAt.lte = toDate;
  }

  const issues = await findIssues({
    where,
    take: limit,
    skip,
    orderBy: { createdAt: 'desc' }
  });

  return { page, limit, issues: issues.map(mapIssueVoteCount) };
};

export const getIssueTimeline = async ({ issueId }) => {
  const parsedIssueId = parseIssueId(issueId);
  const issue = await findIssueTimelineById(parsedIssueId);

  if (!issue) {
    throw new IssueServiceError('Issue not found', 404);
  }

  const mappedUpdates = issue.updates.map(mapTimelineUpdate);
  const hasStoredCreatedEvent = mappedUpdates.some((update) => update.type === 'CREATED');
  const timeline = hasStoredCreatedEvent
    ? mappedUpdates
    : [
        {
          type: 'CREATED',
          remarks: 'Issue reported by citizen.',
          fromDepartment: null,
          toDepartment: null,
          oldStatus: null,
          newStatus: 'open',
          proofImageUrl: null,
          actor: null,
          createdAt: issue.createdAt
        },
        ...mappedUpdates
      ];

  return {
    issueId: issue.id,
    timeline
  };
};

export const toggleVoteIssue = async ({ issueId, userId }) => {
  const parsedIssueId = parseIssueId(issueId);

  if (!userId) {
    throw new IssueServiceError('Authentication required', 401);
  }

  const issue = await findIssueById(parsedIssueId);
  if (!issue) {
    throw new IssueServiceError('Issue not found', 404);
  }

  const existingVote = await findVote({
    userId,
    issueId: parsedIssueId
  });

  if (existingVote) {
    await deleteVote({
      userId,
      issueId: parsedIssueId
    });

    return {
      voted: false,
      message: 'Vote removed',
      totalVotes: await countVotesByIssue(parsedIssueId)
    };
  }

  await createVote({
    userId,
    issueId: parsedIssueId
  });

  return {
    voted: true,
    message: 'Upvoted successfully',
    totalVotes: await countVotesByIssue(parsedIssueId)
  };
};
