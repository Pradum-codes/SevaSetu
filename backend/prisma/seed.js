import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

const IDS = {
  state: '11111111-1111-1111-1111-111111111111',
  urbanDistrict: '22222222-2222-2222-2222-222222222222',
  urbanCity: '33333333-3333-3333-3333-333333333333',
  urbanZone: '44444444-4444-4444-4444-444444444444',
  urbanWard: '55555555-5555-5555-5555-555555555555',
  ruralDistrict: '66666666-6666-6666-6666-666666666666',
  ruralBlock: '77777777-7777-7777-7777-777777777777',
  ruralPanchayat: '88888888-8888-8888-8888-888888888888'
};

const seedRoles = async () => {
  await prisma.role.upsert({
    where: { name: 'CITIZEN' },
    update: {},
    create: { name: 'CITIZEN' }
  });
  await prisma.role.upsert({
    where: { name: 'AUTHORITY' },
    update: {},
    create: { name: 'AUTHORITY' }
  });
  await prisma.role.upsert({
    where: { name: 'ADMIN' },
    update: {},
    create: { name: 'ADMIN' }
  });
};

const upsertJurisdiction = async ({ id, name, type, category, parentId = null, pincode = null }) => {
  return prisma.jurisdiction.upsert({
    where: { id },
    update: { name, type, category, parentId, pincode },
    create: { id, name, type, category, parentId, pincode }
  });
};

const seedJurisdictions = async () => {
  await upsertJurisdiction({
    id: IDS.state,
    name: 'Madhya Pradesh',
    type: 'STATE',
    category: 'URBAN'
  });

  await upsertJurisdiction({
    id: IDS.urbanDistrict,
    name: 'Bhopal District',
    type: 'DISTRICT',
    category: 'URBAN',
    parentId: IDS.state
  });

  await upsertJurisdiction({
    id: IDS.urbanCity,
    name: 'Bhopal City',
    type: 'CITY',
    category: 'URBAN',
    parentId: IDS.urbanDistrict
  });

  await upsertJurisdiction({
    id: IDS.urbanZone,
    name: 'Zone 1',
    type: 'ZONE',
    category: 'URBAN',
    parentId: IDS.urbanCity
  });

  await upsertJurisdiction({
    id: IDS.urbanWard,
    name: 'Ward 12',
    type: 'WARD',
    category: 'URBAN',
    parentId: IDS.urbanZone
  });

  await upsertJurisdiction({
    id: IDS.ruralDistrict,
    name: 'Sehore District',
    type: 'DISTRICT',
    category: 'RURAL',
    parentId: IDS.state
  });

  await upsertJurisdiction({
    id: IDS.ruralBlock,
    name: 'Ichhawar Block',
    type: 'BLOCK',
    category: 'RURAL',
    parentId: IDS.ruralDistrict
  });

  await upsertJurisdiction({
    id: IDS.ruralPanchayat,
    name: 'Bordi Kalan Panchayat',
    type: 'PANCHAYAT',
    category: 'RURAL',
    parentId: IDS.ruralBlock
  });
};

const seedDepartments = async () => {
  const departments = ['Road', 'Water', 'Electricity', 'Sanitation'];
  for (const name of departments) {
    await prisma.department.upsert({
      where: { name },
      update: {},
      create: { name }
    });
  }
};

const seedCategories = async () => {
  const categories = ['Road', 'Garbage', 'Water', 'Electricity', 'Streetlight'];
  for (const name of categories) {
    await prisma.category.upsert({
      where: { name },
      update: {},
      create: { name }
    });
  }
};

const main = async () => {
  await seedRoles();
  await seedJurisdictions();
  await seedDepartments();
  await seedCategories();

  console.log('Seed completed.');
  console.log('Urban chain IDs:');
  console.log(`districtId=${IDS.urbanDistrict}`);
  console.log(`cityId=${IDS.urbanCity}`);
  console.log(`zoneId=${IDS.urbanZone}`);
  console.log(`wardId=${IDS.urbanWard}`);
  console.log('Rural chain IDs:');
  console.log(`districtId=${IDS.ruralDistrict}`);
  console.log(`blockId=${IDS.ruralBlock}`);
  console.log(`panchayatId=${IDS.ruralPanchayat}`);

  const categories = await prisma.category.findMany({
    orderBy: { id: 'asc' },
    select: { id: true, name: true }
  });
  console.log('Categories (use id as categoryId in POST /issues):');
  categories.forEach((category) => {
    console.log(`${category.id} => ${category.name}`);
  });
};

main()
  .catch((error) => {
    console.error('Seed failed:', error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
