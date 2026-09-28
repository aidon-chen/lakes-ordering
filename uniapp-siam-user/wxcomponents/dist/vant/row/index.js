var import_component = require("../common/component");
var import_relation = require("../common/relation");
(0, import_component.VantComponent)({
  relation: (0, import_relation.useChildren)("col", function(target) {
    const { gutter } = this.data;
    if (gutter) {
      target.setData({ gutter });
    }
  }),
  props: {
    gutter: {
      type: Number,
      observer: "setGutter"
    }
  },
  methods: {
    setGutter() {
      this.children.forEach((col) => {
        col.setData(this.data);
      });
    }
  }
});
