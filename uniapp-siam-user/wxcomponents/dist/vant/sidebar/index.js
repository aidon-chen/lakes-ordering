var import_component = require("../common/component");
var import_relation = require("../common/relation");
(0, import_component.VantComponent)({
  relation: (0, import_relation.useChildren)("sidebar-item", function() {
    this.setActive(this.data.activeKey);
  }),
  props: {
    activeKey: {
      type: Number,
      value: 0,
      observer: "setActive"
    }
  },
  beforeCreate() {
    this.currentActive = -1;
  },
  methods: {
    setActive(activeKey) {
      const { children, currentActive } = this;
      if (!children.length) {
        return Promise.resolve();
      }
      this.currentActive = activeKey;
      const stack = [];
      if (currentActive !== activeKey && children[currentActive]) {
        stack.push(children[currentActive].setActive(false));
      }
      if (children[activeKey]) {
        stack.push(children[activeKey].setActive(true));
      }
      return Promise.all(stack);
    }
  }
});
