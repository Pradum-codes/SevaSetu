import express from 'express';
import dotenv from 'dotenv';

dotenv.config();
const app = express();

const app = require('./app');

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});

app.get('/', (req, res) => {
  res.send('Hello, World!');
});
