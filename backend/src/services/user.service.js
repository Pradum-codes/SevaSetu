import prisma from '../config/prisma.js';

class UserServiceError extends Error {
  constructor(message, statusCode = 400, details = null) {
    super(message);
    this.name = 'UserServiceError';
    this.statusCode = statusCode;
    this.details = details;
  }
}

const EDITABLE_FIELDS = new Set([
  'phone',
  'gender',
  'jurisdictionId',
  'districtId',
  'cityId',
  'wardId',
  'blockId',
  'panchayatId',
  'addressDistrict',
  'addressAreaType',
  'addressCityOrPanchayat',
  'addressWard',
  'addressLocality',
  'addressLandmark',
  'addressText',
  'pinCode',
  'addressLat',
  'addressLng',
  'profileImageUrl'
]);

const IMMUTABLE_FIELDS = new Set(['name', 'email', 'idType', 'idNumber', 'aadhaarNumber']);
const isPhoneValid = (phone) => typeof phone === 'string' && /^\d{10}$/.test(phone);
const isAreaTypeValid = (areaType) => areaType === 'URBAN' || areaType === 'RURAL';
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

const toOptionalTrimmedString = (value) => {
  if (value === undefined || value === null) return null;
  const normalized = String(value).trim();
  return normalized.length ? normalized : null;
};

const requireJurisdiction = async (id, expectedType, fieldName) => {
  const record = await prisma.jurisdiction.findUnique({
    where: { id },
    select: { id: true, type: true, parentId: true }
  });
  if (!record) {
    throw new UserServiceError(`${fieldName} is invalid: no matching jurisdiction found`, 400);
  }
  if (record.type !== expectedType) {
    throw new UserServiceError(`${fieldName} must reference a ${expectedType} jurisdiction`, 400);
  }
  return record;
};

const resolveJurisdictionPatch = async (payload) => {
  const areaType = toOptionalTrimmedString(payload?.addressAreaType);
  const districtId = toOptionalTrimmedString(payload?.districtId);
  const cityId = toOptionalTrimmedString(payload?.cityId);
  const wardId = toOptionalTrimmedString(payload?.wardId);
  const blockId = toOptionalTrimmedString(payload?.blockId);
  const panchayatId = toOptionalTrimmedString(payload?.panchayatId);

  const hasHierarchyInput = Boolean(districtId || cityId || wardId || blockId || panchayatId);
  if (!hasHierarchyInput) return null;

  if (!areaType || !isAreaTypeValid(areaType)) {
    throw new UserServiceError('addressAreaType must be URBAN or RURAL when jurisdiction IDs are provided', 400);
  }
  if (!districtId) {
    throw new UserServiceError('districtId is required when jurisdiction IDs are provided', 400);
  }

  const district = await requireJurisdiction(districtId, 'DISTRICT', 'districtId');

  if (areaType === 'URBAN') {
    if (!cityId) throw new UserServiceError('cityId is required for URBAN area updates', 400);
    if (!wardId) throw new UserServiceError('wardId is required for URBAN area updates', 400);
    if (blockId || panchayatId) {
      throw new UserServiceError('blockId and panchayatId are not allowed for URBAN area updates', 400);
    }

    const city = await requireJurisdiction(cityId, 'CITY', 'cityId');
    if (city.parentId !== district.id) {
      throw new UserServiceError('cityId must belong to the provided districtId', 400);
    }

    const ward = await requireJurisdiction(wardId, 'WARD', 'wardId');
    if (ward.parentId !== city.id) {
      throw new UserServiceError('wardId must belong to the provided cityId', 400);
    }

    return {
      addressAreaType: 'URBAN',
      addressDistrict: district.id,
      addressCityOrPanchayat: city.id,
      addressWard: ward.id,
      jurisdictionId: ward.id
    };
  }

  if (!blockId) throw new UserServiceError('blockId is required for RURAL area updates', 400);
  if (!panchayatId) throw new UserServiceError('panchayatId is required for RURAL area updates', 400);
  if (cityId || wardId) {
    throw new UserServiceError('cityId and wardId are not allowed for RURAL area updates', 400);
  }

  const block = await requireJurisdiction(blockId, 'BLOCK', 'blockId');
  if (block.parentId !== district.id) {
    throw new UserServiceError('blockId must belong to the provided districtId', 400);
  }

  const panchayat = await requireJurisdiction(panchayatId, 'PANCHAYAT', 'panchayatId');
  if (panchayat.parentId !== block.id) {
    throw new UserServiceError('panchayatId must belong to the provided blockId', 400);
  }

  return {
    addressAreaType: 'RURAL',
    addressDistrict: district.id,
    addressCityOrPanchayat: panchayat.id,
    addressWard: null,
    jurisdictionId: panchayat.id
  };
};

