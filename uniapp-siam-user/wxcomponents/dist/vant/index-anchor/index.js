var import_utils = require("../common/utils");
var import_component = require("../common/component");
var import_relation = require("../common/relation");
(0, import_component.VantComponent)({
  relation: (0, import_relation.useParent)("index-bar"),
  props: {
    useSlot: Boolean,
    index: null
  },
  data: {
    active: false,
    wrapperStyle: "",
    anchorStyle: ""
  },
  methods: {
    scrollIntoView(scrollTop) {
      (0, import_utils.getRect)(this, ".van-index-anchor-wrapper").then((rect) => {
        wx.pageScrollTo({
          duration: 0,
          scrollTop: scrollTop + rect.top - this.parent.data.stickyOffsetTop
        });
      });
    }
  }
});
