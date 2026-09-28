var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __export = (target, all) => {
  for (var name in all)
    __defProp(target, name, { get: all[name], enumerable: true });
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);
var component_exports = {};
__export(component_exports, {
  VantComponent: () => VantComponent
});
module.exports = __toCommonJS(component_exports);
var import_basic = require("../mixins/basic");
function mapKeys(source, target, map) {
  Object.keys(map).forEach((key) => {
    if (source[key]) {
      target[map[key]] = source[key];
    }
  });
}
function VantComponent(vantOptions) {
  const options = {};
  mapKeys(vantOptions, options, {
    data: "data",
    props: "properties",
    watch: "observers",
    mixins: "behaviors",
    methods: "methods",
    beforeCreate: "created",
    created: "attached",
    mounted: "ready",
    destroyed: "detached",
    classes: "externalClasses"
  });
  options.externalClasses = options.externalClasses || [];
  options.externalClasses.push("custom-class");
  options.behaviors = options.behaviors || [];
  options.behaviors.push(import_basic.basic);
  const { relation } = vantOptions;
  if (relation) {
    options.relations = relation.relations;
    options.behaviors.push(relation.mixin);
  }
  if (vantOptions.field) {
    options.behaviors.push("wx://form-field");
  }
  options.options = {
    multipleSlots: true,
    addGlobalClass: true
  };
  Component(options);
}
