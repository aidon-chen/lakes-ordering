var import_component = require("../common/component");
(0, import_component.VantComponent)({
  classes: ["info-class"],
  props: {
    dot: Boolean,
    info: null,
    size: null,
    color: String,
    customStyle: String,
    classPrefix: {
      type: String,
      value: "van-icon"
    },
    name: String
  },
  methods: {
    onClick() {
      this.$emit("click");
    }
  }
});
