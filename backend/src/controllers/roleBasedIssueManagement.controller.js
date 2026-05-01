import prisma from '../config/prisma.js';

// Helper: Get admin's role
const getAdminRole = async (adminId) => {
  const admin = await prisma.user.findUnique({
    where: { id: adminId },
    include: {
      userRoles: { include: { role: true } },
      authorityProfile: {
        include: {
          jurisdiction: true,
          department: true
        }
      }
    }
  });

  return {
    roles: admin?.userRoles?.map(ur => ur.role?.name) || [],
    jurisdiction: admin?.authorityProfile?.jurisdiction,
    department: admin?.authorityProfile?.department,
    jurisdictionId: admin?.authorityProfile?.jurisdictionId
  };
};

// Helper: Get jurisdiction scope
const getAdminJurisdictionScope = async (jurisdictionId) => {
  const accessibleIds = new Set([jurisdictionId]);
  const queue = [jurisdictionId];

  while (queue.length > 0) {
    const currentId = queue.shift();
    const children = await prisma.jurisdiction.findMany({
      where: { parentId: currentId },
      select: { id: true }
    });

    children.forEach(child => {
      accessibleIds.add(child.id);
      queue.push(child.id);
    });
  }

  return Array.from(accessibleIds);
};

// STATE_ADMIN: View issues and forward to districts
export const stateAdminListIssues = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    const { status, page = 1, limit = 20 } = req.query;

    if (!adminId) return res.sendStatus(401);

    const adminData = await getAdminRole(adminId);
    if (adminData.jurisdiction?.type !== 'STATE') {
      return res.status(403).json({ error: 'STATE_ADMIN access required' });
    }

    const accessibleJurisdictions = await getAdminJurisdictionScope(adminData.jurisdictionId);

    const skip = (page - 1) * limit;
    const where = { jurisdictionId: { in: accessibleJurisdictions } };
    if (status) where.status = status;

    const [issues, total] = await Promise.all([
      prisma.issue.findMany({
        where,
        skip,
        take: parseInt(limit),
        include: {
          user: { select: { id: true, email: true, name: true } },
          jurisdiction: { select: { id: true, name: true, type: true } },
          category: { select: { id: true, name: true } }
        },
        orderBy: { createdAt: 'desc' }
      }),
      prisma.issue.count({ where })
    ]);

    // Get all districts for dropdown
    const districts = await prisma.jurisdiction.findMany({
      where: {
        type: 'DISTRICT',
        parentId: adminData.jurisdictionId
      },
      select: { id: true, name: true }
    });

    return res.json({
      issues,
      availableDistricts: districts,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        totalPages: Math.ceil(total / limit)
      },
      adminRole: 'STATE_ADMIN',
      message: 'State admin can only forward issues to districts'
    });
  } catch (error) {
    console.error('stateAdminListIssues error:', error);
    return res.status(500).json({ error: 'Failed to fetch issues' });
  }
};

