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
var page_scroll_exports = {};
__export(page_scroll_exports, {
  pageScrollMixin: () => pageScrollMixin
});
module.exports = __toCommonJS(page_scroll_exports);
var import_validator = require("../common/validator");
var import_utils = require("../common/utils");
function onPageScroll(event) {
  const { vanPageScroller = [] } = (0, import_utils.getCurrentPage)();
  vanPageScroller.forEach((scroller) => {
    if (typeof scroller === "function") {
      scroller(event);
    }
  });
}
function pageScrollMixin(scroller) {
  return Behavior({
    attached() {
      const page = (0, import_utils.getCurrentPage)();
      if (!(0, import_utils.isDef)(page)) {
        return;
      }
      const _scroller = scroller.bind(this);
      const { vanPageScroller = [] } = page;
      if ((0, import_validator.isFunction)(page.onPageScroll) && page.onPageScroll !== onPageScroll) {
        vanPageScroller.push(page.onPageScroll.bind(page));
      }
      vanPageScroller.push(_scroller);
      page.vanPageScroller = vanPageScroller;
      page.onPageScroll = onPageScroll;
      this._scroller = _scroller;
    },
    detached() {
      const page = (0, import_utils.getCurrentPage)();
      if (!(0, import_utils.isDef)(page) || !(0, import_utils.isDef)(page.vanPageScroller)) {
        return;
      }
      const { vanPageScroller } = page;
      const index = vanPageScroller.findIndex((v) => v === this._scroller);
      if (index > -1) {
        page.vanPageScroller.splice(index, 1);
      }
      this._scroller = void 0;
    }
  });
}
