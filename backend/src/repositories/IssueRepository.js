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
