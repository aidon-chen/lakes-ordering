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
var color_exports = {};
__export(color_exports, {
  BLUE: () => BLUE,
  GRAY: () => GRAY,
  GRAY_DARK: () => GRAY_DARK,
  GREEN: () => GREEN,
  ORANGE: () => ORANGE,
  RED: () => RED,
  WHITE: () => WHITE
});
module.exports = __toCommonJS(color_exports);
const RED = "#ee0a24";
const BLUE = "#1989fa";
const WHITE = "#fff";
const GREEN = "#07c160";
const ORANGE = "#ff976a";
const GRAY = "#323233";
const GRAY_DARK = "#969799";
