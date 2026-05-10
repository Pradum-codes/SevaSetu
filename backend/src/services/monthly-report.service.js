import crypto from 'crypto';
import PDFDocument from 'pdfkit';
import prisma from '../config/prisma.js';
import { createMonthlyReportNotification } from './notification.service.js';

const monthKey = (date) => `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}`;

const resolveMonthRange = (month) => {
  if (month) {
    const [year, mon] = String(month).split('-').map(Number);
    if (!year || !mon || mon < 1 || mon > 12) {
      const error = new Error('month must be YYYY-MM');
      error.statusCode = 400;
      throw error;
    }
    const start = new Date(Date.UTC(year, mon - 1, 1, 0, 0, 0));
    const end = new Date(Date.UTC(year, mon, 1, 0, 0, 0));
    return { start, end, month: `${year}-${String(mon).padStart(2, '0')}` };
  }

  const now = new Date();
  const start = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth() - 1, 1, 0, 0, 0));
  const end = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1, 0, 0, 0));
  return { start, end, month: monthKey(start) };
};

const buildPdfBuffer = ({ user, month, reportData }) => {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument({ size: 'A4', margin: 40 });
    const chunks = [];
    const pageWidth = doc.page.width - doc.page.margins.left - doc.page.margins.right;
    const brand = '#0B6B4F';
    const muted = '#5B6B63';
    const border = '#D9E7DF';

    doc.on('data', (chunk) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', reject);

    const drawKpi = (x, y, label, value) => {
      const cardW = (pageWidth - 24) / 3;
      const cardH = 64;
      doc.roundedRect(x, y, cardW, cardH, 8).fillAndStroke('#F5FAF7', border);
      doc.fillColor(muted).fontSize(9).text(label, x + 10, y + 10, { width: cardW - 20 });
      doc.fillColor('#102019').font('Helvetica-Bold').fontSize(16).text(String(value), x + 10, y + 30, { width: cardW - 20 });
      doc.font('Helvetica');
    };

    const drawBar = (label, count, maxCount, y, color = brand) => {
      const barX = doc.page.margins.left + 140;
      const barW = pageWidth - 210;
      const fillW = maxCount > 0 ? Math.max((count / maxCount) * barW, 2) : 0;
      doc.fillColor('#1E2B24').fontSize(10).text(label, doc.page.margins.left, y + 1, { width: 130 });
      doc.roundedRect(barX, y, barW, 12, 4).fill('#EFF5F1');
      if (fillW > 0) doc.roundedRect(barX, y, fillW, 12, 4).fill(color);
      doc.fillColor('#1E2B24').fontSize(10).text(String(count), barX + barW + 6, y + 1, { width: 40 });
    };

    const issueCategoryCounts = reportData.appendix.reduce((acc, issue) => {
      const key = issue.category?.name || 'Uncategorized';
      acc[key] = (acc[key] || 0) + 1;
      return acc;
    }, {});
    const topCategories = Object.entries(issueCategoryCounts).sort((a, b) => b[1] - a[1]).slice(0, 5);

    doc.roundedRect(doc.page.margins.left, 36, pageWidth, 84, 10).fill('#F0F8F4');
    doc.fillColor(brand).font('Helvetica-Bold').fontSize(22).text('SevaSetu Monthly Summary', doc.page.margins.left + 16, 52);
    doc.fillColor(muted).font('Helvetica').fontSize(10).text(`Scope: ${user.name || user.email}`, doc.page.margins.left + 16, 84);
    doc.fillColor(muted).font('Helvetica').fontSize(10).text(`Jurisdiction: ${user.jurisdiction?.name || 'N/A'}`, doc.page.margins.left + 16, 98);
    doc.fillColor(muted).font('Helvetica').fontSize(10).text(`Month: ${month} | Generated: ${new Date().toISOString().slice(0, 10)}`, doc.page.margins.left + 280, 98);

    let y = 140;
    drawKpi(doc.page.margins.left, y, 'Total Issues', reportData.totalIssues);
    drawKpi(doc.page.margins.left + (pageWidth - 24) / 3 + 12, y, 'Resolved', reportData.resolvedThisMonth);
    drawKpi(doc.page.margins.left + 2 * ((pageWidth - 24) / 3 + 12), y, 'Pending', reportData.pendingIssues);
    y += 84;
    drawKpi(doc.page.margins.left, y, 'Avg Resolution (hrs)', reportData.avgResolutionHours ?? 'N/A');
    drawKpi(doc.page.margins.left + (pageWidth - 24) / 3 + 12, y, 'Most Reported Category', reportData.mostReportedCategory || 'N/A');
    drawKpi(doc.page.margins.left + 2 * ((pageWidth - 24) / 3 + 12), y, 'Recent Updates', reportData.recentUpdates.length);

    y += 96;
    doc.fillColor('#102019').font('Helvetica-Bold').fontSize(13).text('Status Breakdown', doc.page.margins.left, y);
    y += 20;
    const statusEntries = Object.entries(reportData.statusCounts);
    const statusMax = Math.max(...statusEntries.map(([, c]) => c), 1);
    statusEntries.forEach(([status, count], idx) => {
      drawBar(status.replaceAll('_', ' '), count, statusMax, y + idx * 18, '#0B6B4F');
    });
    y += statusEntries.length * 18 + 18;

    doc.fillColor('#102019').font('Helvetica-Bold').fontSize(13).text('Category Breakdown (Top 5)', doc.page.margins.left, y);
    y += 20;
    const catMax = Math.max(...topCategories.map(([, c]) => c), 1);
    topCategories.forEach(([name, count], idx) => {
      drawBar(name, count, catMax, y + idx * 18, '#2C8C66');
    });
    y += topCategories.length * 18 + 18;

    doc.fillColor('#102019').font('Helvetica-Bold').fontSize(13).text('Top 5 Recent Updates', doc.page.margins.left, y);
    y += 18;
    doc.font('Helvetica').fontSize(10).fillColor('#22322B');
    if (!reportData.recentUpdates.length) {
      doc.text('No updates in this month.', doc.page.margins.left, y);
      y += 20;
    } else {
      reportData.recentUpdates.forEach((u) => {
        const line = `#${u.issueId}  ${u.message}  (${u.createdAt.toISOString().slice(0, 10)})`;
        doc.text(line, doc.page.margins.left, y, { width: pageWidth });
        y += 16;
      });
    }

    doc.addPage();
    doc.fillColor('#102019').font('Helvetica-Bold').fontSize(14).text('Appendix - Issue Register');
    doc.moveDown(0.5);
    doc.font('Helvetica').fontSize(9).fillColor('#2A3A33');
    reportData.appendix.slice(0, 150).forEach((issue, idx) => {
      const line = `${idx + 1}. #${issue.id} | ${issue.title.slice(0, 40)} | Created: ${issue.createdAt.toISOString().slice(0, 10)} | Status: ${issue.status} | Updated: ${issue.updatedAt.toISOString().slice(0, 10)}`;
      doc.text(line, { width: pageWidth });
      if (doc.y > doc.page.height - 60) {
        doc.addPage();
      }
    });

    doc.end();
  });
};