const getJurisdictionChain = async (jurisdictionId) => {
  if (!jurisdictionId) return [];
  const chain = [];
  let currentId = jurisdictionId;

  while (currentId) {
    const node = await prisma.jurisdiction.findUnique({
      where: { id: currentId },
      select: { id: true, name: true, type: true, parentId: true }
    });
    if (!node) break;
    chain.push(node);
    currentId = node.parentId;
  }

  return chain;
};

const mapUserProfile = async (user) => {
  const chain = await getJurisdictionChain(user?.jurisdictionId);
  const districtNode = chain.find((node) => node.type === 'DISTRICT') || null;
  const selectedJurisdiction = chain[0] || null;

  return {
    id: user.id,
    name: user.name,
    email: user.email,
    phone: user.phone,
    gender: user.gender,
    profileImageUrl: user.profileImageUrl,
    district: districtNode
      ? {
          id: districtNode.id,
          name: districtNode.name
        }
      : null,
    jurisdiction: selectedJurisdiction
      ? {
          id: selectedJurisdiction.id,
          name: selectedJurisdiction.name,
          type: selectedJurisdiction.type
        }
      : null,
    address: {
      addressAreaType: user.addressAreaType,
      addressDistrict: user.addressDistrict,
      addressCityOrPanchayat: user.addressCityOrPanchayat,
      addressWard: user.addressWard,
      addressLocality: user.addressLocality,
      addressLandmark: user.addressLandmark,
      addressText: user.addressText,
      pinCode: user.pinCode,
      addressLat: user.addressLat,
      addressLng: user.addressLng
    },
    jurisdictionIds: {
      districtId: user.addressDistrict || null,
      cityOrPanchayatId: user.addressCityOrPanchayat || null,
      wardId: user.addressWard || null,
      jurisdictionId: user.jurisdictionId || null
    },
    registrationStatus: getRegistrationStatus(user)
  };
};

const validatePatchPayload = (payload) => {
  const errors = [];

  for (const field of Object.keys(payload)) {
    if (IMMUTABLE_FIELDS.has(field)) errors.push(`${field} cannot be updated`);
    if (!EDITABLE_FIELDS.has(field) && !IMMUTABLE_FIELDS.has(field)) {
      errors.push(`${field} is not allowed`);
    }
  }

  if (payload.phone !== undefined && payload.phone !== null) {
    if (!isPhoneValid(String(payload.phone).trim())) {
      errors.push('phone must be a valid 10-digit number');
    }
  }

  if (payload.addressAreaType !== undefined && payload.addressAreaType !== null) {
    if (!isAreaTypeValid(payload.addressAreaType)) {
      errors.push('addressAreaType must be URBAN or RURAL');
    }
  }

  if (payload.addressLat !== undefined && payload.addressLat !== null && !isLatitudeValid(payload.addressLat)) {
    errors.push('addressLat must be between -90 and 90');
  }
  if (payload.addressLng !== undefined && payload.addressLng !== null && !isLongitudeValid(payload.addressLng)) {
    errors.push('addressLng must be between -180 and 180');
  }

  if (payload.addressLat !== undefined && payload.addressLng === undefined) {
    errors.push('addressLng is required when addressLat is provided');
  }
  if (payload.addressLng !== undefined && payload.addressLat === undefined) {
    errors.push('addressLat is required when addressLng is provided');
  }

  const areaType = payload.addressAreaType;
  if (areaType === 'URBAN' && payload.addressCityOrPanchayat !== undefined && payload.addressWard === undefined) {
    errors.push('addressWard is required for URBAN updates when addressCityOrPanchayat is provided');
  }
  if (areaType === 'RURAL' && payload.addressCityOrPanchayat !== undefined) {
    // addressCityOrPanchayat currently stores both city and panchayat variants by design.
  }

  return errors;
};

