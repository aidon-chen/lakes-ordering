var import_component = require("../common/component");
var import_relation = require("../common/relation");
(0, import_component.VantComponent)({
  relation: (0, import_relation.useChildren)("goods-action-button", function() {
    this.children.forEach((item) => {
      item.updateStyle();
    });
  }),
  props: {
    safeAreaInsetBottom: {
      type: Boolean,
      value: true
    }
  }
});
