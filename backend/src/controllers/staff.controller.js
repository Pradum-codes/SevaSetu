import prisma from '../config/prisma.js';

// Helper to check if user is SUPER_ADMIN
const isSuperAdmin = (userRoles = []) => {
  return userRoles.some(ur => ur.role?.name === 'ADMIN');
};

// Helper to get admin's jurisdiction scope
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

// GET /admin/staff - List all staff users
export const listStaffUsers = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    
    // Get admin's authority profile to check scope
    const admin = await prisma.user.findUnique({
      where: { id: adminId },
      include: {
        userRoles: { include: { role: true } },
        authorityProfile: { include: { jurisdiction: true } }
      }
    });

    if (!admin || !admin.authorityProfile) {
      return res.status(403).json({ error: 'Admin profile not found' });
    }

    // Get admin's jurisdiction scope
    const accessibleJurisdictions = await getAdminJurisdictionScope(
      admin.authorityProfile.jurisdictionId
    );

    // List staff with filtering based on scope
    const staff = await prisma.user.findMany({
      where: {
        authorityProfile: {
          jurisdictionId: {
            in: accessibleJurisdictions
          }
        }
      },
      include: {
        authorityProfile: {
          include: {
            department: true,
            designation: true,
            jurisdiction: { select: { id: true, name: true, type: true } }
          }
        },
        userRoles: {
          include: { role: true }
        }
      },
      select: {
        id: true,
        email: true,
        name: true,
        phone: true,
        isActive: true,
        createdAt: true,
        authorityProfile: true,
        userRoles: true
      }
    });

    return res.status(200).json({
      staff,
      scope: {
        jurisdictionCount: accessibleJurisdictions.length
      }
    });
  } catch (error) {
    console.error('Error listing staff:', error);
    return res.status(500).json({ error: 'Failed to list staff' });
  }
};

// POST /admin/staff - Create a new staff member (SUPER_ADMIN only)
export const createStaffUser = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    const { email, name, phone, departmentId, designationId, jurisdictionId, password } = req.body;

    // Validate input
    if (!email || !name || !departmentId || !designationId || !jurisdictionId || !password) {
      return res.status(400).json({
        error: 'email, name, departmentId, designationId, jurisdictionId, and password are required'
      });
    }

    // Check if admin is SUPER_ADMIN
    const admin = await prisma.user.findUnique({
      where: { id: adminId },
      include: {
        userRoles: { include: { role: true } }
      }
    });

    if (!admin || !isSuperAdmin(admin.userRoles)) {
      return res.status(403).json({ error: 'Only SUPER_ADMIN can create staff users' });
    }

    // Check if email already exists
    const existingUser = await prisma.user.findUnique({
      where: { email: email.toLowerCase() }
    });

    if (existingUser) {
      return res.status(400).json({ error: 'Email already exists' });
    }

    // Verify department, designation, and jurisdiction exist
    const [department, designation, jurisdiction] = await Promise.all([
      prisma.department.findUnique({ where: { id: parseInt(departmentId) } }),
      prisma.designation.findUnique({ where: { id: parseInt(designationId) } }),
      prisma.jurisdiction.findUnique({ where: { id: jurisdictionId } })
    ]);

    if (!department || !designation || !jurisdiction) {
      return res.status(400).json({
        error: 'Invalid departmentId, designationId, or jurisdictionId'
      });
    }

    // Hash password
    const bcrypt = await import('bcrypt');
    const passwordHash = await bcrypt.default.hash(password, 10);

    // Create user and authority profile in a transaction
    const newStaff = await prisma.$transaction(async (tx) => {
      const user = await tx.user.create({
        data: {
          email: email.toLowerCase(),
          name,
          phone: phone || null,
          passwordHash,
          isActive: true
        }
      });

      // Create authority profile
      await tx.authorityProfile.create({
        data: {
          userId: user.id,
          departmentId: parseInt(departmentId),
          designationId: parseInt(designationId),
          jurisdictionId,
          isActive: true
        }
      });

      // Assign AUTHORITY role
      const authorityRole = await tx.role.findUnique({
        where: { name: 'AUTHORITY' }
      });

      if (authorityRole) {
        await tx.userRole.create({
          data: {
            userId: user.id,
            roleId: authorityRole.id
          }
        });
      }

      return user;
    });

    return res.status(201).json({
      staff: {
        id: newStaff.id,
        email: newStaff.email,
        name: newStaff.name,
        phone: newStaff.phone,
        isActive: newStaff.isActive,
        createdAt: newStaff.createdAt
      },
      message: 'Staff user created successfully'
    });
  } catch (error) {
    console.error('Error creating staff user:', error);
    return res.status(500).json({ error: 'Failed to create staff user' });
  }
};

