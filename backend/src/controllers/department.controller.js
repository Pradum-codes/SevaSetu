import prisma from '../config/prisma.js';

// GET /admin/departments - List all departments
export const listDepartments = async (req, res) => {
  try {
    const departments = await prisma.department.findMany({
      select: {
        id: true,
        name: true
      },
      orderBy: { name: 'asc' }
    });

    return res.status(200).json({ departments });
  } catch (error) {
    console.error('Error listing departments:', error);
    return res.status(500).json({ error: 'Failed to list departments' });
  }
};

// POST /admin/departments - Create a new department (SUPER_ADMIN only)
export const createDepartment = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    const { name } = req.body;

    if (!name || typeof name !== 'string' || name.trim().length === 0) {
      return res.status(400).json({ error: 'name is required' });
    }

    // Check if admin is SUPER_ADMIN
    const admin = await prisma.user.findUnique({
      where: { id: adminId },
      include: {
        userRoles: { include: { role: true } }
      }
    });

    const isSuperAdmin = admin?.userRoles?.some(ur => ur.role?.name === 'ADMIN');

    if (!isSuperAdmin) {
      return res.status(403).json({ error: 'Only SUPER_ADMIN can create departments' });
    }

    // Check if department already exists
    const existingDept = await prisma.department.findUnique({
      where: { name: name.trim() }
    });

    if (existingDept) {
      return res.status(400).json({ error: 'Department already exists' });
    }

    const department = await prisma.department.create({
      data: {
        name: name.trim()
      }
    });

    return res.status(201).json({
      department,
      message: 'Department created successfully'
    });
  } catch (error) {
    console.error('Error creating department:', error);
    if (error.code === 'P2002') {
      return res.status(400).json({ error: 'Department name already exists' });
    }
    return res.status(500).json({ error: 'Failed to create department' });
  }
};

// GET /admin/designations - List all designations
export const listDesignations = async (req, res) => {
  try {
    const designations = await prisma.designation.findMany({
      select: {
        id: true,
        name: true
      },
      orderBy: { name: 'asc' }
    });

    return res.status(200).json({ designations });
  } catch (error) {
    console.error('Error listing designations:', error);
    return res.status(500).json({ error: 'Failed to list designations' });
  }
};

// POST /admin/designations - Create a new designation (SUPER_ADMIN only)
export const createDesignation = async (req, res) => {
  try {
    const adminId = req.user?.userId;
    const { name } = req.body;

    if (!name || typeof name !== 'string' || name.trim().length === 0) {
      return res.status(400).json({ error: 'name is required' });
    }

    // Check if admin is SUPER_ADMIN
    const admin = await prisma.user.findUnique({
      where: { id: adminId },
      include: {
        userRoles: { include: { role: true } }
      }
    });

    const isSuperAdmin = admin?.userRoles?.some(ur => ur.role?.name === 'ADMIN');

    if (!isSuperAdmin) {
      return res.status(403).json({ error: 'Only SUPER_ADMIN can create designations' });
    }

    // Check if designation already exists
    const existingDesig = await prisma.designation.findUnique({
      where: { name: name.trim() }
    });

    if (existingDesig) {
      return res.status(400).json({ error: 'Designation already exists' });
    }

    const designation = await prisma.designation.create({
      data: {
        name: name.trim()
      }
    });

    return res.status(201).json({
      designation,
      message: 'Designation created successfully'
    });
  } catch (error) {
    console.error('Error creating designation:', error);
    if (error.code === 'P2002') {
      return res.status(400).json({ error: 'Designation name already exists' });
    }
    return res.status(500).json({ error: 'Failed to create designation' });
  }
};
