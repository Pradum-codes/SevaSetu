import prisma from '../config/prisma.js';

const issueSelect = {
  id: true,
  clientId: true,
  userId: true,
  categoryId: true,
  title: true,
  description: true,
  addressText: true,
  landmark: true,
  locality: true,
  lat: true,
  lng: true,
  status: true,
  priority: true,
  departmentId: true,
  jurisdictionId: true,
  createdAt: true,
  updatedAt: true,
  category: {
    select: {
      id: true,
      name: true
    }
  },
  images: {
    select: {
      id: true,
      imageUrl: true,
      createdAt: true
    },
    orderBy: { createdAt: 'asc' }
  },
  _count: {
    select: {
      votes: true
    }
  }
};

const issueTimelineSelect = {
  id: true,
  createdAt: true,
  updates: {
    where: {
      visibleToCitizen: true
    },
    orderBy: [{ createdAt: 'asc' }, { id: 'asc' }],
    select: {
      id: true,
      message: true,
      remarks: true,
      oldStatus: true,
      newStatus: true,
      type: true,
      proofImageUrl: true,
      visibleToCitizen: true,
      createdAt: true,
      actor: {
        select: {
          id: true,
          name: true
        }
      },
      fromDepartment: {
        select: {
          id: true,
          name: true
        }
      },
      toDepartment: {
        select: {
          id: true,
          name: true
        }
      }
    }
  }
};

export const findIssueByClientId = async (clientId) => {
  return prisma.issue.findUnique({
    where: { clientId },
    select: issueSelect
  });
};

export const createIssue = async (data) => {
  return prisma.issue.create({
    data,
    select: issueSelect
  });
};

export const createIssueWithTimeline = async ({ issueData, timelineUpdates }) => {
  return prisma.$transaction(async (tx) => {
    const issue = await tx.issue.create({
      data: issueData,
      select: issueSelect
    });

    for (const update of timelineUpdates || []) {
      await tx.issueUpdate.create({
        data: {
          ...update,
          issueId: issue.id
        }
      });
    }

    return issue;
  });
};

export const findIssues = async ({ where, take, skip, orderBy }) => {
  return prisma.issue.findMany({
    where,
    take,
    skip,
    orderBy,
    select: issueSelect
  });
};

export const countIssues = async ({ where }) => {
  return prisma.issue.count({ where });
};

export const groupIssueCountsByStatus = async ({ where }) => {
  return prisma.issue.groupBy({
    by: ['status'],
    where,
    _count: {
      _all: true
    }
  });
};

export const findJurisdictionById = async (id) => {
  return prisma.jurisdiction.findUnique({
    where: { id },
    select: {
      id: true,
      name: true,
      type: true,
      category: true,
      parentId: true
    }
  });
};

export const findCategoryById = async (id) => {
  return prisma.category.findUnique({
    where: { id },
    select: { id: true, name: true }
  });
};

export const findIssueTimelineById = async (id) => {
  return prisma.issue.findUnique({
    where: { id },
    select: issueTimelineSelect
  });
};

export const findJurisdictionsByParentIds = async (parentIds) => {
  if (!Array.isArray(parentIds) || parentIds.length === 0) return [];
  return prisma.jurisdiction.findMany({
    where: {
      parentId: { in: parentIds }
    },
    select: {
      id: true,
      parentId: true,
      type: true,
      category: true,
      name: true
    }
  });
};

// --- Vote related operations ---

export const findVote = async ({ userId, issueId }) => {
  return prisma.vote.findUnique({
    where: {
      userId_issueId: {
        userId,
        issueId
      }
    }
  });
};

export const createVote = async ({ userId, issueId }) => {
  return prisma.vote.create({
    data: {
      userId,
      issueId
    }
  });
};

export const countVotesByIssue = async (issueId) => {
  return prisma.vote.count({
    where: { issueId }
  });
};

export const findIssueById = async (id) => {
  return prisma.issue.findUnique({
    where: { id },
    select: { id: true } // lightweight existence check
  });
};

export const deleteVote = async ({ userId, issueId }) => {
  return prisma.vote.delete({
    where: {
      userId_issueId: {
        userId,
        issueId
      }
    }
  });
};
