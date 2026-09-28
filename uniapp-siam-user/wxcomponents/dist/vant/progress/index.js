var import_component = require("../common/component");
var import_color = require("../common/color");
var import_utils = require("../common/utils");
(0, import_component.VantComponent)({
  props: {
    inactive: Boolean,
    percentage: {
      type: Number,
      observer: "setLeft"
    },
    pivotText: String,
    pivotColor: String,
    trackColor: String,
    showPivot: {
      type: Boolean,
      value: true
    },
    color: {
      type: String,
      value: import_color.BLUE
    },
    textColor: {
      type: String,
      value: "#fff"
    },
    strokeWidth: {
      type: null,
      value: 4
    }
  },
  data: {
    right: 0
  },
  mounted() {
    this.setLeft();
  },
  methods: {
    setLeft() {
      Promise.all([
        (0, import_utils.getRect)(this, ".van-progress"),
        (0, import_utils.getRect)(this, ".van-progress__pivot")
      ]).then(([portion, pivot]) => {
        if (portion && pivot) {
          this.setData({
            right: pivot.width * (this.data.percentage - 100) / 100
          });
        }
      });
    }
  }
});
