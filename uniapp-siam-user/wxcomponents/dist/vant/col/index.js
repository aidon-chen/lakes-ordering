var import_relation = require("../common/relation");
var import_component = require("../common/component");
(0, import_component.VantComponent)({
  relation: (0, import_relation.useParent)("row"),
  props: {
    span: Number,
    offset: Number
  }
});
