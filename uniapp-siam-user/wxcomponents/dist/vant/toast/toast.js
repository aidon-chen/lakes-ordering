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
var toast_exports = {};
__export(toast_exports, {
  default: () => toast_default
});
module.exports = __toCommonJS(toast_exports);
var import_validator = require("../common/validator");
const defaultOptions = {
  type: "text",
  mask: false,
  message: "",
  show: true,
  zIndex: 1e3,
  duration: 2e3,
  position: "middle",
  forbidClick: false,
  loadingType: "circular",
  selector: "#van-toast"
};
let queue = [];
let currentOptions = Object.assign({}, defaultOptions);
function parseOptions(message) {
  return (0, import_validator.isObj)(message) ? message : { message };
}
function getContext() {
  const pages = getCurrentPages();
  return pages[pages.length - 1];
}
function Toast(toastOptions) {
  const options = Object.assign(Object.assign({}, currentOptions), parseOptions(toastOptions));
  const context = (typeof options.context === "function" ? options.context() : options.context) || getContext();
  const toast = context.selectComponent(options.selector);
  if (!toast) {
    console.warn("\u672A\u627E\u5230 van-toast \u8282\u70B9\uFF0C\u8BF7\u786E\u8BA4 selector \u53CA context \u662F\u5426\u6B63\u786E");
    return;
  }
  delete options.context;
  delete options.selector;
  toast.clear = () => {
    toast.setData({ show: false });
    if (options.onClose) {
      options.onClose();
    }
  };
  queue.push(toast);
  toast.setData(options);
  clearTimeout(toast.timer);
  if (options.duration != null && options.duration > 0) {
    toast.timer = setTimeout(() => {
      toast.clear();
      queue = queue.filter((item) => item !== toast);
    }, options.duration);
  }
  return toast;
}
const createMethod = (type) => (options) => Toast(Object.assign({ type }, parseOptions(options)));
Toast.loading = createMethod("loading");
Toast.success = createMethod("success");
Toast.fail = createMethod("fail");
Toast.clear = () => {
  queue.forEach((toast) => {
    toast.clear();
  });
  queue = [];
};
Toast.setDefaultOptions = (options) => {
  Object.assign(currentOptions, options);
};
Toast.resetDefaultOptions = () => {
  currentOptions = Object.assign({}, defaultOptions);
};
var toast_default = Toast;
