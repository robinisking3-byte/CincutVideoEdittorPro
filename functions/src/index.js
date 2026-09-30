const auth = require("./auth");
const users = require("./users");
const friends = require("./friends");
const projects = require("./projects");
const collaboration = require("./collaboration");
const coins = require("./coins");
const payments = require("./payments");
const content = require("./content");
const live = require("./live");
const support = require("./support");
const festivals = require("./festivals");
const notifications = require("./notifications");
const moderation = require("./moderation");
const admin = require("./admin");
const ai = require("./ai");

module.exports = {
  ...auth,
  ...users,
  ...friends,
  ...projects,
  ...collaboration,
  ...coins,
  ...payments,
  ...content,
  ...live,
  ...support,
  ...festivals,
  ...notifications,
  ...moderation,
  ...admin,
  ...ai
};
