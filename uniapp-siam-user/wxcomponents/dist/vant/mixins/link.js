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
var link_exports = {};
__export(link_exports, {
  link: () => link
});
module.exports = __toCommonJS(link_exports);
const link = Behavior({
  properties: {
    url: String,
    linkType: {
      type: String,
      value: "navigateTo"
    }
  },
  methods: {
    jumpLink(urlKey = "url") {
      const url = this.data[urlKey];
      if (url) {
        if (this.data.linkType === "navigateTo" && getCurrentPages().length > 9) {
          wx.redirectTo({ url });
        } else {
          wx[this.data.linkType]({ url });
        }
      }
    }
  }
});
