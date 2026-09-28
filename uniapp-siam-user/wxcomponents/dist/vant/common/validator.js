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
var validator_exports = {};
__export(validator_exports, {
  isBoolean: () => isBoolean,
  isDef: () => isDef,
  isFunction: () => isFunction,
  isImageUrl: () => isImageUrl,
  isNumber: () => isNumber,
  isObj: () => isObj,
  isPlainObject: () => isPlainObject,
  isPromise: () => isPromise,
  isVideoUrl: () => isVideoUrl
});
module.exports = __toCommonJS(validator_exports);
function isFunction(val) {
  return typeof val === "function";
}
function isPlainObject(val) {
  return val !== null && typeof val === "object" && !Array.isArray(val);
}
function isPromise(val) {
  return isPlainObject(val) && isFunction(val.then) && isFunction(val.catch);
}
function isDef(value) {
  return value !== void 0 && value !== null;
}
function isObj(x) {
  const type = typeof x;
  return x !== null && (type === "object" || type === "function");
}
function isNumber(value) {
  return /^\d+(\.\d+)?$/.test(value);
}
function isBoolean(value) {
  return typeof value === "boolean";
}
const IMAGE_REGEXP = /\.(jpeg|jpg|gif|png|svg|webp|jfif|bmp|dpg)/i;
const VIDEO_REGEXP = /\.(mp4|mpg|mpeg|dat|asf|avi|rm|rmvb|mov|wmv|flv|mkv)/i;
function isImageUrl(url) {
  return IMAGE_REGEXP.test(url);
}
function isVideoUrl(url) {
  return VIDEO_REGEXP.test(url);
}
