var import_component = require("../common/component");
var import_relation = require("../common/relation");
var import_button = require("../mixins/button");
var import_link = require("../mixins/link");
(0, import_component.VantComponent)({
  mixins: [import_link.link, import_button.button],
  relation: (0, import_relation.useParent)("goods-action"),
  props: {
    text: String,
    color: String,
    size: {
      type: String,
      value: "normal"
    },
    loading: Boolean,
    disabled: Boolean,
    plain: Boolean,
    type: {
      type: String,
      value: "danger"
    }
  },
  methods: {
    onClick(event) {
      this.$emit("click", event.detail);
      this.jumpLink();
    },
    updateStyle() {
      if (this.parent == null) {
        return;
      }
      const { index } = this;
      const { children = [] } = this.parent;
      this.setData({
        isFirst: index === 0,
        isLast: index === children.length - 1
      });
    }
  }
});
