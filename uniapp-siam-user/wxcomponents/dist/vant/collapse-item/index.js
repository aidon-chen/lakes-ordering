var import_component = require("../common/component");
var import_relation = require("../common/relation");
var import_animate = require("./animate");
(0, import_component.VantComponent)({
  classes: ["title-class", "content-class"],
  relation: (0, import_relation.useParent)("collapse"),
  props: {
    size: String,
    name: null,
    title: null,
    value: null,
    icon: String,
    label: String,
    disabled: Boolean,
    clickable: Boolean,
    border: {
      type: Boolean,
      value: true
    },
    isLink: {
      type: Boolean,
      value: true
    }
  },
  data: {
    expanded: false
  },
  mounted() {
    this.updateExpanded();
    this.mounted = true;
  },
  methods: {
    updateExpanded() {
      if (!this.parent) {
        return;
      }
      const { value, accordion } = this.parent.data;
      const { children = [] } = this.parent;
      const { name } = this.data;
      const index = children.indexOf(this);
      const currentName = name == null ? index : name;
      const expanded = accordion ? value === currentName : (value || []).some((name2) => name2 === currentName);
      if (expanded !== this.data.expanded) {
        (0, import_animate.setContentAnimate)(this, expanded, this.mounted);
      }
      this.setData({ index, expanded });
    },
    onClick() {
      if (this.data.disabled) {
        return;
      }
      const { name, expanded } = this.data;
      const index = this.parent.children.indexOf(this);
      const currentName = name == null ? index : name;
      this.parent.switch(currentName, !expanded);
    }
  }
});
