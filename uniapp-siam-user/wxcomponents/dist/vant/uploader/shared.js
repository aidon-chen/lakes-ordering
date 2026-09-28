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
var shared_exports = {};
__export(shared_exports, {
  imageProps: () => imageProps,
  mediaProps: () => mediaProps,
  messageFileProps: () => messageFileProps,
  videoProps: () => videoProps
});
module.exports = __toCommonJS(shared_exports);
const imageProps = {
  sizeType: {
    type: Array,
    value: ["original", "compressed"]
  },
  capture: {
    type: Array,
    value: ["album", "camera"]
  },
  showmenu: {
    type: Boolean,
    value: true
  }
};
const videoProps = {
  capture: {
    type: Array,
    value: ["album", "camera"]
  },
  compressed: {
    type: Boolean,
    value: true
  },
  maxDuration: {
    type: Number,
    value: 60
  },
  camera: {
    type: String,
    value: "back"
  }
};
const mediaProps = {
  capture: {
    type: Array,
    value: ["album", "camera"]
  },
  mediaType: {
    type: Array,
    value: ["image", "video"]
  },
  maxDuration: {
    type: Number,
    value: 60
  },
  camera: {
    type: String,
    value: "back"
  }
};
const messageFileProps = {
  extension: null
};