const prepareUpdateData = (payload) => {
  const update = {};
  const transientHierarchyFields = new Set([
    'districtId',
    'cityId',
    'wardId',
    'blockId',
    'panchayatId'
  ]);

  for (const [key, value] of Object.entries(payload)) {
    if (!EDITABLE_FIELDS.has(key)) continue;
    if (transientHierarchyFields.has(key)) continue;

    if (
      key === 'phone' ||
      key === 'gender' ||
      key === 'addressDistrict' ||
      key === 'addressAreaType' ||
      key === 'addressCityOrPanchayat' ||
      key === 'addressWard' ||
      key === 'addressLocality' ||
      key === 'addressLandmark' ||
      key === 'addressText' ||
      key === 'pinCode' ||
      key === 'jurisdictionId' ||
      key === 'profileImageUrl'
    ) {
      update[key] = toOptionalTrimmedString(value);
      continue;
    }

    update[key] = value;
  }

  return update;
};

export const getCurrentUserProfile = async ({ userId }) => {
  if (!userId) throw new UserServiceError('Authenticated user is required', 401);

  const user = await prisma.user.findUnique({ where: { id: userId } });
  if (!user) throw new UserServiceError('User not found', 404);

  return mapUserProfile(user);
};

export const updateCurrentUserProfile = async ({ userId, payload }) => {
  if (!userId) throw new UserServiceError('Authenticated user is required', 401);

  const input = payload && typeof payload === 'object' ? payload : {};
  const errors = validatePatchPayload(input);
  if (errors.length) {
    throw new UserServiceError('Validation failed', 400, errors);
  }

  if (!Object.keys(input).length) {
    throw new UserServiceError('At least one field is required', 400);
  }

  const updateData = prepareUpdateData(input);
  const jurisdictionPatch = await resolveJurisdictionPatch(input);
  if (jurisdictionPatch) {
    Object.assign(updateData, jurisdictionPatch);
  }

  if (updateData.jurisdictionId && !jurisdictionPatch) {
    const jurisdiction = await prisma.jurisdiction.findUnique({
      where: { id: updateData.jurisdictionId },
      select: { id: true }
    });
    if (!jurisdiction) throw new UserServiceError('Invalid jurisdictionId', 400);
  }

  const updatedUser = await prisma.$transaction(async (tx) => {
    const saved = await tx.user.update({
      where: { id: userId },
      data: updateData
    });

    await tx.userActivityEvent.create({
      data: {
        userId,
        type: 'PROFILE_UPDATED',
        title: 'Profile updated',
        description: 'Your account settings were updated successfully.',
        metadata: {
          updatedFields: Object.keys(updateData)
        }
      }
    });

    return saved;
  });

  return mapUserProfile(updatedUser);
};

export const getCurrentUserActivitySummary = async ({ userId }) => {
  if (!userId) throw new UserServiceError('Authenticated user is required', 401);

  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: { id: true }
  });
  if (!user) throw new UserServiceError('User not found', 404);

  const statusRows = await prisma.issue.groupBy({
    by: ['status'],
    where: { userId },
    _count: { _all: true }
  });

  const summary = {
    total: 0,
    open: 0,
    assigned: 0,
    inProgress: 0,
    resolved: 0,
    closed: 0,
    rejected: 0,
    lastActivityAt: null
  };

  for (const row of statusRows) {
    const status = String(row.status || '').toLowerCase();
    const count = row._count?._all || 0;

    if (status === 'open') summary.open += count;
    else if (status === 'assigned') summary.assigned += count;
    else if (status === 'in_progress') summary.inProgress += count;
    else if (status === 'resolved') summary.resolved += count;
    else if (status === 'closed') summary.closed += count;
    else if (status === 'rejected') summary.rejected += count;
  }

  summary.total =
    summary.open +
    summary.assigned +
    summary.inProgress +
    summary.resolved +
    summary.closed +
    summary.rejected;

  const latestIssue = await prisma.issue.findFirst({
    where: { userId },
    orderBy: { updatedAt: 'desc' },
    select: { updatedAt: true }
  });
  summary.lastActivityAt = latestIssue?.updatedAt?.toISOString() || null;

  return summary;
};