// STATE_ADMIN: Forward issue to district (with district jurisdiction update)
export const stateAdminForwardToDistrict = async (req, res) => {
  try {
    const { issueId } = req.params;
    const { toDistrictId, remarks } = req.body;
    const adminId = req.user?.userId;

    if (!adminId) return res.sendStatus(401);

    const parsedIssueId = parseInt(issueId);
    if (isNaN(parsedIssueId)) {
      return res.status(400).json({ error: 'Invalid issue ID' });
    }

    if (!toDistrictId || !remarks || remarks.trim() === '') {
      return res.status(400).json({ error: 'toDistrictId and remarks are required' });
    }

    const adminData = await getAdminRole(adminId);
    if (adminData.jurisdiction?.type !== 'STATE') {
      return res.status(403).json({ error: 'STATE_ADMIN access required' });
    }

    const issue = await prisma.issue.findUnique({
      where: { id: parsedIssueId },
      include: { jurisdiction: true }
    });

    if (!issue) return res.status(404).json({ error: 'Issue not found' });

    // Verify target district exists and is child of state
    const targetDistrict = await prisma.jurisdiction.findUnique({
      where: { id: toDistrictId }
    });

    if (!targetDistrict || targetDistrict.parentId !== adminData.jurisdictionId) {
      return res.status(400).json({ error: 'Invalid target district' });
    }

    const [updatedIssue] = await prisma.$transaction([
      prisma.issue.update({
        where: { id: parsedIssueId },
        data: {
          jurisdictionId: toDistrictId,
          status: 'forwarded'
        }
      }),
      prisma.issueUpdate.create({
        data: {
          issueId: parsedIssueId,
          actorUserId: adminId,
          oldStatus: issue.status,
          newStatus: 'forwarded',
          type: 'FORWARDED_TO_DISTRICT',
          remarks,
          visibleToCitizen: true
        }
      })
    ]);

    return res.json({
      message: 'Issue forwarded to district successfully',
      issue: updatedIssue
    });
  } catch (error) {
    console.error('stateAdminForwardToDistrict error:', error);
    return res.status(500).json({ error: 'Failed to forward issue to district' });
  }
};

// DISTRICT_ADMIN: List issues and departments
export const districtAdminListIssues = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    const { status, departmentId, page = 1, limit = 20 } = req.query;

    if (!adminId) return res.sendStatus(401);

    const adminData = await getAdminRole(adminId);
    if (adminData.jurisdiction?.type !== 'DISTRICT') {
      return res.status(403).json({ error: 'DISTRICT_ADMIN access required' });
    }

    const accessibleJurisdictions = await getAdminJurisdictionScope(adminData.jurisdictionId);

    const skip = (page - 1) * limit;
    const where = { jurisdictionId: { in: accessibleJurisdictions } };
    if (status) where.status = status;
    if (departmentId) where.departmentId = parseInt(departmentId);

    const [issues, total] = await Promise.all([
      prisma.issue.findMany({
        where,
        skip,
        take: parseInt(limit),
        include: {
          user: { select: { id: true, email: true, name: true } },
          department: { select: { id: true, name: true } },
          jurisdiction: { select: { id: true, name: true, type: true } },
          category: { select: { id: true, name: true } }
        },
        orderBy: { createdAt: 'desc' }
      }),
      prisma.issue.count({ where })
    ]);

    // Get departments for dropdown
    const departments = await prisma.department.findMany({
      select: { id: true, name: true },
      orderBy: { name: 'asc' }
    });

    return res.json({
      issues,
      availableDepartments: departments,
      allStatuses: ['open', 'assigned', 'in_progress', 'forwarded', 'resolved', 'closed'],
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        totalPages: Math.ceil(total / limit)
      },
      adminRole: 'DISTRICT_ADMIN',
      message: 'District admin can assign to departments and close after review'
    });
  } catch (error) {
    console.error('districtAdminListIssues error:', error);
    return res.status(500).json({ error: 'Failed to fetch issues' });
  }
};

// DISTRICT_ADMIN: Assign issue to department
export const districtAdminAssignToDepartment = async (req, res) => {
  try {
    const { issueId } = req.params;
    const { departmentId, remarks } = req.body;
    const adminId = req.user?.userId;

    if (!adminId) return res.sendStatus(401);

    const parsedIssueId = parseInt(issueId);
    if (isNaN(parsedIssueId)) {
      return res.status(400).json({ error: 'Invalid issue ID' });
    }

    if (!departmentId || !remarks || remarks.trim() === '') {
      return res.status(400).json({ error: 'departmentId and remarks are required' });
    }

    const adminData = await getAdminRole(adminId);
    if (adminData.jurisdiction?.type !== 'DISTRICT') {
      return res.status(403).json({ error: 'DISTRICT_ADMIN access required' });
    }

    const issue = await prisma.issue.findUnique({
      where: { id: parsedIssueId }
    });

    if (!issue) return res.status(404).json({ error: 'Issue not found' });

    const [updatedIssue] = await prisma.$transaction([
      prisma.issue.update({
        where: { id: parsedIssueId },
        data: {
          departmentId: parseInt(departmentId),
          status: 'assigned'
        }
      }),
      prisma.issueUpdate.create({
        data: {
          issueId: parsedIssueId,
          actorUserId: adminId,
          toDepartmentId: parseInt(departmentId),
          oldStatus: issue.status,
          newStatus: 'assigned',
          type: 'ASSIGNED',
          remarks,
          visibleToCitizen: true
        }
      })
    ]);

    return res.json({
      message: 'Issue assigned to department successfully',
      issue: updatedIssue
    });
  } catch (error) {
    console.error('districtAdminAssignToDepartment error:', error);
    return res.status(500).json({ error: 'Failed to assign issue to department' });
  }
};

