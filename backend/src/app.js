const express = require("express");
const authRoutes = require("./routes/authRoutes");
const errorHandler = require("./middleware/errorHandler");

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// quick sanity check from the browser / emulator
app.get("/api/health", (req, res) =>
  res.json({ success: true, message: "TrailMind API is running" })
);

app.use("/api/auth", authRoutes);

// unmatched routes
app.use((req, res) =>
  res.status(404).json({ success: false, message: `Route ${req.originalUrl} not found` })
);

app.use(errorHandler);

module.exports = app;