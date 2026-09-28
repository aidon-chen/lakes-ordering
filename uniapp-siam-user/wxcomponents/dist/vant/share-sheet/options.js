var import_component = require("../common/component");
(0, import_component.VantComponent)({
  props: {
    options: Array,
    showBorder: Boolean
  },
  methods: {
    onSelect(event) {
      const { index } = event.currentTarget.dataset;
      const option = this.data.options[index];
      this.$emit("select", Object.assign(Object.assign({}, option), { index }));
    }
  }
});
