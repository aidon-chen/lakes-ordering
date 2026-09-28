var import_link = require("../mixins/link");
var import_component = require("../common/component");
(0, import_component.VantComponent)({
  classes: [
    "num-class",
    "desc-class",
    "thumb-class",
    "title-class",
    "price-class",
    "origin-price-class"
  ],
  mixins: [import_link.link],
  props: {
    tag: String,
    num: String,
    desc: String,
    thumb: String,
    title: String,
    price: {
      type: String,
      observer: "updatePrice"
    },
    centered: Boolean,
    lazyLoad: Boolean,
    thumbLink: String,
    originPrice: String,
    thumbMode: {
      type: String,
      value: "aspectFit"
    },
    currency: {
      type: String,
      value: "\xA5"
    }
  },
  methods: {
    updatePrice() {
      const { price } = this.data;
      const priceArr = price.toString().split(".");
      this.setData({
        integerStr: priceArr[0],
        decimalStr: priceArr[1] ? `.${priceArr[1]}` : ""
      });
    },
    onClickThumb() {
      this.jumpLink("thumbLink");
    }
  }
});
