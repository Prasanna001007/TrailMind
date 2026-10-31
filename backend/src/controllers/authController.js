const jwt = require("jsonwebtoken");
const User = require("../models/User");
const asyncHandler = require("../utils/asyncHandler");

const signToken = (userId) =>
  jwt.sign({ id: userId }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN || "7d",
  });

// POST /api/auth/register
const register = asyncHandler(async (req, res) => {
  const { name, email, password, nationality } = req.body;

  if (!name || !email || !password) {
    return res
      .status(400)
      .json({ success: false, message: "Name, email and password are required" });
  }

  if (password.length < 8) {
    return res.status(400).json({
      success: false,
      message: "Password must be at least 8 characters",
    });
  }

  const exists = await User.findOne({ email: email.toLowerCase() });
  if (exists) {
    return res
      .status(409)
      .json({ success: false, message: "An account with that email already exists" });
  }

  const user = await User.create({
    name,
    email,
    passwordHash: password, // pre-save hook hashes this
    nationality,
  });

  res.status(201).json({
    success: true,
    token: signToken(user._id),
    user: user.toSafeJSON(),
  });
});

// POST /api/auth/login
const login = asyncHandler(async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res
      .status(400)
      .json({ success: false, message: "Email and password are required" });
  }

  // passwordHash is select:false, so ask for it explicitly
  const user = await User.findOne({ email: email.toLowerCase() }).select(
    "+passwordHash"
  );

  // same message for both cases — don't leak which emails exist
  if (!user || !(await user.comparePassword(password))) {
    return res
      .status(401)
      .json({ success: false, message: "Invalid email or password" });
  }

  res.status(200).json({
    success: true,
    token: signToken(user._id),
    user: user.toSafeJSON(),
  });
});

// GET /api/auth/me   (protected)
const me = asyncHandler(async (req, res) => {
  res.status(200).json({ success: true, user: req.user.toSafeJSON() });
});

module.exports = { register, login, me };