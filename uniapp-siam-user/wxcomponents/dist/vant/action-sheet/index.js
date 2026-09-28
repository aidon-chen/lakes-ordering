var import_component = require("../common/component");
var import_button = require("../mixins/button");
(0, import_component.VantComponent)({
  classes: ["list-class"],
  mixins: [import_button.button],
  props: {
    show: Boolean,
    title: String,
    cancelText: String,
    description: String,
    round: {
      type: Boolean,
      value: true
    },
    zIndex: {
      type: Number,
      value: 100
    },
    actions: {
      type: Array,
      value: []
    },
    overlay: {
      type: Boolean,
      value: true
    },
    closeOnClickOverlay: {
      type: Boolean,
      value: true
    },
    closeOnClickAction: {
      type: Boolean,
      value: true
    },
    safeAreaInsetBottom: {
      type: Boolean,
      value: true
    }
  },
  methods: {
    onSelect(event) {
      const { index } = event.currentTarget.dataset;
      const { actions, closeOnClickAction, canIUseGetUserProfile } = this.data;
      const item = actions[index];
      if (item) {
        this.$emit("select", item);
        if (closeOnClickAction) {
          this.onClose();
        }
        if (item.openType === "getUserInfo" && canIUseGetUserProfile) {
          wx.getUserProfile({
            desc: item.getUserProfileDesc || "  ",
            complete: (userProfile) => {
              this.$emit("getuserinfo", userProfile);
            }
          });
        }
      }
    },
    onCancel() {
      this.$emit("cancel");
    },
    onClose() {
      this.$emit("close");
    },
    onClickOverlay() {
      this.$emit("click-overlay");
      this.onClose();
    }
  }
});
