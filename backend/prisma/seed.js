import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

// Punjab State + 5 Districts (3 Urban, 2 Rural) with their hierarchies
const IDS = {
  // State
  punjabState: '10000000-0000-0000-0000-000000000000',

  // Amritsar (Urban)
  amritsarDistrict: '20000001-0000-0000-0000-000000000000',
  amritsarCity: '20000001-1000-0000-0000-000000000000',
  amritsarZone: '20000001-1100-0000-0000-000000000000',
  amritsarWard: '20000001-1110-0000-0000-000000000000',

  // Ludhiana (Urban)
  ludbianaDistrict: '20000002-0000-0000-0000-000000000000',
  ludhianaCity: '20000002-1000-0000-0000-000000000000',
  ludhianaZone: '20000002-1100-0000-0000-000000000000',
  ludhianaWard: '20000002-1110-0000-0000-000000000000',

  // Jalandhar (Urban)
  jalandharDistrict: '20000003-0000-0000-0000-000000000000',
  jalandharCity: '20000003-1000-0000-0000-000000000000',
  jalandharZone: '20000003-1100-0000-0000-000000000000',
  jalandharWard: '20000003-1110-0000-0000-000000000000',

  // Hoshiarpur (Rural)
  hoshiarpurDistrict: '20000004-0000-0000-0000-000000000000',
  hoshiarpurBlock: '20000004-2000-0000-0000-000000000000',
  hoshiarpurPanchayat: '20000004-2100-0000-0000-000000000000',

  // Sangrur (Rural)
  sangurDistrict: '20000005-0000-0000-0000-000000000000',
  sangurBlock: '20000005-2000-0000-0000-000000000000',
  sangurPanchayat: '20000005-2100-0000-0000-000000000000'
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
  // Create Punjab State
  await upsertJurisdiction({
    id: IDS.punjabState,
    name: 'Punjab',
    type: 'STATE',
    category: 'URBAN'
  });

  // ============ AMRITSAR (URBAN) ============
  await upsertJurisdiction({
    id: IDS.amritsarDistrict,
    name: 'Amritsar District',
    type: 'DISTRICT',
    category: 'URBAN',
    parentId: IDS.punjabState
  });

  await upsertJurisdiction({
    id: IDS.amritsarCity,
    name: 'Amritsar City',
    type: 'CITY',
    category: 'URBAN',
    parentId: IDS.amritsarDistrict
  });

  await upsertJurisdiction({
    id: IDS.amritsarZone,
    name: 'Amritsar Zone 1',
    type: 'ZONE',
    category: 'URBAN',
    parentId: IDS.amritsarCity
  });

  await upsertJurisdiction({
    id: IDS.amritsarWard,
    name: 'Amritsar Ward 5',
    type: 'WARD',
    category: 'URBAN',
    parentId: IDS.amritsarZone
  });

  // ============ LUDHIANA (URBAN) ============
  await upsertJurisdiction({
    id: IDS.ludbianaDistrict,
    name: 'Ludhiana District',
    type: 'DISTRICT',
    category: 'URBAN',
    parentId: IDS.punjabState
  });

  await upsertJurisdiction({
    id: IDS.ludhianaCity,
    name: 'Ludhiana City',
    type: 'CITY',
    category: 'URBAN',
    parentId: IDS.ludbianaDistrict
  });

  await upsertJurisdiction({
    id: IDS.ludhianaZone,
    name: 'Ludhiana Zone 2',
    type: 'ZONE',
    category: 'URBAN',
    parentId: IDS.ludhianaCity
  });

  await upsertJurisdiction({
    id: IDS.ludhianaWard,
    name: 'Ludhiana Ward 8',
    type: 'WARD',
    category: 'URBAN',
    parentId: IDS.ludhianaZone
  });

  // ============ JALANDHAR (URBAN) ============
  await upsertJurisdiction({
    id: IDS.jalandharDistrict,
    name: 'Jalandhar District',
    type: 'DISTRICT',
    category: 'URBAN',
    parentId: IDS.punjabState
  });

  await upsertJurisdiction({
    id: IDS.jalandharCity,
    name: 'Jalandhar City',
    type: 'CITY',
    category: 'URBAN',
    parentId: IDS.jalandharDistrict
  });

  await upsertJurisdiction({
    id: IDS.jalandharZone,
    name: 'Jalandhar Zone 1',
    type: 'ZONE',
    category: 'URBAN',
    parentId: IDS.jalandharCity
  });

  await upsertJurisdiction({
    id: IDS.jalandharWard,
    name: 'Jalandhar Ward 12',
    type: 'WARD',
    category: 'URBAN',
    parentId: IDS.jalandharZone
  });

  // ============ HOSHIARPUR (RURAL) ============
  await upsertJurisdiction({
    id: IDS.hoshiarpurDistrict,
    name: 'Hoshiarpur District',
    type: 'DISTRICT',
    category: 'RURAL',
    parentId: IDS.punjabState
  });

  await upsertJurisdiction({
    id: IDS.hoshiarpurBlock,
    name: 'Dasuya Block',
    type: 'BLOCK',
    category: 'RURAL',
    parentId: IDS.hoshiarpurDistrict
  });

  await upsertJurisdiction({
    id: IDS.hoshiarpurPanchayat,
    name: 'Dasuya Panchayat',
    type: 'PANCHAYAT',
    category: 'RURAL',
    parentId: IDS.hoshiarpurBlock
  });

  // ============ SANGRUR (RURAL) ============
  await upsertJurisdiction({
    id: IDS.sangurDistrict,
    name: 'Sangrur District',
    type: 'DISTRICT',
    category: 'RURAL',
    parentId: IDS.punjabState
  });

  await upsertJurisdiction({
    id: IDS.sangurBlock,
    name: 'Sangrur Block',
    type: 'BLOCK',
    category: 'RURAL',
    parentId: IDS.sangurDistrict
  });

  await upsertJurisdiction({
    id: IDS.sangurPanchayat,
    name: 'Sangrur Panchayat',
    type: 'PANCHAYAT',
    category: 'RURAL',
    parentId: IDS.sangurBlock
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

  console.log('==========================================');
  console.log('Seed completed successfully!');
  console.log('==========================================\n');

  console.log('PUNJAB STATE ID:');
  console.log(`stateId=${IDS.punjabState}\n`);

  console.log('AMRITSAR DISTRICT (URBAN):');
  console.log(`districtId=${IDS.amritsarDistrict}`);
  console.log(`cityId=${IDS.amritsarCity}`);
  console.log(`zoneId=${IDS.amritsarZone}`);
  console.log(`wardId=${IDS.amritsarWard}\n`);

  console.log('LUDHIANA DISTRICT (URBAN):');
  console.log(`districtId=${IDS.ludbianaDistrict}`);
  console.log(`cityId=${IDS.ludhianaCity}`);
  console.log(`zoneId=${IDS.ludhianaZone}`);
  console.log(`wardId=${IDS.ludhianaWard}\n`);

  console.log('JALANDHAR DISTRICT (URBAN):');
  console.log(`districtId=${IDS.jalandharDistrict}`);
  console.log(`cityId=${IDS.jalandharCity}`);
  console.log(`zoneId=${IDS.jalandharZone}`);
  console.log(`wardId=${IDS.jalandharWard}\n`);

  console.log('HOSHIARPUR DISTRICT (RURAL):');
  console.log(`districtId=${IDS.hoshiarpurDistrict}`);
  console.log(`blockId=${IDS.hoshiarpurBlock}`);
  console.log(`panchayatId=${IDS.hoshiarpurPanchayat}\n`);

  console.log('SANGRUR DISTRICT (RURAL):');
  console.log(`districtId=${IDS.sangurDistrict}`);
  console.log(`blockId=${IDS.sangurBlock}`);
  console.log(`panchayatId=${IDS.sangurPanchayat}\n`);

  const categories = await prisma.category.findMany({
    orderBy: { id: 'asc' },
    select: { id: true, name: true }
  });
  console.log('Categories (use id as categoryId in POST /issues):');
  categories.forEach((category) => {
    console.log(`  ${category.id} => ${category.name}`);
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
