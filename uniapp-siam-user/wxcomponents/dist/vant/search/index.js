var import_component = require("../common/component");
var import_version = require("../common/version");
(0, import_component.VantComponent)({
  field: true,
  classes: ["field-class", "input-class", "cancel-class"],
  props: {
    value: {
      type: String,
      value: ""
    },
    label: String,
    focus: Boolean,
    error: Boolean,
    disabled: Boolean,
    readonly: Boolean,
    inputAlign: String,
    showAction: Boolean,
    useActionSlot: Boolean,
    useLeftIconSlot: Boolean,
    useRightIconSlot: Boolean,
    leftIcon: {
      type: String,
      value: "search"
    },
    rightIcon: String,
    placeholder: String,
    placeholderStyle: String,
    actionText: {
      type: String,
      value: "\u53D6\u6D88"
    },
    background: {
      type: String,
      value: "#ffffff"
    },
    maxlength: {
      type: Number,
      value: -1
    },
    shape: {
      type: String,
      value: "square"
    },
    clearable: {
      type: Boolean,
      value: true
    },
    clearTrigger: {
      type: String,
      value: "focus"
    },
    clearIcon: {
      type: String,
      value: "clear"
    },
    cursorSpacing: {
      type: Number,
      value: 0
    }
  },
  methods: {
    onChange(event) {
      if ((0, import_version.canIUseModel)()) {
        this.setData({ value: event.detail });
      }
      this.$emit("change", event.detail);
    },
    onCancel() {
      setTimeout(() => {
        if ((0, import_version.canIUseModel)()) {
          this.setData({ value: "" });
        }
        this.$emit("cancel");
        this.$emit("change", "");
      }, 200);
    },
    onSearch(event) {
      this.$emit("search", event.detail);
    },
    onFocus(event) {
      this.$emit("focus", event.detail);
    },
    onBlur(event) {
      this.$emit("blur", event.detail);
    },
    onClear(event) {
      this.$emit("clear", event.detail);
    },
    onClickInput(event) {
      this.$emit("click-input", event.detail);
    }
  }
});
