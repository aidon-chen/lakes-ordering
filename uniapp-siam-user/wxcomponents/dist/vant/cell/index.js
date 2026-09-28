var import_link = require("../mixins/link");
var import_component = require("../common/component");
(0, import_component.VantComponent)({
  classes: [
    "title-class",
    "label-class",
    "value-class",
    "right-icon-class",
    "hover-class"
  ],
  mixins: [import_link.link],
  props: {
    title: null,
    value: null,
    icon: String,
    size: String,
    label: String,
    center: Boolean,
    isLink: Boolean,
    required: Boolean,
    clickable: Boolean,
    titleWidth: String,
    customStyle: String,
    arrowDirection: String,
    useLabelSlot: Boolean,
    border: {
      type: Boolean,
      value: true
    },
    titleStyle: String
  },
  methods: {
    onClick(event) {
      this.$emit("click", event.detail);
      this.jumpLink();
    }
  }
});
