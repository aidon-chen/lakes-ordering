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
var version_exports = {};
__export(version_exports, {
  canIUseAnimate: () => canIUseAnimate,
  canIUseCanvas2d: () => canIUseCanvas2d,
  canIUseFormFieldButton: () => canIUseFormFieldButton,
  canIUseGetUserProfile: () => canIUseGetUserProfile,
  canIUseGroupSetData: () => canIUseGroupSetData,
  canIUseModel: () => canIUseModel,
  canIUseNextTick: () => canIUseNextTick,
  getSystemInfoSync: () => getSystemInfoSync
});
module.exports = __toCommonJS(version_exports);
let systemInfo;
function getSystemInfoSync() {
  if (systemInfo == null) {
    systemInfo = wx.getSystemInfoSync();
  }
  return systemInfo;
}
function compareVersion(v1, v2) {
  v1 = v1 ? v1.split(".") : [];
  v2 = v2 ? v2.split(".") : [];
  const len = Math.max(v1.length, v2.length);
  while (v1.length < len) {
    v1.push("0");
  }
  while (v2.length < len) {
    v2.push("0");
  }
  for (let i = 0; i < len; i++) {
    const num1 = parseInt(v1[i], 10);
    const num2 = parseInt(v2[i], 10);
    if (num1 > num2) {
      return 1;
    }
    if (num1 < num2) {
      return -1;
    }
  }
  return 0;
}
function gte(version) {
  const system = getSystemInfoSync();
  return compareVersion(system.SDKVersion, version) >= 0;
}
function canIUseModel() {
  return gte("2.9.3");
}
function canIUseFormFieldButton() {
  return gte("2.10.3");
}
function canIUseAnimate() {
  return gte("2.9.0");
}
function canIUseGroupSetData() {
  return gte("2.4.0");
}
function canIUseNextTick() {
  try {
    return wx.canIUse("nextTick");
  } catch (e) {
    return gte("2.7.1");
  }
}
function canIUseCanvas2d() {
  return gte("2.9.0");
}
function canIUseGetUserProfile() {
  return !!wx.getUserProfile;
}
