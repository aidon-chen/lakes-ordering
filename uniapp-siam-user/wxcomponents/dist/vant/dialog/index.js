var import_component = require("../common/component");
var import_button = require("../mixins/button");
var import_color = require("../common/color");
var import_utils = require("../common/utils");
(0, import_component.VantComponent)({
  mixins: [import_button.button],
  classes: ["cancle-button-class", "confirm-button-class"],
  props: {
    show: {
      type: Boolean,
      observer(show) {
        !show && this.stopLoading();
      }
    },
    title: String,
    message: String,
    theme: {
      type: String,
      value: "default"
    },
    confirmButtonId: String,
    className: String,
    customStyle: String,
    asyncClose: Boolean,
    messageAlign: String,
    beforeClose: null,
    overlayStyle: String,
    useSlot: Boolean,
    useTitleSlot: Boolean,
    useConfirmButtonSlot: Boolean,
    useCancelButtonSlot: Boolean,
    showCancelButton: Boolean,
    closeOnClickOverlay: Boolean,
    confirmButtonOpenType: String,
    width: null,
    zIndex: {
      type: Number,
      value: 2e3
    },
    confirmButtonText: {
      type: String,
      value: "\u786E\u8BA4"
    },
    cancelButtonText: {
      type: String,
      value: "\u53D6\u6D88"
    },
    confirmButtonColor: {
      type: String,
      value: import_color.RED
    },
    cancelButtonColor: {
      type: String,
      value: import_color.GRAY
    },
    showConfirmButton: {
      type: Boolean,
      value: true
    },
    overlay: {
      type: Boolean,
      value: true
    },
    transition: {
      type: String,
      value: "scale"
    }
  },
  data: {
    loading: {
      confirm: false,
      cancel: false
    },
    callback: () => {
    }
  },
  methods: {
    onConfirm() {
      this.handleAction("confirm");
    },
    onCancel() {
      this.handleAction("cancel");
    },
    onClickOverlay() {
      this.close("overlay");
    },
    close(action) {
      this.setData({ show: false });
      wx.nextTick(() => {
        this.$emit("close", action);
        const { callback } = this.data;
        if (callback) {
          callback(action, this);
        }
      });
    },
    stopLoading() {
      this.setData({
        loading: {
          confirm: false,
          cancel: false
        }
      });
    },
    handleAction(action) {
      this.$emit(action, { dialog: this });
      const { asyncClose, beforeClose } = this.data;
      if (!asyncClose && !beforeClose) {
        this.close(action);
        return;
      }
      this.setData({
        [`loading.${action}`]: true
      });
      if (beforeClose) {
        (0, import_utils.toPromise)(beforeClose(action)).then((value) => {
          if (value) {
            this.close(action);
          } else {
            this.stopLoading();
          }
        });
      }
    }
  }
});
