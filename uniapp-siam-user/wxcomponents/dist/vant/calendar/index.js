var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));
var import_component = require("../common/component");
var import_utils = require("./utils");
var import_toast = __toESM(require("../toast/toast"));
var import_utils2 = require("../common/utils");
const initialMinDate = (0, import_utils.getToday)().getTime();
const initialMaxDate = (() => {
  const now = (0, import_utils.getToday)();
  return new Date(now.getFullYear(), now.getMonth() + 6, now.getDate()).getTime();
})();
const getTime = (date) => date instanceof Date ? date.getTime() : date;
(0, import_component.VantComponent)({
  props: {
    title: {
      type: String,
      value: "\u65E5\u671F\u9009\u62E9"
    },
    color: String,
    show: {
      type: Boolean,
      observer(val) {
        if (val) {
          this.initRect();
          this.scrollIntoView();
        }
      }
    },
    formatter: null,
    confirmText: {
      type: String,
      value: "\u786E\u5B9A"
    },
    confirmDisabledText: {
      type: String,
      value: "\u786E\u5B9A"
    },
    rangePrompt: String,
    showRangePrompt: {
      type: Boolean,
      value: true
    },
    defaultDate: {
      type: null,
      value: (0, import_utils.getToday)().getTime(),
      observer(val) {
        this.setData({ currentDate: val });
        this.scrollIntoView();
      }
    },
    allowSameDay: Boolean,
    type: {
      type: String,
      value: "single",
      observer: "reset"
    },
    minDate: {
      type: Number,
      value: initialMinDate
    },
    maxDate: {
      type: Number,
      value: initialMaxDate
    },
    position: {
      type: String,
      value: "bottom"
    },
    rowHeight: {
      type: null,
      value: import_utils.ROW_HEIGHT
    },
    round: {
      type: Boolean,
      value: true
    },
    poppable: {
      type: Boolean,
      value: true
    },
    showMark: {
      type: Boolean,
      value: true
    },
    showTitle: {
      type: Boolean,
      value: true
    },
    showConfirm: {
      type: Boolean,
      value: true
    },
    showSubtitle: {
      type: Boolean,
      value: true
    },
    safeAreaInsetBottom: {
      type: Boolean,
      value: true
    },
    closeOnClickOverlay: {
      type: Boolean,
      value: true
    },
    maxRange: {
      type: null,
      value: null
    },
    minRange: {
      type: Number,
      value: 1
    },
    firstDayOfWeek: {
      type: Number,
      value: 0
    },
    readonly: Boolean
  },
  data: {
    subtitle: "",
    currentDate: null,
    scrollIntoView: ""
  },
  watch: {
    minDate() {
      this.initRect();
    },
    maxDate() {
      this.initRect();
    }
  },
  created() {
    this.setData({
      currentDate: this.getInitialDate(this.data.defaultDate)
    });
  },
  mounted() {
    if (this.data.show || !this.data.poppable) {
      this.initRect();
      this.scrollIntoView();
    }
  },
  methods: {
    reset() {
      this.setData({ currentDate: this.getInitialDate(this.data.defaultDate) });
      this.scrollIntoView();
    },
    initRect() {
      if (this.contentObserver != null) {
        this.contentObserver.disconnect();
      }
      const contentObserver = this.createIntersectionObserver({
        thresholds: [0, 0.1, 0.9, 1],
        observeAll: true
      });
      this.contentObserver = contentObserver;
      contentObserver.relativeTo(".van-calendar__body");
      contentObserver.observe(".month", (res) => {
        if (res.boundingClientRect.top <= res.relativeRect.top) {
          this.setData({ subtitle: (0, import_utils.formatMonthTitle)(res.dataset.date) });
        }
      });
    },
    limitDateRange(date, minDate = null, maxDate = null) {
      minDate = minDate || this.data.minDate;
      maxDate = maxDate || this.data.maxDate;
      if ((0, import_utils.compareDay)(date, minDate) === -1) {
        return minDate;
      }
      if ((0, import_utils.compareDay)(date, maxDate) === 1) {
        return maxDate;
      }
      return date;
    },
    getInitialDate(defaultDate = null) {
      const { type, minDate, maxDate, allowSameDay } = this.data;
      if (!defaultDate)
        return [];
      const now = (0, import_utils.getToday)().getTime();
      if (type === "range") {
        if (!Array.isArray(defaultDate)) {
          defaultDate = [];
        }
        const [startDay, endDay] = defaultDate || [];
        const startDate = getTime(startDay || now);
        const start = this.limitDateRange(startDate, minDate, allowSameDay ? startDate : (0, import_utils.getPrevDay)(new Date(maxDate)).getTime());
        const date = getTime(endDay || now);
        const end = this.limitDateRange(date, allowSameDay ? date : (0, import_utils.getNextDay)(new Date(minDate)).getTime());
        return [start, end];
      }
      if (type === "multiple") {
        if (Array.isArray(defaultDate)) {
          return defaultDate.map((date) => this.limitDateRange(date));
        }
        return [this.limitDateRange(now)];
      }
      if (!defaultDate || Array.isArray(defaultDate)) {
        defaultDate = now;
      }
      return this.limitDateRange(defaultDate);
    },
    scrollIntoView() {
      (0, import_utils2.requestAnimationFrame)(() => {
        const { currentDate, type, show, poppable, minDate, maxDate } = this.data;
        if (!currentDate)
          return;
        const targetDate = type === "single" ? currentDate : currentDate[0];
        const displayed = show || !poppable;
        if (!targetDate || !displayed) {
          return;
        }
        const months = (0, import_utils.getMonths)(minDate, maxDate);
        months.some((month, index) => {
          if ((0, import_utils.compareMonth)(month, targetDate) === 0) {
            this.setData({ scrollIntoView: `month${index}` });
            return true;
          }
          return false;
        });
      });
    },
    onOpen() {
      this.$emit("open");
    },
    onOpened() {
      this.$emit("opened");
    },
    onClose() {
      this.$emit("close");
    },
    onClosed() {
      this.$emit("closed");
    },
    onClickDay(event) {
      if (this.data.readonly) {
        return;
      }
      let { date } = event.detail;
      const { type, currentDate, allowSameDay } = this.data;
      if (type === "range") {
        const [startDay, endDay] = currentDate;
        if (startDay && !endDay) {
          const compareToStart = (0, import_utils.compareDay)(date, startDay);
          if (compareToStart === 1) {
            const { days } = this.selectComponent(".month").data;
            days.some((day, index) => {
              const isDisabled = day.type === "disabled" && getTime(startDay) < getTime(day.date) && getTime(day.date) < getTime(date);
              if (isDisabled) {
                ({ date } = days[index - 1]);
              }
              return isDisabled;
            });
            this.select([startDay, date], true);
          } else if (compareToStart === -1) {
            this.select([date, null]);
          } else if (allowSameDay) {
            this.select([date, date], true);
          }
        } else {
          this.select([date, null]);
        }
      } else if (type === "multiple") {
        let selectedIndex;
        const selected = currentDate.some((dateItem, index) => {
          const equal = (0, import_utils.compareDay)(dateItem, date) === 0;
          if (equal) {
            selectedIndex = index;
          }
          return equal;
        });
        if (selected) {
          const cancelDate = currentDate.splice(selectedIndex, 1);
          this.setData({ currentDate });
          this.unselect(cancelDate);
        } else {
          this.select([...currentDate, date]);
        }
      } else {
        this.select(date, true);
      }
    },
    unselect(dateArray) {
      const date = dateArray[0];
      if (date) {
        this.$emit("unselect", (0, import_utils.copyDates)(date));
      }
    },
    select(date, complete) {
      if (complete && this.data.type === "range") {
        const valid = this.checkRange(date);
        if (!valid) {
          if (this.data.showConfirm) {
            this.emit([
              date[0],
              (0, import_utils.getDayByOffset)(date[0], this.data.maxRange - 1)
            ]);
          } else {
            this.emit(date);
          }
          return;
        }
      }
      this.emit(date);
      if (complete && !this.data.showConfirm) {
        this.onConfirm();
      }
    },
    emit(date) {
      this.setData({
        currentDate: Array.isArray(date) ? date.map(getTime) : getTime(date)
      });
      this.$emit("select", (0, import_utils.copyDates)(date));
    },
    checkRange(date) {
      const { maxRange, rangePrompt, showRangePrompt } = this.data;
      if (maxRange && (0, import_utils.calcDateNum)(date) > maxRange) {
        if (showRangePrompt) {
          (0, import_toast.default)({
            context: this,
            message: rangePrompt || `\u9009\u62E9\u5929\u6570\u4E0D\u80FD\u8D85\u8FC7 ${maxRange} \u5929`
          });
        }
        this.$emit("over-range");
        return false;
      }
      return true;
    },
    onConfirm() {
      if (this.data.type === "range" && !this.checkRange(this.data.currentDate)) {
        return;
      }
      wx.nextTick(() => {
        this.$emit("confirm", (0, import_utils.copyDates)(this.data.currentDate));
      });
    },
    onClickSubtitle(event) {
      this.$emit("click-subtitle", event);
    }
  }
});
