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
var transition_exports = {};
__export(transition_exports, {
  transition: () => transition
});
module.exports = __toCommonJS(transition_exports);
var import_utils = require("../common/utils");
var import_validator = require("../common/validator");
const getClassNames = (name) => ({
  enter: `van-${name}-enter van-${name}-enter-active enter-class enter-active-class`,
  "enter-to": `van-${name}-enter-to van-${name}-enter-active enter-to-class enter-active-class`,
  leave: `van-${name}-leave van-${name}-leave-active leave-class leave-active-class`,
  "leave-to": `van-${name}-leave-to van-${name}-leave-active leave-to-class leave-active-class`
});
function transition(showDefaultValue) {
  return Behavior({
    properties: {
      customStyle: String,
      // @ts-ignore
      show: {
        type: Boolean,
        value: showDefaultValue,
        observer: "observeShow"
      },
      // @ts-ignore
      duration: {
        type: null,
        value: 300,
        observer: "observeDuration"
      },
      name: {
        type: String,
        value: "fade"
      }
    },
    data: {
      type: "",
      inited: false,
      display: false
    },
    ready() {
      if (this.data.show === true) {
        this.observeShow(true, false);
      }
    },
    methods: {
      observeShow(value, old) {
        if (value === old) {
          return;
        }
        value ? this.enter() : this.leave();
      },
      enter() {
        const { duration, name } = this.data;
        const classNames = getClassNames(name);
        const currentDuration = (0, import_validator.isObj)(duration) ? duration.enter : duration;
        if (this.status === "enter") {
          return;
        }
        this.status = "enter";
        this.$emit("before-enter");
        (0, import_utils.requestAnimationFrame)(() => {
          if (this.status !== "enter") {
            return;
          }
          this.$emit("enter");
          this.setData({
            inited: true,
            display: true,
            classes: classNames.enter,
            currentDuration
          });
          (0, import_utils.requestAnimationFrame)(() => {
            if (this.status !== "enter") {
              return;
            }
            this.transitionEnded = false;
            this.setData({ classes: classNames["enter-to"] });
          });
        });
      },
      leave() {
        if (!this.data.display) {
          return;
        }
        const { duration, name } = this.data;
        const classNames = getClassNames(name);
        const currentDuration = (0, import_validator.isObj)(duration) ? duration.leave : duration;
        this.status = "leave";
        this.$emit("before-leave");
        (0, import_utils.requestAnimationFrame)(() => {
          if (this.status !== "leave") {
            return;
          }
          this.$emit("leave");
          this.setData({
            classes: classNames.leave,
            currentDuration
          });
          (0, import_utils.requestAnimationFrame)(() => {
            if (this.status !== "leave") {
              return;
            }
            this.transitionEnded = false;
            setTimeout(() => this.onTransitionEnd(), currentDuration);
            this.setData({ classes: classNames["leave-to"] });
          });
        });
      },
      onTransitionEnd() {
        if (this.transitionEnded) {
          return;
        }
        this.transitionEnded = true;
        this.$emit(`after-${this.status}`);
        const { show, display } = this.data;
        if (!show && display) {
          this.setData({ display: false });
        }
      }
    }
  });
}
