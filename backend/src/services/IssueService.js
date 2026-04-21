import {
  createIssue as createIssueRecord,
  findCategoryById,
  findIssueByClientId,
  findIssues,
  findJurisdictionById,
  findJurisdictionsByParentIds
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

const parseOptionalDate = (value) => {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    throw new IssueServiceError('lastSync must be a valid ISO date');
  }
  return date;
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

    return ward.id;
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

    return panchayat.id;
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
  const jurisdictionId = await resolveIssueJurisdiction(payload);

  const existingIssue = await findIssueByClientId(clientId);
  if (existingIssue) {
    if (existingIssue.userId !== userId) {
      throw new IssueServiceError(
        'This clientId already exists for another user. Please regenerate and retry',
        409
      );
    }

    return {
      issue: existingIssue,
      created: false
    };
  }

  const issue = await createIssueRecord({
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
  });

  return {
    issue,
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

  return { issues, page, limit };
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
    new: newIssues,
    updated: updatedIssues,
    deleted: []
  };
};

export const listNearbyIssuesByDistrict = async ({ query }) => {
  const districtId = toOptionalTrimmedString(query?.districtId || query?.district);
  if (!districtId) {
    throw new IssueServiceError('districtId query parameter is required');
  }

  const district = await requireJurisdiction(districtId, 'DISTRICT', 'districtId');
  const jurisdictionIds = await collectDescendantJurisdictionIds(district.id);
  const limit = parsePositiveInt(query?.limit, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
  const page = parsePositiveInt(query?.page, 1, Number.MAX_SAFE_INTEGER);
  const skip = (page - 1) * limit;
  const status = normalizeStatus(query?.status);

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
    district: {
      id: district.id,
      name: district.name,
      category: district.category
    },
    page,
    limit,
    issues
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

  const where = { userId };
  if (status) where.status = status;

  const issues = await findIssues({
    where,
    take: limit,
    skip,
    orderBy: { createdAt: 'desc' }
  });

  return { page, limit, issues };
};
