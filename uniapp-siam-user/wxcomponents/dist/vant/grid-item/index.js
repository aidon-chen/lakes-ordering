var import_component = require("../common/component");
var import_relation = require("../common/relation");
var import_link = require("../mixins/link");
(0, import_component.VantComponent)({
  relation: (0, import_relation.useParent)("grid"),
  classes: ["content-class", "icon-class", "text-class"],
  mixins: [import_link.link],
  props: {
    icon: String,
    iconColor: String,
    iconPrefix: {
      type: String,
      value: "van-icon"
    },
    dot: Boolean,
    info: null,
    badge: null,
    text: String,
    useSlot: Boolean
  },
  data: {
    viewStyle: ""
  },
  mounted() {
    this.updateStyle();
  },
  methods: {
    updateStyle() {
      if (!this.parent) {
        return;
      }
      const { data, children } = this.parent;
      const { columnNum, border, square, gutter, clickable, center, direction, reverse, iconSize } = data;
      this.setData({
        center,
        border,
        square,
        gutter,
        clickable,
        direction,
        reverse,
        iconSize,
        index: children.indexOf(this),
        columnNum
      });
    },
    onClick() {
      this.$emit("click");
      this.jumpLink();
    }
  }
});
