const pool = require('../db');

exports.createIssue = async (req, res) => {
  const { title, description, lat, lng } = req.body;

  try {
    const result = await pool.query(
      `INSERT INTO issues (title, description, lat, lng)
       VALUES ($1, $2, $3, $4)
       RETURNING *`,
      [title, description, lat, lng]
    );

    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

exports.getIssues = async (req, res) => {
  try {
    const result = await pool.query(`SELECT * FROM issues LIMIT 50`);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};
