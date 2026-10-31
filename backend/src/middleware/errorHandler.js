const errorHandler = (err, req, res, next) => {
  console.error(err);

  // duplicate email
  if (err.code === 11000) {
    return res.status(409).json({
      success: false,
      message: "An account with that email already exists",
    });
  }

  // mongoose validation
  if (err.name === "ValidationError") {
    const message = Object.values(err.errors)
      .map((e) => e.message)
      .join(", ");
    return res.status(400).json({ success: false, message });
  }

  // bad ObjectId
  if (err.name === "CastError") {
    return res.status(400).json({ success: false, message: "Invalid ID" });
  }

  const status = err.statusCode || 500;
  res.status(status).json({
    success: false,
    message: err.message || "Internal server error",
  });
};

module.exports = errorHandler;