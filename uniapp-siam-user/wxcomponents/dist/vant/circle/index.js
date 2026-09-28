var import_color = require("../common/color");
var import_component = require("../common/component");
var import_utils = require("../common/utils");
var import_validator = require("../common/validator");
var import_version = require("../common/version");
var import_canvas = require("./canvas");
function format(rate) {
  return Math.min(Math.max(rate, 0), 100);
}
const PERIMETER = 2 * Math.PI;
const BEGIN_ANGLE = -Math.PI / 2;
const STEP = 1;
(0, import_component.VantComponent)({
  props: {
    text: String,
    lineCap: {
      type: String,
      value: "round"
    },
    value: {
      type: Number,
      value: 0,
      observer: "reRender"
    },
    speed: {
      type: Number,
      value: 50
    },
    size: {
      type: Number,
      value: 100,
      observer() {
        this.drawCircle(this.currentValue);
      }
    },
    fill: String,
    layerColor: {
      type: String,
      value: import_color.WHITE
    },
    color: {
      type: null,
      value: import_color.BLUE,
      observer() {
        this.setHoverColor().then(() => {
          this.drawCircle(this.currentValue);
        });
      }
    },
    type: {
      type: String,
      value: ""
    },
    strokeWidth: {
      type: Number,
      value: 4
    },
    clockwise: {
      type: Boolean,
      value: true
    }
  },
  data: {
    hoverColor: import_color.BLUE
  },
  methods: {
    getContext() {
      const { type, size } = this.data;
      if (type === "" || !(0, import_version.canIUseCanvas2d)()) {
        const ctx = wx.createCanvasContext("van-circle", this);
        return Promise.resolve(ctx);
      }
      const dpr = (0, import_utils.getSystemInfoSync)().pixelRatio;
      return new Promise((resolve) => {
        wx.createSelectorQuery().in(this).select("#van-circle").node().exec((res) => {
          const canvas = res[0].node;
          const ctx = canvas.getContext(type);
          if (!this.inited) {
            this.inited = true;
            canvas.width = size * dpr;
            canvas.height = size * dpr;
            ctx.scale(dpr, dpr);
          }
          resolve((0, import_canvas.adaptor)(ctx));
        });
      });
    },
    setHoverColor() {
      const { color, size } = this.data;
      if ((0, import_validator.isObj)(color)) {
        return this.getContext().then((context) => {
          if (!context)
            return;
          const LinearColor = context.createLinearGradient(size, 0, 0, 0);
          Object.keys(color).sort((a, b) => parseFloat(a) - parseFloat(b)).map((key) => LinearColor.addColorStop(parseFloat(key) / 100, color[key]));
          this.hoverColor = LinearColor;
        });
      }
      this.hoverColor = color;
      return Promise.resolve();
    },
    presetCanvas(context, strokeStyle, beginAngle, endAngle, fill) {
      const { strokeWidth, lineCap, clockwise, size } = this.data;
      const position = size / 2;
      const radius = position - strokeWidth / 2;
      context.setStrokeStyle(strokeStyle);
      context.setLineWidth(strokeWidth);
      context.setLineCap(lineCap);
      context.beginPath();
      context.arc(position, position, radius, beginAngle, endAngle, !clockwise);
      context.stroke();
      if (fill) {
        context.setFillStyle(fill);
        context.fill();
      }
    },
    renderLayerCircle(context) {
      const { layerColor, fill } = this.data;
      this.presetCanvas(context, layerColor, 0, PERIMETER, fill);
    },
    renderHoverCircle(context, formatValue) {
      const { clockwise } = this.data;
      const progress = PERIMETER * (formatValue / 100);
      const endAngle = clockwise ? BEGIN_ANGLE + progress : 3 * Math.PI - (BEGIN_ANGLE + progress);
      this.presetCanvas(context, this.hoverColor, BEGIN_ANGLE, endAngle);
    },
    drawCircle(currentValue) {
      const { size } = this.data;
      this.getContext().then((context) => {
        if (!context)
          return;
        context.clearRect(0, 0, size, size);
        this.renderLayerCircle(context);
        const formatValue = format(currentValue);
        if (formatValue !== 0) {
          this.renderHoverCircle(context, formatValue);
        }
        context.draw();
      });
    },
    reRender() {
      const { value, speed } = this.data;
      if (speed <= 0 || speed > 1e3) {
        this.drawCircle(value);
        return;
      }
      this.clearMockInterval();
      this.currentValue = this.currentValue || 0;
      const run = () => {
        this.interval = setTimeout(() => {
          if (this.currentValue !== value) {
            if (Math.abs(this.currentValue - value) < STEP) {
              this.currentValue = value;
            } else if (this.currentValue < value) {
              this.currentValue += STEP;
            } else {
              this.currentValue -= STEP;
            }
            this.drawCircle(this.currentValue);
            run();
          } else {
            this.clearMockInterval();
          }
        }, 1e3 / speed);
      };
      run();
    },
    clearMockInterval() {
      if (this.interval) {
        clearTimeout(this.interval);
        this.interval = null;
      }
    }
  },
  mounted() {
    this.currentValue = this.data.value;
    this.setHoverColor().then(() => {
      this.drawCircle(this.currentValue);
    });
  },
  destroyed() {
    this.clearMockInterval();
  }
});