const uploadUnsignedWithPreset = async ({ buffer, publicId, cloudName, uploadPreset }) => {
  const formData = new FormData();
  formData.append('file', new Blob([buffer], { type: 'application/pdf' }), `${publicId}.pdf`);
  formData.append('upload_preset', uploadPreset);
  formData.append('resource_type', 'image');
  formData.append('public_id', publicId);

  const response = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/image/upload`, {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Cloudinary unsigned upload failed: ${response.status} ${text}`);
  }

  const payload = await response.json();
  return payload?.secure_url || payload?.url;
};

const uploadSignedWithApiSecret = async ({ buffer, publicId, cloudName, apiKey, apiSecret }) => {
  const timestamp = Math.floor(Date.now() / 1000);
  const paramsToSign = `public_id=${publicId}&timestamp=${timestamp}`;
  const signature = crypto.createHash('sha1').update(`${paramsToSign}${apiSecret}`).digest('hex');

  const formData = new FormData();
  formData.append('file', new Blob([buffer], { type: 'application/pdf' }), `${publicId}.pdf`);
  formData.append('resource_type', 'image');
  formData.append('public_id', publicId);
  formData.append('api_key', apiKey);
  formData.append('timestamp', String(timestamp));
  formData.append('signature', signature);

  const response = await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/image/upload`, {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Cloudinary signed upload failed: ${response.status} ${text}`);
  }

  const payload = await response.json();
  return payload?.secure_url || payload?.url;
};

