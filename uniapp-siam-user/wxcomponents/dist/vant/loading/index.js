var import_component = require("../common/component");
(0, import_component.VantComponent)({
  props: {
    color: String,
    vertical: Boolean,
    type: {
      type: String,
      value: "circular"
    },
    size: String,
    textSize: String
  },
  data: {
    array12: Array.from({ length: 12 })
  }
});
