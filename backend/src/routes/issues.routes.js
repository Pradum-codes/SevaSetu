const express = require('express');
const router = express.Router();
const { createIssue, getIssues } = require('../controllers/issues.controller');

router.post('/', createIssue);
router.get('/', getIssues);

module.exports = router;