// PATCH /admin/staff/:id - Update a staff member
export const updateStaffUser = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    const staffId = req.params.id;
    const { name, phone, departmentId, designationId, jurisdictionId } = req.body;

    // Get admin's authority profile
    const admin = await prisma.user.findUnique({
      where: { id: adminId },
      include: { authorityProfile: true }
    });

    if (!admin || !admin.authorityProfile) {
      return res.status(403).json({ error: 'Admin profile not found' });
    }

    // Get staff member
    const staff = await prisma.user.findUnique({
      where: { id: staffId },
      include: { authorityProfile: true }
    });

    if (!staff || !staff.authorityProfile) {
      return res.status(404).json({ error: 'Staff user not found' });
    }

    // Check if admin has access to staff's jurisdiction
    const accessibleJurisdictions = await getAdminJurisdictionScope(
      admin.authorityProfile.jurisdictionId
    );

    if (!accessibleJurisdictions.includes(staff.authorityProfile.jurisdictionId)) {
      return res.status(403).json({ error: 'Cannot modify staff outside your jurisdiction' });
    }

    // Update staff user
    const updatedStaff = await prisma.$transaction(async (tx) => {
      const user = await tx.user.update({
        where: { id: staffId },
        data: {
          ...(name && { name }),
          ...(phone && { phone })
        }
      });

      // Update authority profile if provided
      if (departmentId || designationId || jurisdictionId) {
        await tx.authorityProfile.update({
          where: { userId: staffId },
          data: {
            ...(departmentId && { departmentId: parseInt(departmentId) }),
            ...(designationId && { designationId: parseInt(designationId) }),
            ...(jurisdictionId && { jurisdictionId })
          }
        });
      }

      return user;
    });

    return res.status(200).json({
      staff: {
        id: updatedStaff.id,
        email: updatedStaff.email,
        name: updatedStaff.name,
        phone: updatedStaff.phone,
        isActive: updatedStaff.isActive
      },
      message: 'Staff user updated successfully'
    });
  } catch (error) {
    console.error('Error updating staff user:', error);
    return res.status(500).json({ error: 'Failed to update staff user' });
  }
};

// PATCH /admin/staff/:id/deactivate - Deactivate a staff member (SUPER_ADMIN only)
export const deactivateStaffUser = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    const staffId = req.params.id;

    // Check if admin is SUPER_ADMIN
    const admin = await prisma.user.findUnique({
      where: { id: adminId },
      include: {
        userRoles: { include: { role: true } }
      }
    });

    if (!admin || !isSuperAdmin(admin.userRoles)) {
      return res.status(403).json({ error: 'Only SUPER_ADMIN can deactivate staff' });
    }

    // Deactivate staff user
    const deactivatedStaff = await prisma.user.update({
      where: { id: staffId },
      data: { isActive: false }
    });

    return res.status(200).json({
      staff: {
        id: deactivatedStaff.id,
        email: deactivatedStaff.email,
        name: deactivatedStaff.name,
        isActive: deactivatedStaff.isActive
      },
      message: 'Staff user deactivated successfully'
    });
  } catch (error) {
    console.error('Error deactivating staff user:', error);
    return res.status(500).json({ error: 'Failed to deactivate staff user' });
  }
};

// GET /admin/staff/:id - Get a specific staff member
export const getStaffUser = async (req, res) => {
  try {
    const staffId = req.params.id;

    const staff = await prisma.user.findUnique({
      where: { id: staffId },
      include: {
        authorityProfile: {
          include: {
            department: true,
            designation: true,
            jurisdiction: { select: { id: true, name: true, type: true } }
          }
        },
        userRoles: {
          include: { role: true }
        }
      },
      select: {
        id: true,
        email: true,
        name: true,
        phone: true,
        isActive: true,
        createdAt: true,
        authorityProfile: true,
        userRoles: true
      }
    });

    if (!staff) {
      return res.status(404).json({ error: 'Staff user not found' });
    }

    return res.status(200).json({ staff });
  } catch (error) {
    console.error('Error fetching staff user:', error);
    return res.status(500).json({ error: 'Failed to fetch staff user' });
  }
};
