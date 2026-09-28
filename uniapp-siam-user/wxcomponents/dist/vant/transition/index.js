var import_component = require("../common/component");
var import_transition = require("../mixins/transition");
(0, import_component.VantComponent)({
  classes: [
    "enter-class",
    "enter-active-class",
    "enter-to-class",
    "leave-class",
    "leave-active-class",
    "leave-to-class"
  ],
  mixins: [(0, import_transition.transition)(true)]
});
