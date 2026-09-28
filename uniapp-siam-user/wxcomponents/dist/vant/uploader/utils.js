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
  chooseFile: () => chooseFile,
  isImageFile: () => isImageFile,
  isVideoFile: () => isVideoFile
});
module.exports = __toCommonJS(utils_exports);
var import_utils = require("../common/utils");
var import_validator = require("../common/validator");
function isImageFile(item) {
  if (item.isImage != null) {
    return item.isImage;
  }
  if (item.type) {
    return item.type === "image";
  }
  if (item.url) {
    return (0, import_validator.isImageUrl)(item.url);
  }
  return false;
}
function isVideoFile(item) {
  if (item.isVideo != null) {
    return item.isVideo;
  }
  if (item.type) {
    return item.type === "video";
  }
  if (item.url) {
    return (0, import_validator.isVideoUrl)(item.url);
  }
  return false;
}
function formatImage(res) {
  return res.tempFiles.map((item) => Object.assign(Object.assign({}, (0, import_utils.pickExclude)(item, ["path"])), { type: "image", url: item.tempFilePath || item.path, thumb: item.tempFilePath || item.path }));
}
function formatVideo(res) {
  return [
    Object.assign(Object.assign({}, (0, import_utils.pickExclude)(res, ["tempFilePath", "thumbTempFilePath", "errMsg"])), { type: "video", url: res.tempFilePath, thumb: res.thumbTempFilePath })
  ];
}
function formatMedia(res) {
  return res.tempFiles.map((item) => Object.assign(Object.assign({}, (0, import_utils.pickExclude)(item, ["fileType", "thumbTempFilePath", "tempFilePath"])), { type: res.type, url: item.tempFilePath, thumb: res.type === "video" ? item.thumbTempFilePath : item.tempFilePath }));
}
function formatFile(res) {
  return res.tempFiles.map((item) => Object.assign(Object.assign({}, (0, import_utils.pickExclude)(item, ["path"])), { url: item.path }));
}
function chooseFile({ accept, multiple, capture, compressed, maxDuration, sizeType, camera, maxCount, mediaType, extension }) {
  return new Promise((resolve, reject) => {
    switch (accept) {
      case "image":
        if (import_utils.isPC || import_utils.isWxWork) {
          wx.chooseImage({
            count: multiple ? Math.min(maxCount, 9) : 1,
            sourceType: capture,
            sizeType,
            success: (res) => resolve(formatImage(res)),
            fail: reject
          });
        } else {
          wx.chooseMedia({
            count: multiple ? Math.min(maxCount, 9) : 1,
            mediaType: ["image"],
            sourceType: capture,
            maxDuration,
            sizeType,
            camera,
            success: (res) => resolve(formatImage(res)),
            fail: reject
          });
        }
        break;
      case "media":
        wx.chooseMedia({
          count: multiple ? Math.min(maxCount, 9) : 1,
          mediaType,
          sourceType: capture,
          maxDuration,
          sizeType,
          camera,
          success: (res) => resolve(formatMedia(res)),
          fail: reject
        });
        break;
      case "video":
        wx.chooseVideo({
          sourceType: capture,
          compressed,
          maxDuration,
          camera,
          success: (res) => resolve(formatVideo(res)),
          fail: reject
        });
        break;
      default:
        wx.chooseMessageFile(Object.assign(Object.assign({ count: multiple ? maxCount : 1, type: accept }, extension ? { extension } : {}), { success: (res) => resolve(formatFile(res)), fail: reject }));
        break;
    }
  });
}
