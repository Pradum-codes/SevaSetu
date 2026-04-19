const express = require('express');
const app = express();
const issuesRoutes = require('./routes/issues.routes');

app.use(express.json());

app.get('/', (req, res) => {
  res.send('SevaSetu API running');
});

// src/app.js

app.use('/issues', issuesRoutes);

module.exports = app;