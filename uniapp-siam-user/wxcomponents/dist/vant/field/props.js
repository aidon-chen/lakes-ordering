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
var props_exports = {};
__export(props_exports, {
  commonProps: () => commonProps,
  inputProps: () => inputProps,
  textareaProps: () => textareaProps
});
module.exports = __toCommonJS(props_exports);
const commonProps = {
  value: {
    type: String,
    observer(value) {
      if (value !== this.value) {
        this.setData({ innerValue: value });
        this.value = value;
      }
    }
  },
  placeholder: String,
  placeholderStyle: String,
  placeholderClass: String,
  disabled: Boolean,
  maxlength: {
    type: Number,
    value: -1
  },
  cursorSpacing: {
    type: Number,
    value: 50
  },
  autoFocus: Boolean,
  focus: Boolean,
  cursor: {
    type: Number,
    value: -1
  },
  selectionStart: {
    type: Number,
    value: -1
  },
  selectionEnd: {
    type: Number,
    value: -1
  },
  adjustPosition: {
    type: Boolean,
    value: true
  },
  holdKeyboard: Boolean
};
const inputProps = {
  type: {
    type: String,
    value: "text"
  },
  password: Boolean,
  confirmType: String,
  confirmHold: Boolean,
  alwaysEmbed: Boolean
};
const textareaProps = {
  autoHeight: Boolean,
  fixed: Boolean,
  showConfirmBar: {
    type: Boolean,
    value: true
  },
  disableDefaultPadding: {
    type: Boolean,
    value: true
  }
};
