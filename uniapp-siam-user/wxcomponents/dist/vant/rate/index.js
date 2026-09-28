var import_utils = require("../common/utils");
var import_component = require("../common/component");
var import_version = require("../common/version");
(0, import_component.VantComponent)({
  field: true,
  classes: ["icon-class"],
  props: {
    value: {
      type: Number,
      observer(value) {
        if (value !== this.data.innerValue) {
          this.setData({ innerValue: value });
        }
      }
    },
    readonly: Boolean,
    disabled: Boolean,
    allowHalf: Boolean,
    size: null,
    icon: {
      type: String,
      value: "star"
    },
    voidIcon: {
      type: String,
      value: "star-o"
    },
    color: String,
    voidColor: String,
    disabledColor: String,
    count: {
      type: Number,
      value: 5,
      observer(value) {
        this.setData({ innerCountArray: Array.from({ length: value }) });
      }
    },
    gutter: null,
    touchable: {
      type: Boolean,
      value: true
    }
  },
  data: {
    innerValue: 0,
    innerCountArray: Array.from({ length: 5 })
  },
  methods: {
    onSelect(event) {
      const { data } = this;
      const { score } = event.currentTarget.dataset;
      if (!data.disabled && !data.readonly) {
        this.setData({ innerValue: score + 1 });
        if ((0, import_version.canIUseModel)()) {
          this.setData({ value: score + 1 });
        }
        wx.nextTick(() => {
          this.$emit("input", score + 1);
          this.$emit("change", score + 1);
        });
      }
    },
    onTouchMove(event) {
      const { touchable } = this.data;
      if (!touchable)
        return;
      const { clientX } = event.touches[0];
      (0, import_utils.getAllRect)(this, ".van-rate__icon").then((list) => {
        const target = list.sort((cur, next) => cur.dataset.score - next.dataset.score).find((item) => clientX >= item.left && clientX <= item.right);
        if (target != null) {
          this.onSelect(Object.assign(Object.assign({}, event), { currentTarget: target }));
        }
      });
    }
  }
});
