var import_component = require("../common/component");
var import_utils = require("./utils");
function simpleTick(fn) {
  return setTimeout(fn, 30);
}
(0, import_component.VantComponent)({
  props: {
    useSlot: Boolean,
    millisecond: Boolean,
    time: {
      type: Number,
      observer: "reset"
    },
    format: {
      type: String,
      value: "HH:mm:ss"
    },
    autoStart: {
      type: Boolean,
      value: true
    }
  },
  data: {
    timeData: (0, import_utils.parseTimeData)(0),
    formattedTime: "0"
  },
  destroyed() {
    clearTimeout(this.tid);
    this.tid = null;
  },
  methods: {
    // 开始
    start() {
      if (this.counting) {
        return;
      }
      this.counting = true;
      this.endTime = Date.now() + this.remain;
      this.tick();
    },
    // 暂停
    pause() {
      this.counting = false;
      clearTimeout(this.tid);
    },
    // 重置
    reset() {
      this.pause();
      this.remain = this.data.time;
      this.setRemain(this.remain);
      if (this.data.autoStart) {
        this.start();
      }
    },
    tick() {
      if (this.data.millisecond) {
        this.microTick();
      } else {
        this.macroTick();
      }
    },
    microTick() {
      this.tid = simpleTick(() => {
        this.setRemain(this.getRemain());
        if (this.remain !== 0) {
          this.microTick();
        }
      });
    },
    macroTick() {
      this.tid = simpleTick(() => {
        const remain = this.getRemain();
        if (!(0, import_utils.isSameSecond)(remain, this.remain) || remain === 0) {
          this.setRemain(remain);
        }
        if (this.remain !== 0) {
          this.macroTick();
        }
      });
    },
    getRemain() {
      return Math.max(this.endTime - Date.now(), 0);
    },
    setRemain(remain) {
      this.remain = remain;
      const timeData = (0, import_utils.parseTimeData)(remain);
      if (this.data.useSlot) {
        this.$emit("change", timeData);
      }
      this.setData({
        formattedTime: (0, import_utils.parseFormat)(this.data.format, timeData)
      });
      if (remain === 0) {
        this.pause();
        this.$emit("finish");
      }
    }
  }
});
