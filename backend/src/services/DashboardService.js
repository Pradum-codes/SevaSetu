import {
  countIssues,
  findJurisdictionById,
  findJurisdictionsByParentIds,
  groupIssueCountsByStatus
} from '../repositories/IssueRepository.js';

const DEFAULT_RADIUS_KM = 5;
const DEFAULT_INSIGHT_WINDOW_DAYS = 30;
const UNRESOLVED_STATUSES = ['open', 'in_progress'];

export class DashboardServiceError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.name = 'DashboardServiceError';
    this.statusCode = statusCode;
  }
}

const toTrimmedString = (value) => {
  if (value === undefined || value === null) return '';
  return String(value).trim();
};

const toOptionalTrimmedString = (value) => {
  const valueAsString = toTrimmedString(value);
  return valueAsString || null;
};

const parseCoordinate = (value) => {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value.trim());
    if (Number.isFinite(parsed)) return parsed;
  }
  return null;
};

const parsePositiveOptionalNumber = (value, defaultValue, fieldName) => {
  if (value === undefined || value === null || value === '') return defaultValue;
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    throw new DashboardServiceError(`${fieldName} must be a positive number`);
  }
  return parsed;
};

const parsePositiveOptionalInt = (value, defaultValue, fieldName) => {
  if (value === undefined || value === null || value === '') return defaultValue;
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new DashboardServiceError(`${fieldName} must be a positive integer`);
  }
  return parsed;
};

const assertValidLatLng = ({ lat, lng }) => {
  if (lat < -90 || lat > 90) {
    throw new DashboardServiceError('lat must be a valid latitude between -90 and 90');
  }
  if (lng < -180 || lng > 180) {
    throw new DashboardServiceError('lng must be a valid longitude between -180 and 180');
  }
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

const requireDistrict = async (districtId) => {
  const district = await findJurisdictionById(districtId);
  if (!district) {
    throw new DashboardServiceError('districtId is invalid: no matching jurisdiction found');
  }
  if (district.type !== 'DISTRICT') {
    throw new DashboardServiceError('districtId must reference a DISTRICT jurisdiction');
  }
  return district;
};

const mapStatusCounts = (statusRows) => {
  const snapshot = {
    open: 0,
    inProgress: 0,
    resolved: 0,
    rejected: 0
  };

  for (const row of statusRows) {
    const status = String(row.status || '').toLowerCase();
    const count = row._count?._all || 0;

    if (status === 'open') snapshot.open += count;
    else if (status === 'in_progress') snapshot.inProgress += count;
    else if (status === 'resolved') snapshot.resolved += count;
    else if (status === 'rejected') snapshot.rejected += count;
  }

  return snapshot;
};

const toCoverageText = (mode, radiusKm) => {
  if (mode === 'location') {
    return `within ${radiusKm} km`;
  }
  return 'within district';
};

const buildSearchContext = ({ mode, location, district }) => ({
  mode,
  location: location || { lat: null, lng: null, radiusKm: null },
  district: district || { id: null, name: null, category: null }
});

const resolveDashboardQuery = async (query) => {
  const lat = parseCoordinate(query?.lat);
  const lng = parseCoordinate(query?.lng);
  const hasLat = lat !== null;
  const hasLng = lng !== null;

  if (hasLat !== hasLng) {
    throw new DashboardServiceError('lat and lng must be provided together');
  }

  if (hasLat && hasLng) {
    assertValidLatLng({ lat, lng });
    const radiusKm = parsePositiveOptionalNumber(
      query?.radiusKm,
      DEFAULT_RADIUS_KM,
      'radiusKm'
    );

    const districtId = toOptionalTrimmedString(query?.districtId);
    const district = districtId ? await requireDistrict(districtId) : null;
    const bounds = buildRadiusBounds({ lat, lng, radiusKm });

    return {
      mode: 'location',
      district,
      radiusKm,
      location: { lat, lng, radiusKm },
      nearbyWhere: {
        lat: { gte: bounds.minLat, lte: bounds.maxLat },
        lng: { gte: bounds.minLng, lte: bounds.maxLng }
      }
    };
  }

  const districtId = toOptionalTrimmedString(query?.districtId);
  if (!districtId) {
    throw new DashboardServiceError('districtId is required when lat/lng are missing');
  }

  const district = await requireDistrict(districtId);
  const jurisdictionIds = await collectDescendantJurisdictionIds(district.id);

  return {
    mode: 'district',
    district,
    radiusKm: null,
    location: null,
    nearbyWhere: {
      jurisdictionId: { in: jurisdictionIds }
    }
  };
};

export const getDashboardData = async ({ userId, query }) => {
  if (!userId) {
    throw new DashboardServiceError('Authenticated user is required', 401);
  }

  const insightWindowDays = parsePositiveOptionalInt(
    query?.insightWindowDays,
    DEFAULT_INSIGHT_WINDOW_DAYS,
    'insightWindowDays'
  );

  const context = await resolveDashboardQuery(query);
  const insightStartAt = new Date(Date.now() - insightWindowDays * 24 * 60 * 60 * 1000);

  const [myReportStatusRows, nearbyStatusRows, nearbyTotal, nearbyHighPriority, insightStatusRows] =
    await Promise.all([
      groupIssueCountsByStatus({
        where: {
          userId
        }
      }),
      groupIssueCountsByStatus({
        where: context.nearbyWhere
      }),
      countIssues({
        where: context.nearbyWhere
      }),
      countIssues({
        where: {
          ...context.nearbyWhere,
          priority: 'high',
          status: { in: UNRESOLVED_STATUSES }
        }
      }),
      groupIssueCountsByStatus({
        where: {
          ...context.nearbyWhere,
          createdAt: { gte: insightStartAt }
        }
      })
    ]);

  const myReportsSnapshot = mapStatusCounts(myReportStatusRows);
  const myReportsTotal =
    myReportsSnapshot.open +
    myReportsSnapshot.inProgress +
    myReportsSnapshot.resolved +
    myReportsSnapshot.rejected;

  const nearbyCounts = mapStatusCounts(nearbyStatusRows);
  const insightCounts = mapStatusCounts(insightStatusRows);
  const unresolved = myReportsSnapshot.open + myReportsSnapshot.inProgress;

  return {
    searchContext: buildSearchContext({
      mode: context.mode,
      location: context.location,
      district: context.district
        ? {
            id: context.district.id,
            name: context.district.name,
            category: context.district.category
          }
        : null
    }),
    myReportsSnapshot: {
      open: myReportsSnapshot.open,
      inProgress: myReportsSnapshot.inProgress,
      resolved: myReportsSnapshot.resolved,
      rejected: myReportsSnapshot.rejected,
      total: myReportsTotal
    },
    myPendingAction: {
      unresolved,
      cta: {
        type: 'OPEN_REPORTS',
        filters: ['OPEN', 'IN_PROGRESS'],
        label: 'Open Reports'
      }
    },
    nearbyRiskSummary: {
      highPriority: nearbyHighPriority,
      open: nearbyCounts.open,
      totalNearby: nearbyTotal,
      coverageText: toCoverageText(context.mode, context.radiusKm)
    },
    nearbyInsights: {
      open: insightCounts.open,
      inProgress: insightCounts.inProgress,
      closed: insightCounts.resolved + insightCounts.rejected
    },
    generatedAt: new Date().toISOString()
  };
};
