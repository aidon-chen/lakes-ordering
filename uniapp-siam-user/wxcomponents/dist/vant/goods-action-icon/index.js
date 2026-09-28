var import_component = require("../common/component");
var import_button = require("../mixins/button");
var import_link = require("../mixins/link");
(0, import_component.VantComponent)({
  classes: ["icon-class", "text-class", "info-class"],
  mixins: [import_link.link, import_button.button],
  props: {
    text: String,
    dot: Boolean,
    info: String,
    icon: String,
    size: String,
    color: String,
    classPrefix: {
      type: String,
      value: "van-icon"
    },
    disabled: Boolean,
    loading: Boolean
  },
  methods: {
    onClick(event) {
      this.$emit("click", event.detail);
      this.jumpLink();
    }
  }
});
