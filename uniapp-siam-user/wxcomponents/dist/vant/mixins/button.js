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
var button_exports = {};
__export(button_exports, {
  button: () => button
});
module.exports = __toCommonJS(button_exports);
var import_version = require("../common/version");
const button = Behavior({
  externalClasses: ["hover-class"],
  properties: {
    id: String,
    buttonId: String,
    lang: String,
    businessId: Number,
    sessionFrom: String,
    sendMessageTitle: String,
    sendMessagePath: String,
    sendMessageImg: String,
    showMessageCard: Boolean,
    appParameter: String,
    ariaLabel: String,
    openType: String,
    getUserProfileDesc: String
  },
  data: {
    canIUseGetUserProfile: (0, import_version.canIUseGetUserProfile)()
  },
  methods: {
    onGetUserInfo(event) {
      this.triggerEvent("getuserinfo", event.detail);
    },
    onContact(event) {
      this.triggerEvent("contact", event.detail);
    },
    onGetPhoneNumber(event) {
      this.triggerEvent("getphonenumber", event.detail);
    },
    onGetRealTimePhoneNumber(event) {
      this.triggerEvent("getrealtimephonenumber", event.detail);
    },
    onError(event) {
      this.triggerEvent("error", event.detail);
    },
    onLaunchApp(event) {
      this.triggerEvent("launchapp", event.detail);
    },
    onOpenSetting(event) {
      this.triggerEvent("opensetting", event.detail);
    },
    onAgreePrivacyAuthorization(event) {
      this.triggerEvent("agreeprivacyauthorization", event.detail);
    },
    onChooseAvatar(event) {
      this.triggerEvent("chooseavatar", event.detail);
    }
  }
});
