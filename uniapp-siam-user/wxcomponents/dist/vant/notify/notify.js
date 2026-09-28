var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);
var notify_exports = {};
__export(notify_exports, {
  default: () => Notify
});
module.exports = __toCommonJS(notify_exports);
var import_color = require("../common/color");
const defaultOptions = {
  selector: "#van-notify",
  type: "danger",
  message: "",
  background: "",
  duration: 3e3,
  zIndex: 110,
  top: 0,
  color: import_color.WHITE,
  safeAreaInsetTop: false,
  onClick: () => {
  },
  onOpened: () => {
  },
  onClose: () => {
  }
};
let currentOptions = Object.assign({}, defaultOptions);
function parseOptions(message) {
  if (message == null) {
    return {};
  }
  return typeof message === "string" ? { message } : message;
}
function getContext() {
  const pages = getCurrentPages();
  return pages[pages.length - 1];
}
function Notify(options) {
  options = Object.assign(Object.assign({}, currentOptions), parseOptions(options));
  const context = options.context || getContext();
  const notify = context.selectComponent(options.selector);
  delete options.context;
  delete options.selector;
  if (notify) {
    notify.setData(options);
    notify.showNotify();
    return notify;
  }
  console.warn("\u672A\u627E\u5230 van-notify \u8282\u70B9\uFF0C\u8BF7\u786E\u8BA4 selector \u53CA context \u662F\u5426\u6B63\u786E");
}
Notify.clear = function(options) {
  options = Object.assign(Object.assign({}, defaultOptions), parseOptions(options));
  const context = options.context || getContext();
  const notify = context.selectComponent(options.selector);
  if (notify) {
    notify.hide();
  }
};
Notify.setDefaultOptions = (options) => {
  Object.assign(currentOptions, options);
};
Notify.resetDefaultOptions = () => {
  currentOptions = Object.assign({}, defaultOptions);
};
