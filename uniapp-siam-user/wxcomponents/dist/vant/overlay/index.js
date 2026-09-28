var import_component = require("../common/component");
(0, import_component.VantComponent)({
  props: {
    show: Boolean,
    customStyle: String,
    duration: {
      type: null,
      value: 300
    },
    zIndex: {
      type: Number,
      value: 1
    },
    lockScroll: {
      type: Boolean,
      value: true
    },
    rootPortal: {
      type: Boolean,
      value: false
    }
  },
  methods: {
    onClick() {
      this.$emit("click");
    },
    // for prevent touchmove
    noop() {
    }
  }
});
