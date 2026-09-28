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
var animate_exports = {};
__export(animate_exports, {
  setContentAnimate: () => setContentAnimate
});
module.exports = __toCommonJS(animate_exports);
var import_utils = require("../common/utils");
function useAnimation(context, expanded, mounted, height) {
  const animation = wx.createAnimation({
    duration: 0,
    timingFunction: "ease-in-out"
  });
  if (expanded) {
    if (height === 0) {
      animation.height("auto").top(1).step();
    } else {
      animation.height(height).top(1).step({
        duration: mounted ? 300 : 1
      }).height("auto").step();
    }
    context.setData({
      animation: animation.export()
    });
    return;
  }
  animation.height(height).top(0).step({ duration: 1 }).height(0).step({
    duration: 300
  });
  context.setData({
    animation: animation.export()
  });
}
function setContentAnimate(context, expanded, mounted) {
  (0, import_utils.getRect)(context, ".van-collapse-item__content").then((rect) => rect.height).then((height) => {
    useAnimation(context, expanded, mounted, height);
  });
}