// DISTRICT_ADMIN: Close issue after reviewing proof from department
export const districtAdminCloseIssue = async (req, res) => {
  try {
    const { issueId } = req.params;
    const { finalRemarks, proofImageUrl } = req.body;
    const adminId = req.user?.userId;

    if (!adminId) return res.sendStatus(401);

    const parsedIssueId = parseInt(issueId);
    if (isNaN(parsedIssueId)) {
      return res.status(400).json({ error: 'Invalid issue ID' });
    }

    if (!finalRemarks || !proofImageUrl) {
      return res.status(400).json({ error: 'finalRemarks and proofImageUrl are required' });
    }

    const adminData = await getAdminRole(adminId);
    if (adminData.jurisdiction?.type !== 'DISTRICT') {
      return res.status(403).json({ error: 'DISTRICT_ADMIN access required' });
    }

    const issue = await prisma.issue.findUnique({
      where: { id: parsedIssueId }
    });

    if (!issue) return res.status(404).json({ error: 'Issue not found' });

    const [updatedIssue] = await prisma.$transaction([
      prisma.issue.update({
        where: { id: parsedIssueId },
        data: { status: 'closed' }
      }),
      prisma.issueUpdate.create({
        data: {
          issueId: parsedIssueId,
          actorUserId: adminId,
          oldStatus: issue.status,
          newStatus: 'closed',
          type: 'CLOSED_WITH_PROOF',
          remarks: finalRemarks,
          proofImageUrl,
          visibleToCitizen: true
        }
      })
    ]);

    return res.json({
      message: 'Issue closed successfully after review',
      issue: updatedIssue
    });
  } catch (error) {
    console.error('districtAdminCloseIssue error:', error);
    return res.status(500).json({ error: 'Failed to close issue' });
  }
};

// DEPARTMENT_ADMIN: List issues assigned to their department
export const departmentAdminListIssues = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    const { status, page = 1, limit = 20 } = req.query;

    if (!adminId) return res.sendStatus(401);

    const adminData = await getAdminRole(adminId);
    if (!adminData.department) {
      return res.status(403).json({ error: 'DEPARTMENT_ADMIN access required' });
    }

    const skip = (page - 1) * limit;
    const where = {
      departmentId: adminData.department.id
    };
    if (status) where.status = status;

    const [issues, total] = await Promise.all([
      prisma.issue.findMany({
        where,
        skip,
        take: parseInt(limit),
        include: {
          user: { select: { id: true, email: true, name: true } },
          department: { select: { id: true, name: true } },
          jurisdiction: { select: { id: true, name: true, type: true } },
          category: { select: { id: true, name: true } }
        },
        orderBy: { createdAt: 'desc' }
      }),
      prisma.issue.count({ where })
    ]);

    return res.json({
      issues,
      departmentName: adminData.department.name,
      allStatuses: ['assigned', 'in_progress', 'resolved'],
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        totalPages: Math.ceil(total / limit)
      },
      adminRole: 'DEPARTMENT_ADMIN',
      message: 'Department admin can only view assigned issues and provide proof'
    });
  } catch (error) {
    console.error('departmentAdminListIssues error:', error);
    return res.status(500).json({ error: 'Failed to fetch issues' });
  }
};

