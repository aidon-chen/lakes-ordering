var import_component = require("../../../common/component");
var import_utils = require("../../utils");
(0, import_component.VantComponent)({
  props: {
    date: {
      type: null,
      observer: "setDays"
    },
    type: {
      type: String,
      observer: "setDays"
    },
    color: String,
    minDate: {
      type: null,
      observer: "setDays"
    },
    maxDate: {
      type: null,
      observer: "setDays"
    },
    showMark: Boolean,
    rowHeight: null,
    formatter: {
      type: null,
      observer: "setDays"
    },
    currentDate: {
      type: null,
      observer: "setDays"
    },
    firstDayOfWeek: {
      type: Number,
      observer: "setDays"
    },
    allowSameDay: Boolean,
    showSubtitle: Boolean,
    showMonthTitle: Boolean
  },
  data: {
    visible: true,
    days: []
  },
  methods: {
    onClick(event) {
      const { index } = event.currentTarget.dataset;
      const item = this.data.days[index];
      if (item.type !== "disabled") {
        this.$emit("click", item);
      }
    },
    setDays() {
      const days = [];
      const startDate = new Date(this.data.date);
      const year = startDate.getFullYear();
      const month = startDate.getMonth();
      const totalDay = (0, import_utils.getMonthEndDay)(startDate.getFullYear(), startDate.getMonth() + 1);
      for (let day = 1; day <= totalDay; day++) {
        const date = new Date(year, month, day);
        const type = this.getDayType(date);
        let config = {
          date,
          type,
          text: day,
          bottomInfo: this.getBottomInfo(type)
        };
        if (this.data.formatter) {
          config = this.data.formatter(config);
        }
        days.push(config);
      }
      this.setData({ days });
    },
    getMultipleDayType(day) {
      const { currentDate } = this.data;
      if (!Array.isArray(currentDate)) {
        return "";
      }
      const isSelected = (date) => currentDate.some((item) => (0, import_utils.compareDay)(item, date) === 0);
      if (isSelected(day)) {
        const prevDay = (0, import_utils.getPrevDay)(day);
        const nextDay = (0, import_utils.getNextDay)(day);
        const prevSelected = isSelected(prevDay);
        const nextSelected = isSelected(nextDay);
        if (prevSelected && nextSelected) {
          return "multiple-middle";
        }
        if (prevSelected) {
          return "end";
        }
        return nextSelected ? "start" : "multiple-selected";
      }
      return "";
    },
    getRangeDayType(day) {
      const { currentDate, allowSameDay } = this.data;
      if (!Array.isArray(currentDate)) {
        return "";
      }
      const [startDay, endDay] = currentDate;
      if (!startDay) {
        return "";
      }
      const compareToStart = (0, import_utils.compareDay)(day, startDay);
      if (!endDay) {
        return compareToStart === 0 ? "start" : "";
      }
      const compareToEnd = (0, import_utils.compareDay)(day, endDay);
      if (compareToStart === 0 && compareToEnd === 0 && allowSameDay) {
        return "start-end";
      }
      if (compareToStart === 0) {
        return "start";
      }
      if (compareToEnd === 0) {
        return "end";
      }
      if (compareToStart > 0 && compareToEnd < 0) {
        return "middle";
      }
      return "";
    },
    getDayType(day) {
      const { type, minDate, maxDate, currentDate } = this.data;
      if ((0, import_utils.compareDay)(day, minDate) < 0 || (0, import_utils.compareDay)(day, maxDate) > 0) {
        return "disabled";
      }
      if (type === "single") {
        return (0, import_utils.compareDay)(day, currentDate) === 0 ? "selected" : "";
      }
      if (type === "multiple") {
        return this.getMultipleDayType(day);
      }
      if (type === "range") {
        return this.getRangeDayType(day);
      }
      return "";
    },
    getBottomInfo(type) {
      if (this.data.type === "range") {
        if (type === "start") {
          return "\u5F00\u59CB";
        }
        if (type === "end") {
          return "\u7ED3\u675F";
        }
        if (type === "start-end") {
          return "\u5F00\u59CB/\u7ED3\u675F";
        }
      }
    }
  }
});