const uploadPdfToCloudinary = async ({ buffer, publicId }) => {
  const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
  const uploadPreset = process.env.CLOUDINARY_UPLOAD_PRESET;
  const apiKey = process.env.CLOUDINARY_API_KEY;
  const apiSecret = process.env.CLOUDINARY_API_SECRET;

  if (!cloudName) {
    throw new Error('Missing CLOUDINARY_CLOUD_NAME for monthly report upload');
  }

  if (uploadPreset) {
    return uploadUnsignedWithPreset({ buffer, publicId, cloudName, uploadPreset });
  }

  if (apiKey && apiSecret) {
    return uploadSignedWithApiSecret({ buffer, publicId, cloudName, apiKey, apiSecret });
  }

  throw new Error('Cloudinary upload is not configured. Set CLOUDINARY_UPLOAD_PRESET or CLOUDINARY_API_KEY/CLOUDINARY_API_SECRET');
};

const toJpgPreviewUrl = ({ cloudName, publicId, version }) => {
  if (!cloudName || !publicId || !version) return null;
  return `https://res.cloudinary.com/${cloudName}/image/upload/pg_1,f_jpg/v${version}/${publicId}.jpg`;
};

const aggregateMonthForScope = async ({ start, end }) => {
  const issues = await prisma.issue.findMany({
    where: { createdAt: { lt: end } },
    select: {
      id: true,
      title: true,
      status: true,
      createdAt: true,
      updatedAt: true,
      category: { select: { name: true } },
      user: { select: { email: true, name: true } }
    }
  });

  const updates = await prisma.issueUpdate.findMany({
    where: { createdAt: { gte: start, lt: end } },
    orderBy: { createdAt: 'desc' },
    take: 5,
    select: { issueId: true, message: true, remarks: true, createdAt: true }
  });

  const totalIssues = issues.length;
  const resolvedThisMonth = issues.filter((i) => ['resolved', 'closed'].includes(i.status)).length;
  const pendingIssues = issues.filter((i) => !['resolved', 'closed'].includes(i.status)).length;

  const categoryCounts = issues.reduce((acc, issue) => {
    const key = issue.category?.name || 'Uncategorized';
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});

  const mostReportedCategory = Object.entries(categoryCounts).sort((a, b) => b[1] - a[1])[0]?.[0] || null;

  const resolvedDurations = issues
    .filter((i) => ['resolved', 'closed'].includes(i.status))
    .map((i) => (i.updatedAt.getTime() - i.createdAt.getTime()) / (1000 * 60 * 60))
    .filter((h) => h >= 0);

  const avgResolutionHours = resolvedDurations.length
    ? Number((resolvedDurations.reduce((a, b) => a + b, 0) / resolvedDurations.length).toFixed(2))
    : null;

  return {
    totalIssues,
    resolvedThisMonth,
    pendingIssues,
    mostReportedCategory,
    avgResolutionHours,
    statusCounts: issues.reduce((acc, issue) => {
      acc[issue.status] = (acc[issue.status] || 0) + 1;
      return acc;
    }, {}),
    recentUpdates: updates.map((u) => ({
      issueId: u.issueId,
      message: u.remarks || u.message || 'Issue updated',
      createdAt: u.createdAt
    })),
    appendix: issues,
    scopeLabel: 'All Active Users'
  };
};

