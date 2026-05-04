import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import authRoutes from './routes/auth.routes.js';
import adminRoutes from './routes/admin.routes.js';
import issueRoutes from './routes/issues.routes.js';
import dashboardRoutes from './routes/dashboard.routes.js';
import usersRoutes from './routes/users.routes.js';

// Load environment variables
dotenv.config();

const app = express();

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

if (process.env.ADMIN_API_TIMING === 'true' || process.env.NODE_ENV === 'development') {
  app.use('/admin', (req, res, next) => {
    const startedAt = Date.now();
    res.on('finish', () => {
    //   console.log(
    //     `[admin-api] ${req.method} ${req.originalUrl} ${res.statusCode} ${Date.now() - startedAt}ms`
    //   );
    });
    next();
  });
}

app.use('/admin', (req, res, next) => {
  res.set('Cache-Control', 'no-store');
  next();
});

// Routes
app.get('/health', (req, res) => {
  res.json({ status: 'OK', message: 'Server is running' });
});

// Example route - replace with actual routes
app.get('/', (req, res) => {
  res.json({ message: 'Welcome to SevaSetu Backend API' });
});

app.use('/auth', authRoutes);
app.use('/admin', adminRoutes);
app.use('/issues', issueRoutes);
app.use('/dashboard', dashboardRoutes);
app.use('/users', usersRoutes);

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({ error: 'Internal Server Error' });
});

export default app;
