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
  pickerProps: () => pickerProps
});
module.exports = __toCommonJS(shared_exports);
const pickerProps = {
  title: String,
  loading: Boolean,
  showToolbar: Boolean,
  cancelButtonText: {
    type: String,
    value: "\u53D6\u6D88"
  },
  confirmButtonText: {
    type: String,
    value: "\u786E\u8BA4"
  },
  visibleItemCount: {
    type: Number,
    value: 6
  },
  itemHeight: {
    type: Number,
    value: 44
  }
};
