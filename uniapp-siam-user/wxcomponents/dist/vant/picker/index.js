var import_component = require("../common/component");
var import_shared = require("./shared");
(0, import_component.VantComponent)({
  classes: ["active-class", "toolbar-class", "column-class"],
  props: Object.assign(Object.assign({}, import_shared.pickerProps), { valueKey: {
    type: String,
    value: "text"
  }, toolbarPosition: {
    type: String,
    value: "top"
  }, defaultIndex: {
    type: Number,
    value: 0
  }, columns: {
    type: Array,
    value: [],
    observer(columns = []) {
      this.simple = columns.length && !columns[0].values;
      if (Array.isArray(this.children) && this.children.length) {
        this.setColumns().catch(() => {
        });
      }
    }
  } }),
  beforeCreate() {
    Object.defineProperty(this, "children", {
      get: () => this.selectAllComponents(".van-picker__column") || []
    });
  },
  methods: {
    noop() {
    },
    setColumns() {
      const { data } = this;
      const columns = this.simple ? [{ values: data.columns }] : data.columns;
      const stack = columns.map((column, index) => this.setColumnValues(index, column.values));
      return Promise.all(stack);
    },
    emit(event) {
      const { type } = event.currentTarget.dataset;
      if (this.simple) {
        this.$emit(type, {
          value: this.getColumnValue(0),
          index: this.getColumnIndex(0)
        });
      } else {
        this.$emit(type, {
          value: this.getValues(),
          index: this.getIndexes()
        });
      }
    },
    onChange(event) {
      if (this.simple) {
        this.$emit("change", {
          picker: this,
          value: this.getColumnValue(0),
          index: this.getColumnIndex(0)
        });
      } else {
        this.$emit("change", {
          picker: this,
          value: this.getValues(),
          index: event.currentTarget.dataset.index
        });
      }
    },
    // get column instance by index
    getColumn(index) {
      return this.children[index];
    },
    // get column value by index
    getColumnValue(index) {
      const column = this.getColumn(index);
      return column && column.getValue();
    },
    // set column value by index
    setColumnValue(index, value) {
      const column = this.getColumn(index);
      if (column == null) {
        return Promise.reject(new Error("setColumnValue: \u5BF9\u5E94\u5217\u4E0D\u5B58\u5728"));
      }
      return column.setValue(value);
    },
    // get column option index by column index
    getColumnIndex(columnIndex) {
      return (this.getColumn(columnIndex) || {}).data.currentIndex;
    },
    // set column option index by column index
    setColumnIndex(columnIndex, optionIndex) {
      const column = this.getColumn(columnIndex);
      if (column == null) {
        return Promise.reject(new Error("setColumnIndex: \u5BF9\u5E94\u5217\u4E0D\u5B58\u5728"));
      }
      return column.setIndex(optionIndex);
    },
    // get options of column by index
    getColumnValues(index) {
      return (this.children[index] || {}).data.options;
    },
    // set options of column by index
    setColumnValues(index, options, needReset = true) {
      const column = this.children[index];
      if (column == null) {
        return Promise.reject(new Error("setColumnValues: \u5BF9\u5E94\u5217\u4E0D\u5B58\u5728"));
      }
      const isSame = JSON.stringify(column.data.options) === JSON.stringify(options);
      if (isSame) {
        return Promise.resolve();
      }
      return column.set({ options }).then(() => {
        if (needReset) {
          column.setIndex(0);
        }
      });
    },
    // get values of all columns
    getValues() {
      return this.children.map((child) => child.getValue());
    },
    // set values of all columns
    setValues(values) {
      const stack = values.map((value, index) => this.setColumnValue(index, value));
      return Promise.all(stack);
    },
    // get indexes of all columns
    getIndexes() {
      return this.children.map((child) => child.data.currentIndex);
    },
    // set indexes of all columns
    setIndexes(indexes) {
      const stack = indexes.map((optionIndex, columnIndex) => this.setColumnIndex(columnIndex, optionIndex));
      return Promise.all(stack);
    }
  }
});