// DEPARTMENT_ADMIN: Submit proof and send back to district admin
export const departmentAdminSubmitProof = async (req, res) => {
  try {
    const { issueId } = req.params;
    const { remarks, proofImageUrl } = req.body;
    const adminId = req.user?.userId;

    if (!adminId) return res.sendStatus(401);

    const parsedIssueId = parseInt(issueId);
    if (isNaN(parsedIssueId)) {
      return res.status(400).json({ error: 'Invalid issue ID' });
    }

    if (!remarks || !proofImageUrl) {
      return res.status(400).json({ error: 'remarks and proofImageUrl are required' });
    }

    const adminData = await getAdminRole(adminId);
    if (!adminData.department) {
      return res.status(403).json({ error: 'DEPARTMENT_ADMIN access required' });
    }

    const issue = await prisma.issue.findUnique({
      where: { id: parsedIssueId }
    });

    if (!issue) return res.status(404).json({ error: 'Issue not found' });

    if (issue.departmentId !== adminData.department.id) {
      return res.status(403).json({ error: 'Issue not assigned to your department' });
    }

    const [updatedIssue] = await prisma.$transaction([
      prisma.issue.update({
        where: { id: parsedIssueId },
        data: { status: 'resolved' }
      }),
      prisma.issueUpdate.create({
        data: {
          issueId: parsedIssueId,
          actorUserId: adminId,
          oldStatus: issue.status,
          newStatus: 'resolved',
          type: 'PROOF_SUBMITTED',
          remarks,
          proofImageUrl,
          visibleToCitizen: true
        }
      })
    ]);

    return res.json({
      message: 'Proof submitted. Issue sent back to district admin for review and closure',
      issue: updatedIssue
    });
  } catch (error) {
    console.error('departmentAdminSubmitProof error:', error);
    return res.status(500).json({ error: 'Failed to submit proof' });
  }
};

// DEPARTMENT_ADMIN: Update issue status (in_progress, etc)
export const departmentAdminUpdateStatus = async (req, res) => {
  try {
    const { issueId } = req.params;
    const { newStatus, remarks } = req.body;
    const adminId = req.user?.userId;

    if (!adminId) return res.sendStatus(401);

    const parsedIssueId = parseInt(issueId);
    if (isNaN(parsedIssueId)) {
      return res.status(400).json({ error: 'Invalid issue ID' });
    }

    if (!newStatus) {
      return res.status(400).json({ error: 'newStatus is required' });
    }

    const adminData = await getAdminRole(adminId);
    if (!adminData.department) {
      return res.status(403).json({ error: 'DEPARTMENT_ADMIN access required' });
    }

    const issue = await prisma.issue.findUnique({
      where: { id: parsedIssueId }
    });

    if (!issue) return res.status(404).json({ error: 'Issue not found' });

    if (issue.departmentId !== adminData.department.id) {
      return res.status(403).json({ error: 'Issue not assigned to your department' });
    }

    // Department admin can only set to in_progress or resolved
    if (!['in_progress', 'resolved'].includes(newStatus)) {
      return res.status(400).json({ error: 'Invalid status for department admin' });
    }

    const [updatedIssue] = await prisma.$transaction([
      prisma.issue.update({
        where: { id: parsedIssueId },
        data: { status: newStatus }
      }),
      prisma.issueUpdate.create({
        data: {
          issueId: parsedIssueId,
          actorUserId: adminId,
          oldStatus: issue.status,
          newStatus,
          type: 'STATUS_CHANGED',
          remarks: remarks || `Status updated to ${newStatus}`,
          visibleToCitizen: true
        }
      })
    ]);

    return res.json({
      message: 'Issue status updated successfully',
      issue: updatedIssue
    });
  } catch (error) {
    console.error('departmentAdminUpdateStatus error:', error);
    return res.status(500).json({ error: 'Failed to update issue status' });
  }
};
