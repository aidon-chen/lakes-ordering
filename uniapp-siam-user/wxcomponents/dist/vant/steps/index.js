var import_component = require("../common/component");
var import_color = require("../common/color");
(0, import_component.VantComponent)({
  classes: ["desc-class"],
  props: {
    icon: String,
    steps: Array,
    active: Number,
    direction: {
      type: String,
      value: "horizontal"
    },
    activeColor: {
      type: String,
      value: import_color.GREEN
    },
    inactiveColor: {
      type: String,
      value: import_color.GRAY_DARK
    },
    activeIcon: {
      type: String,
      value: "checked"
    },
    inactiveIcon: String
  },
  methods: {
    onClick(event) {
      const { index } = event.currentTarget.dataset;
      this.$emit("click-step", index);
    }
  }
});
