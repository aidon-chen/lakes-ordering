var import_utils = require("../common/utils");
var import_component = require("../common/component");
var import_validator = require("../common/validator");
var import_page_scroll = require("../mixins/page-scroll");
const ROOT_ELEMENT = ".van-sticky";
(0, import_component.VantComponent)({
  props: {
    zIndex: {
      type: Number,
      value: 99
    },
    offsetTop: {
      type: Number,
      value: 0,
      observer: "onScroll"
    },
    disabled: {
      type: Boolean,
      observer: "onScroll"
    },
    container: {
      type: null,
      observer: "onScroll"
    },
    scrollTop: {
      type: null,
      observer(val) {
        this.onScroll({ scrollTop: val });
      }
    }
  },
  mixins: [
    (0, import_page_scroll.pageScrollMixin)(function(event) {
      if (this.data.scrollTop != null) {
        return;
      }
      this.onScroll(event);
    })
  ],
  data: {
    height: 0,
    fixed: false,
    transform: 0
  },
  mounted() {
    this.onScroll();
  },
  methods: {
    onScroll({ scrollTop } = {}) {
      const { container, offsetTop, disabled } = this.data;
      if (disabled) {
        this.setDataAfterDiff({
          fixed: false,
          transform: 0
        });
        return;
      }
      this.scrollTop = scrollTop || this.scrollTop;
      if (typeof container === "function") {
        Promise.all([(0, import_utils.getRect)(this, ROOT_ELEMENT), this.getContainerRect()]).then(([root, container2]) => {
          if (offsetTop + root.height > container2.height + container2.top) {
            this.setDataAfterDiff({
              fixed: false,
              transform: container2.height - root.height
            });
          } else if (offsetTop >= root.top) {
            this.setDataAfterDiff({
              fixed: true,
              height: root.height,
              transform: 0
            });
          } else {
            this.setDataAfterDiff({ fixed: false, transform: 0 });
          }
        }).catch(() => {
        });
        return;
      }
      (0, import_utils.getRect)(this, ROOT_ELEMENT).then((root) => {
        if (!(0, import_validator.isDef)(root) || !root.width && !root.height) {
          return;
        }
        if (offsetTop >= root.top) {
          this.setDataAfterDiff({ fixed: true, height: root.height });
          this.transform = 0;
        } else {
          this.setDataAfterDiff({ fixed: false });
        }
      });
    },
    setDataAfterDiff(data) {
      wx.nextTick(() => {
        const diff = Object.keys(data).reduce((prev, key) => {
          if (data[key] !== this.data[key]) {
            prev[key] = data[key];
          }
          return prev;
        }, {});
        if (Object.keys(diff).length > 0) {
          this.setData(diff);
        }
        this.$emit("scroll", {
          scrollTop: this.scrollTop,
          isFixed: data.fixed || this.data.fixed
        });
      });
    },
    getContainerRect() {
      const nodesRef = this.data.container();
      if (!nodesRef) {
        return Promise.reject(new Error("not found container"));
      }
      return new Promise((resolve) => nodesRef.boundingClientRect(resolve).exec());
    }
  }
});
