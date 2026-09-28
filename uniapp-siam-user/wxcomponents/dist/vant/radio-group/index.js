var import_component = require("../common/component");
var import_relation = require("../common/relation");
(0, import_component.VantComponent)({
  field: true,
  relation: (0, import_relation.useChildren)("radio"),
  props: {
    value: {
      type: null,
      observer: "updateChildren"
    },
    direction: String,
    disabled: {
      type: Boolean,
      observer: "updateChildren"
    }
  },
  methods: {
    updateChildren() {
      this.children.forEach((child) => child.updateFromParent());
    }
  }
});
