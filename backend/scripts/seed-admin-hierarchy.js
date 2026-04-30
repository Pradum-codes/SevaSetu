#!/usr/bin/env node
import dotenv from 'dotenv';
dotenv.config();

import bcrypt from 'bcrypt';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

// Jurisdiction IDs from seed.js
const JURISDICTIONS = {
  punjabState: '10000000-0000-0000-0000-000000000000',
  kapurthalaDistrict: '20000006-0000-0000-0000-000000000000',
  kapurthalaCity: '20000006-1000-0000-0000-000000000000',
  kapurthalaWard: '20000006-1110-0000-0000-000000000000',
};

// Designations for different role levels
const DESIGNATIONS = {
  STATE_ADMINISTRATOR: 'State Administrator',
  DISTRICT_ADMINISTRATOR: 'District Administrator',
  DEPARTMENT_HEAD: 'Department Head',
};

const seedDesignations = async () => {
  for (const [key, name] of Object.entries(DESIGNATIONS)) {
    await prisma.designation.upsert({
      where: { name },
      update: {},
      create: { name },
    });
  }
  console.log('✓ Designations created');
};

const seedRoles = async () => {
  // Only ADMIN role needed; all seeded admins will use it
  // Their actual permission level is determined by jurisdiction/department scope
  await prisma.role.upsert({
    where: { name: 'ADMIN' },
    update: {},
    create: { name: 'ADMIN' },
  });
  console.log('✓ ADMIN role verified');
};

const ensureDepartments = async () => {
  // General Administration for state/district heads who don't belong to specific departments
  const generalDept = await prisma.department.upsert({
    where: { name: 'General Administration' },
    update: {},
    create: { name: 'General Administration' },
  });

  const departments = ['Road', 'Water', 'Electricity', 'Sanitation'];
  const deptMap = { 'General Administration': generalDept.id };
  
  for (const name of departments) {
    const dept = await prisma.department.upsert({
      where: { name },
      update: {},
      create: { name },
    });
    deptMap[name] = dept.id;
  }
  console.log('✓ Departments verified');
  return deptMap;
};

const createAdmin = async (email, password, name, jurisdictionId, departmentId, designationName, generalDeptId) => {
  try {
    // Get or find designation and ADMIN role
    const designation = await prisma.designation.findUnique({
      where: { name: designationName },
    });
    if (!designation) {
      throw new Error(`Designation "${designationName}" not found`);
    }

    const role = await prisma.role.findUnique({
      where: { name: 'ADMIN' },
    });
    if (!role) {
      throw new Error('ADMIN role not found');
    }

    // Hash password
    const hash = await bcrypt.hash(password, 10);

    // Upsert user
    const user = await prisma.user.upsert({
      where: { email },
      update: { passwordHash: hash, isActive: true, name },
      create: { email, passwordHash: hash, isActive: true, name },
    });

    // Ensure UserRole exists
    const existingUserRole = await prisma.userRole
      .findUnique({
        where: { userId_roleId: { userId: user.id, roleId: role.id } },
      })
      .catch(() => null);

    if (!existingUserRole) {
      await prisma.userRole.create({
        data: { userId: user.id, roleId: role.id },
      });
    }

    // Use General Administration for state/district heads, specific dept for department heads
    const assignedDeptId = departmentId || generalDeptId;

    // Create or update AuthorityProfile
    const createPayload = {
      user: { connect: { id: user.id } },
      jurisdiction: { connect: { id: jurisdictionId } },
      designation: { connect: { id: designation.id } },
      department: { connect: { id: assignedDeptId } },
    };

    const authorityProfile = await prisma.authorityProfile.upsert({
      where: { userId: user.id },
      update: {
        jurisdictionId,
        departmentId: assignedDeptId,
        designationId: designation.id,
      },
      create: createPayload,
    });

    return { user, authorityProfile, role: 'ADMIN' };
  } catch (error) {
    console.error(`Error creating admin ${email}:`, error.message);
    throw error;
  }
};

const main = async () => {
  console.log('\n==========================================');
  console.log('Seeding Admin Hierarchy for Punjab/Kapurthala');
  console.log('==========================================\n');

  try {
    // Setup designations and roles
    await seedDesignations();
    await seedRoles();
    const departments = await ensureDepartments();

    // 1. STATE HEAD - Punjab State
    console.log('\nCreating State Head...');
    const stateAdmin = await createAdmin(
      'state.admin@punjab.gov.in',
      'StateAdmin@123',
      'Punjab State Administrator',
      JURISDICTIONS.punjabState,
      null, // No specific department for state head
      DESIGNATIONS.STATE_ADMINISTRATOR,
      departments['General Administration']
    );
    console.log(`  ✓ ${stateAdmin.user.email}`);

    // 2. KAPURTHALA DISTRICT HEAD
    console.log('\nCreating District Head...');
    const districtAdmin = await createAdmin(
      'kapurthala.district@gov.in',
      'DistrictAdmin@123',
      'Kapurthala District Administrator',
      JURISDICTIONS.kapurthalaDistrict,
      null, // No specific department for district head
      DESIGNATIONS.DISTRICT_ADMINISTRATOR,
      departments['General Administration']
    );
    console.log(`  ✓ ${districtAdmin.user.email}`);

    // 3. DEPARTMENT HEADS for Kapurthala
    console.log('\nCreating Department Heads for Kapurthala...');
    const departmentHeads = [
      { dept: 'Road', email: 'road.head@kapurthala.gov.in', name: 'Road Department Head' },
      { dept: 'Water', email: 'water.head@kapurthala.gov.in', name: 'Water Department Head' },
      { dept: 'Electricity', email: 'electricity.head@kapurthala.gov.in', name: 'Electricity Department Head' },
      { dept: 'Sanitation', email: 'sanitation.head@kapurthala.gov.in', name: 'Sanitation Department Head' },
    ];

    for (const deptHead of departmentHeads) {
      const admin = await createAdmin(
        deptHead.email,
        'DeptHead@123',
        deptHead.name,
        JURISDICTIONS.kapurthalaDistrict,
        departments[deptHead.dept],
        DESIGNATIONS.DEPARTMENT_HEAD,
        departments['General Administration']
      );
      console.log(`  ✓ ${admin.user.email} (${deptHead.dept} Department)`);
    }

    // 4. Print credentials summary
    console.log('\n==========================================');
    console.log('ADMIN CREDENTIALS CREATED');
    console.log('==========================================\n');

    console.log('STATE HEAD (Punjab):');
    console.log('  Email: state.admin@punjab.gov.in');
    console.log('  Password: StateAdmin@123');
    console.log('  Level: SUPER_ADMIN (State Jurisdiction)');
    console.log('  Access: All Punjab districts and descendants\n');

    console.log('DISTRICT HEAD (Kapurthala):');
    console.log('  Email: kapurthala.district@gov.in');
    console.log('  Password: DistrictAdmin@123');
    console.log('  Level: JURISDICTION_ADMIN (District Jurisdiction)');
    console.log('  Access: Kapurthala city, ward, and descendants\n');

    console.log('DEPARTMENT HEADS (Kapurthala):');
    for (const deptHead of departmentHeads) {
      console.log(`  Email: ${deptHead.email}`);
      console.log(`  Password: DeptHead@123`);
      console.log(`  Level: DEPARTMENT_USER (${deptHead.dept} Department)`);
      console.log(`  Jurisdiction: Kapurthala District`);
      console.log('');
    }

    console.log('==========================================\n');

    process.exit(0);
  } catch (error) {
    console.error('Seed failed:', error);
    process.exit(1);
  } finally {
    await prisma.$disconnect();
  }
};

main();
