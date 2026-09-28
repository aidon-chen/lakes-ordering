var import_component = require("../common/component");
var import_relation = require("../common/relation");
(0, import_component.VantComponent)({
  classes: ["active-class", "disabled-class"],
  relation: (0, import_relation.useParent)("sidebar"),
  props: {
    dot: Boolean,
    badge: null,
    info: null,
    title: String,
    disabled: Boolean
  },
  methods: {
    onClick() {
      const { parent } = this;
      if (!parent || this.data.disabled) {
        return;
      }
      const index = parent.children.indexOf(this);
      parent.setActive(index).then(() => {
        this.$emit("click", index);
        parent.$emit("change", index);
      });
    },
    setActive(selected) {
      return this.setData({ selected });
    }
  }
});
