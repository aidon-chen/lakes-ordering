var import_component = require("../common/component");
(0, import_component.VantComponent)({
  props: {
    size: String,
    mark: Boolean,
    color: String,
    plain: Boolean,
    round: Boolean,
    textColor: String,
    type: {
      type: String,
      value: "default"
    },
    closeable: Boolean
  },
  methods: {
    onClose() {
      this.$emit("close");
    }
  }
});
