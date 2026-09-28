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
var utils_exports = {};
__export(utils_exports, {
  addNumber: () => addNumber,
  addUnit: () => addUnit,
  clamp: () => clamp,
  getAllRect: () => getAllRect,
  getCurrentPage: () => getCurrentPage,
  getRect: () => getRect,
  getSystemInfoSync: () => import_version2.getSystemInfoSync,
  groupSetData: () => groupSetData,
  isDef: () => import_validator2.isDef,
  isPC: () => isPC,
  isWxWork: () => isWxWork,
  nextTick: () => nextTick,
  pickExclude: () => pickExclude,
  range: () => range,
  requestAnimationFrame: () => requestAnimationFrame,
  toPromise: () => toPromise
});
module.exports = __toCommonJS(utils_exports);
var import_validator = require("./validator");
var import_version = require("./version");
var import_validator2 = require("./validator");
var import_version2 = require("./version");
function range(num, min, max) {
  return Math.min(Math.max(num, min), max);
}
function nextTick(cb) {
  if ((0, import_version.canIUseNextTick)()) {
    wx.nextTick(cb);
  } else {
    setTimeout(() => {
      cb();
    }, 1e3 / 30);
  }
}
function addUnit(value) {
  if (!(0, import_validator.isDef)(value)) {
    return void 0;
  }
  value = String(value);
  return (0, import_validator.isNumber)(value) ? `${value}px` : value;
}
function requestAnimationFrame(cb) {
  return setTimeout(() => {
    cb();
  }, 1e3 / 30);
}
function pickExclude(obj, keys) {
  if (!(0, import_validator.isPlainObject)(obj)) {
    return {};
  }
  return Object.keys(obj).reduce((prev, key) => {
    if (!keys.includes(key)) {
      prev[key] = obj[key];
    }
    return prev;
  }, {});
}
function getRect(context, selector) {
  return new Promise((resolve) => {
    wx.createSelectorQuery().in(context).select(selector).boundingClientRect().exec((rect = []) => resolve(rect[0]));
  });
}
function getAllRect(context, selector) {
  return new Promise((resolve) => {
    wx.createSelectorQuery().in(context).selectAll(selector).boundingClientRect().exec((rect = []) => resolve(rect[0]));
  });
}
function groupSetData(context, cb) {
  if ((0, import_version.canIUseGroupSetData)()) {
    context.groupSetData(cb);
  } else {
    cb();
  }
}
function toPromise(promiseLike) {
  if ((0, import_validator.isPromise)(promiseLike)) {
    return promiseLike;
  }
  return Promise.resolve(promiseLike);
}
function addNumber(num1, num2) {
  const cardinal = Math.pow(10, 10);
  return Math.round((num1 + num2) * cardinal) / cardinal;
}
const clamp = (num, min, max) => Math.min(Math.max(num, min), max);
function getCurrentPage() {
  const pages = getCurrentPages();
  return pages[pages.length - 1];
}
const isPC = ["mac", "windows"].includes((0, import_version.getSystemInfoSync)().platform);
const isWxWork = (0, import_version.getSystemInfoSync)().environment === "wxwork";
