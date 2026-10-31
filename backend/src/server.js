require("dotenv").config();
const app = require("./app");
const connectDB = require("./config/db");

const PORT = process.env.PORT || 5000;

const start = async () => {
  await connectDB();
  // 0.0.0.0 so the emulator and physical devices on your LAN can reach it
  app.listen(PORT, "0.0.0.0", () =>
    console.log(`Server running on http://localhost:${PORT}`)
  );
};

start();