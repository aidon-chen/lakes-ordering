var import_component = require("../../../common/component");
(0, import_component.VantComponent)({
  props: {
    title: {
      type: String,
      value: "\u65E5\u671F\u9009\u62E9"
    },
    subtitle: String,
    showTitle: Boolean,
    showSubtitle: Boolean,
    firstDayOfWeek: {
      type: Number,
      observer: "initWeekDay"
    }
  },
  data: {
    weekdays: []
  },
  created() {
    this.initWeekDay();
  },
  methods: {
    initWeekDay() {
      const defaultWeeks = ["\u65E5", "\u4E00", "\u4E8C", "\u4E09", "\u56DB", "\u4E94", "\u516D"];
      const firstDayOfWeek = this.data.firstDayOfWeek || 0;
      this.setData({
        weekdays: [
          ...defaultWeeks.slice(firstDayOfWeek, 7),
          ...defaultWeeks.slice(0, firstDayOfWeek)
        ]
      });
    },
    onClickSubtitle(event) {
      this.$emit("click-subtitle", event);
    }
  }
});
