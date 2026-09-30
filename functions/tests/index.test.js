const assert = require("assert");

try {
  // Test module imports
  const security = require("../src/security");
  assert(typeof security.assertAuthenticated === "function");
  assert(typeof security.assertAdminRole === "function");

  console.log("All function security helpers validated successfully.");
} catch (err) {
  console.error("Functions test failed:", err);
  process.exit(1);
}
