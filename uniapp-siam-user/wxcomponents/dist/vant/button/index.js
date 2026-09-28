var import_component = require("../common/component");
var import_button = require("../mixins/button");
var import_version = require("../common/version");
const mixins = [import_button.button];
if ((0, import_version.canIUseFormFieldButton)()) {
  mixins.push("wx://form-field-button");
}
(0, import_component.VantComponent)({
  mixins,
  classes: ["hover-class", "loading-class"],
  data: {
    baseStyle: ""
  },
  props: {
    formType: String,
    icon: String,
    classPrefix: {
      type: String,
      value: "van-icon"
    },
    plain: Boolean,
    block: Boolean,
    round: Boolean,
    square: Boolean,
    loading: Boolean,
    hairline: Boolean,
    disabled: Boolean,
    loadingText: String,
    customStyle: String,
    loadingType: {
      type: String,
      value: "circular"
    },
    type: {
      type: String,
      value: "default"
    },
    dataset: null,
    size: {
      type: String,
      value: "normal"
    },
    loadingSize: {
      type: String,
      value: "20px"
    },
    color: String
  },
  methods: {
    onClick(event) {
      this.$emit("click", event);
      const { canIUseGetUserProfile, openType, getUserProfileDesc, lang } = this.data;
      if (openType === "getUserInfo" && canIUseGetUserProfile) {
        wx.getUserProfile({
          desc: getUserProfileDesc || "  ",
          lang: lang || "en",
          complete: (userProfile) => {
            this.$emit("getuserinfo", userProfile);
          }
        });
      }
    }
  }
});