const parsePositiveInt = (value, defaultValue, maxValue) => {
  if (value === undefined || value === null || value === '') return defaultValue;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed < 1) {
    throw new UserServiceError('Query parameter must be a positive integer', 400);
  }
  return Math.min(parsed, maxValue);
};

const mapActivityEventType = (update) => {
  const rawType = String(update?.type || '').trim();
  if (rawType) return rawType;
  if (update?.newStatus && update?.oldStatus && update.newStatus !== update.oldStatus) return 'STATUS_CHANGED';
  return 'UPDATE';
};

const mapIssueUpdateToActivityEvent = (item) => ({
  eventId: `issue-update-${item.id}`,
  eventType: mapActivityEventType(item),
  title: `Issue update: #${item.issueId}`,
  message: item.remarks || item.message || 'Issue status has been updated.',
  issue: {
    id: item.issueId,
    title: item.issue?.title || null,
    currentStatus: item.issue?.status || null
  },
  oldStatus: item.oldStatus || null,
  newStatus: item.newStatus || null,
  proofImageUrl: item.proofImageUrl || null,
  createdAt: item.createdAt
});

const mapIssueCreatedToActivityEvent = (issue) => ({
  eventId: `issue-created-${issue.id}`,
  eventType: 'ISSUE_REPORTED',
  title: `Issue reported: #${issue.id}`,
  message: issue.title || 'A new issue was reported.',
  issue: {
    id: issue.id,
    title: issue.title || null,
    currentStatus: issue.status || null
  },
  oldStatus: null,
  newStatus: 'open',
  proofImageUrl: null,
  createdAt: issue.createdAt
});

const mapUserEventToActivityEvent = (event) => ({
  eventId: `user-event-${event.id}`,
  eventType: event.type,
  title: event.title,
  message: event.description || '',
  issue: null,
  oldStatus: null,
  newStatus: null,
  proofImageUrl: null,
  metadata: event.metadata || null,
  createdAt: event.createdAt
});

export const getCurrentUserActivityFeed = async ({ userId, query }) => {
  if (!userId) throw new UserServiceError('Authenticated user is required', 401);

  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: { id: true }
  });
  if (!user) throw new UserServiceError('User not found', 404);

  const limit = parsePositiveInt(query?.limit, 20, 100);
  const page = parsePositiveInt(query?.page, 1, Number.MAX_SAFE_INTEGER);

  const fetchSize = Math.min(limit * 3, 300);
  const fetchSkip = (page - 1) * limit;

  const [updates, createdIssues, userEvents] = await Promise.all([
    prisma.issueUpdate.findMany({
      where: {
        visibleToCitizen: true,
        issue: {
          userId
        }
      },
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      skip: fetchSkip,
      take: fetchSize,
      select: {
        id: true,
        issueId: true,
        type: true,
        oldStatus: true,
        newStatus: true,
        message: true,
        remarks: true,
        proofImageUrl: true,
        createdAt: true,
        issue: {
          select: {
            title: true,
            status: true
          }
        }
      }
    }),
    prisma.issue.findMany({
      where: { userId },
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      skip: fetchSkip,
      take: fetchSize,
      select: {
        id: true,
        title: true,
        status: true,
        createdAt: true
      }
    }),
    prisma.userActivityEvent.findMany({
      where: { userId },
      orderBy: [{ createdAt: 'desc' }, { id: 'desc' }],
      skip: fetchSkip,
      take: fetchSize,
      select: {
        id: true,
        type: true,
        title: true,
        description: true,
        metadata: true,
        createdAt: true
      }
    })
  ]);

  const timelineEvents = [
    ...updates.map(mapIssueUpdateToActivityEvent),
    ...createdIssues.map(mapIssueCreatedToActivityEvent),
    ...userEvents.map(mapUserEventToActivityEvent)
  ]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, limit);

  return { page, limit, events: timelineEvents };
};

export { UserServiceError };
