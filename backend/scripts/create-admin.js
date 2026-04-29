#!/usr/bin/env node
import dotenv from 'dotenv';
dotenv.config();

import bcrypt from 'bcrypt';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

function parseArgs() {
  const args = process.argv.slice(2);
  const out = {};
  for (let i = 0; i < args.length; i++) {
    const a = args[i];
    if (a === '--email' && args[i+1]) { out.email = args[++i]; }
    else if (a === '--password' && args[i+1]) { out.password = args[++i]; }
    else if (a === '--name' && args[i+1]) { out.name = args[++i]; }
    else if (a === '--inactive') { out.isActive = false; }
  }
  return out;
}

const main = async () => {
  const { email, password, name, isActive = true } = parseArgs();

  if (!email || !password) {
    console.error('Usage: node scripts/create-admin.js --email admin@example.com --password secret [--name "Admin Name"] [--inactive]');
    process.exit(1);
  }

  try {
    const adminRole = await prisma.role.findUnique({ where: { name: 'ADMIN' } });
    if (!adminRole) {
      console.log('Admin role not found; creating role ADMIN');
      await prisma.role.create({ data: { name: 'ADMIN' } });
    }

    const hash = await bcrypt.hash(password, 10);

    const user = await prisma.user.upsert({
      where: { email },
      update: { passwordHash: hash, isActive, name },
      create: { email, passwordHash: hash, isActive, name }
    });

    const role = await prisma.role.findUnique({ where: { name: 'ADMIN' } });
    if (!role) throw new Error('failed to ensure admin role');

    // ensure UserRole exists
    const existingUserRole = await prisma.userRole.findUnique({ where: { userId_roleId: { userId: user.id, roleId: role.id } } }).catch(() => null);
    if (!existingUserRole) {
      await prisma.userRole.create({ data: { userId: user.id, roleId: role.id } });
    }

    console.log('Admin user created/updated: ', { id: user.id, email: user.email, name: user.name });
    process.exit(0);
  } catch (err) {
    console.error('Error creating admin:', err);
    process.exit(1);
  } finally {
    await prisma.$disconnect();
  }
};

main();
