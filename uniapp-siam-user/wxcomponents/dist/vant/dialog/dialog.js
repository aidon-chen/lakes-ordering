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
var dialog_exports = {};
__export(dialog_exports, {
  default: () => dialog_default
});
module.exports = __toCommonJS(dialog_exports);
let queue = [];
const defaultOptions = {
  show: false,
  title: "",
  width: null,
  theme: "default",
  message: "",
  zIndex: 100,
  overlay: true,
  selector: "#van-dialog",
  className: "",
  asyncClose: false,
  beforeClose: null,
  transition: "scale",
  customStyle: "",
  messageAlign: "",
  overlayStyle: "",
  confirmButtonText: "\u786E\u8BA4",
  cancelButtonText: "\u53D6\u6D88",
  showConfirmButton: true,
  showCancelButton: false,
  closeOnClickOverlay: false,
  confirmButtonOpenType: ""
};
let currentOptions = Object.assign({}, defaultOptions);
function getContext() {
  const pages = getCurrentPages();
  return pages[pages.length - 1];
}
const Dialog = (options) => {
  options = Object.assign(Object.assign({}, currentOptions), options);
  return new Promise((resolve, reject) => {
    const context = (typeof options.context === "function" ? options.context() : options.context) || getContext();
    const dialog = context.selectComponent(options.selector);
    delete options.context;
    delete options.selector;
    if (dialog) {
      dialog.setData(Object.assign({ callback: (action, instance) => {
        action === "confirm" ? resolve(instance) : reject(instance);
      } }, options));
      wx.nextTick(() => {
        dialog.setData({ show: true });
      });
      queue.push(dialog);
    } else {
      console.warn("\u672A\u627E\u5230 van-dialog \u8282\u70B9\uFF0C\u8BF7\u786E\u8BA4 selector \u53CA context \u662F\u5426\u6B63\u786E");
    }
  });
};
Dialog.alert = (options) => Dialog(options);
Dialog.confirm = (options) => Dialog(Object.assign({ showCancelButton: true }, options));
Dialog.close = () => {
  queue.forEach((dialog) => {
    dialog.close();
  });
  queue = [];
};
Dialog.stopLoading = () => {
  queue.forEach((dialog) => {
    dialog.stopLoading();
  });
};
Dialog.currentOptions = currentOptions;
Dialog.defaultOptions = defaultOptions;
Dialog.setDefaultOptions = (options) => {
  currentOptions = Object.assign(Object.assign({}, currentOptions), options);
  Dialog.currentOptions = currentOptions;
};
Dialog.resetDefaultOptions = () => {
  currentOptions = Object.assign({}, defaultOptions);
  Dialog.currentOptions = currentOptions;
};
Dialog.resetDefaultOptions();
var dialog_default = Dialog;
