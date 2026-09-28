var import_component = require("../common/component");
(0, import_component.VantComponent)({
  classes: [
    "main-item-class",
    "content-item-class",
    "main-active-class",
    "content-active-class",
    "main-disabled-class",
    "content-disabled-class"
  ],
  props: {
    items: {
      type: Array,
      observer: "updateSubItems"
    },
    activeId: null,
    mainActiveIndex: {
      type: Number,
      value: 0,
      observer: "updateSubItems"
    },
    height: {
      type: null,
      value: 300
    },
    max: {
      type: Number,
      value: Infinity
    },
    selectedIcon: {
      type: String,
      value: "success"
    }
  },
  data: {
    subItems: []
  },
  methods: {
    // 当一个子项被选择时
    onSelectItem(event) {
      const { item } = event.currentTarget.dataset;
      const isArray = Array.isArray(this.data.activeId);
      const isOverMax = isArray && this.data.activeId.length >= this.data.max;
      const isSelected = isArray ? this.data.activeId.indexOf(item.id) > -1 : this.data.activeId === item.id;
      if (!item.disabled && (!isOverMax || isSelected)) {
        this.$emit("click-item", item);
      }
    },
    // 当一个导航被点击时
    onClickNav(event) {
      const index = event.detail;
      const item = this.data.items[index];
      if (!item.disabled) {
        this.$emit("click-nav", { index });
      }
    },
    // 更新子项列表
    updateSubItems() {
      const { items, mainActiveIndex } = this.data;
      const { children = [] } = items[mainActiveIndex] || {};
      this.setData({ subItems: children });
    }
  }
});
