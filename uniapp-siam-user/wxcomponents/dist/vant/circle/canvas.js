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
var canvas_exports = {};
__export(canvas_exports, {
  adaptor: () => adaptor
});
module.exports = __toCommonJS(canvas_exports);
function adaptor(ctx) {
  return Object.assign(ctx, {
    setStrokeStyle(val) {
      ctx.strokeStyle = val;
    },
    setLineWidth(val) {
      ctx.lineWidth = val;
    },
    setLineCap(val) {
      ctx.lineCap = val;
    },
    setFillStyle(val) {
      ctx.fillStyle = val;
    },
    setFontSize(val) {
      ctx.font = String(val);
    },
    setGlobalAlpha(val) {
      ctx.globalAlpha = val;
    },
    setLineJoin(val) {
      ctx.lineJoin = val;
    },
    setTextAlign(val) {
      ctx.textAlign = val;
    },
    setMiterLimit(val) {
      ctx.miterLimit = val;
    },
    setShadow(offsetX, offsetY, blur, color) {
      ctx.shadowOffsetX = offsetX;
      ctx.shadowOffsetY = offsetY;
      ctx.shadowBlur = blur;
      ctx.shadowColor = color;
    },
    setTextBaseline(val) {
      ctx.textBaseline = val;
    },
    createCircularGradient() {
    },
    draw() {
    }
  });
}