export const generateAndSendMonthlySummary = async ({ month }) => {
  const range = resolveMonthRange(month);
  const runStamp = Date.now();
  console.log('monthly-summary:start', {
    requestedMonth: month || null,
    resolvedMonth: range.month,
    start: range.start.toISOString(),
    end: range.end.toISOString()
  });

  const users = await prisma.user.findMany({
    where: {
      isActive: true
    },
    select: {
      id: true,
      name: true,
      email: true,
      jurisdiction: { select: { id: true, name: true, type: true } }
    }
  });
  console.log('monthly-summary:eligible-users', {
    month: range.month,
    totalEligibleUsers: users.length
  });

  let generatedCount = 0;
  let sentCount = 0;
  let failedCount = 0;

  let reportUrl = null;
  let uploadedRawUrl = null;
  let reportData = null;
  try {
    reportData = await aggregateMonthForScope({ start: range.start, end: range.end });
    const pdfBuffer = await buildPdfBuffer({
      user: { name: 'SevaSetu Community', email: 'all-users@sevasetu.local', jurisdiction: { name: reportData.scopeLabel } },
      month: range.month,
      reportData
    });
    console.log('monthly-summary:shared:pdf-generated', { month: range.month, pdfBytes: pdfBuffer.length });
    uploadedRawUrl = await uploadPdfToCloudinary({
      buffer: pdfBuffer,
      publicId: `monthly-summary/${range.month}/all-users-${runStamp}`
    });
    const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
    const uploadMatch = uploadedRawUrl?.match(/\/v(\d+)\/(.+)\.(pdf|png|jpg|jpeg|webp)$/i);
    if (uploadMatch) {
      const [, version, uploadedPublicId] = uploadMatch;
      reportUrl = toJpgPreviewUrl({ cloudName, publicId: uploadedPublicId, version });
    } else {
      reportUrl = uploadedRawUrl;
    }
    generatedCount = 1;
    console.log('monthly-summary:shared:uploaded', {
      month: range.month,
      uploadedRawUrl,
      deliveredUrl: reportUrl
    });
  } catch (error) {
    console.error('monthly-summary:shared:failed', { month: range.month, error: error?.message || error });
    return {
      month: range.month,
      totalUsers: users.length,
      generatedCount: 0,
      sentCount: 0,
      failedCount: users.length
    };
  }

  for (const user of users) {
    try {
      await createMonthlyReportNotification({
        userId: user.id,
        title: `Monthly summary for ${range.month}`,
        body: `Your report is ready. Total issues: ${reportData.totalIssues}. Tap to download.`,
        reportUrl,
        metadata: {
          month: range.month,
          generatedAt: new Date().toISOString(),
          totalIssues: reportData.totalIssues,
          statusCounts: reportData.statusCounts,
          resolvedThisMonth: reportData.resolvedThisMonth,
          pendingIssues: reportData.pendingIssues,
          mostReportedCategory: reportData.mostReportedCategory,
          avgResolutionHours: reportData.avgResolutionHours,
          recentUpdates: reportData.recentUpdates,
          jurisdiction: user.jurisdiction,
          reportScope: reportData.scopeLabel,
          uploadedRawUrl,
          notableAchievements: [
            reportData.mostReportedCategory
              ? `Most reported category: ${reportData.mostReportedCategory}`
              : 'No dominant category this month'
          ],
          appendix: reportData.appendix
        }
      });

      sentCount += 1;
      console.log('monthly-summary:user:notification-sent', {
        userId: user.id,
        month: range.month,
        reportUrl
      });
    } catch (error) {
      failedCount += 1;
      console.error('monthly-summary:user:failed', { userId: user.id, error: error?.message || error });
    }
  }

  const summary = {
    month: range.month,
    totalUsers: users.length,
    generatedCount,
    sentCount,
    failedCount
  };
  console.log('monthly-summary:complete', summary);
  return summary;
};
