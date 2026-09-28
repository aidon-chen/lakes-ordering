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
var relation_exports = {};
__export(relation_exports, {
  useChildren: () => useChildren,
  useParent: () => useParent
});
module.exports = __toCommonJS(relation_exports);
function useParent(name, onEffect) {
  const path = `../${name}/index`;
  return {
    relations: {
      [path]: {
        type: "ancestor",
        linked() {
          onEffect && onEffect.call(this);
        },
        linkChanged() {
          onEffect && onEffect.call(this);
        },
        unlinked() {
          onEffect && onEffect.call(this);
        }
      }
    },
    mixin: Behavior({
      created() {
        Object.defineProperty(this, "parent", {
          get: () => this.getRelationNodes(path)[0]
        });
        Object.defineProperty(this, "index", {
          // @ts-ignore
          get: () => {
            var _a, _b;
            return (_b = (_a = this.parent) === null || _a === void 0 ? void 0 : _a.children) === null || _b === void 0 ? void 0 : _b.indexOf(this);
          }
        });
      }
    })
  };
}
function useChildren(name, onEffect) {
  const path = `../${name}/index`;
  return {
    relations: {
      [path]: {
        type: "descendant",
        linked(target) {
          onEffect && onEffect.call(this, target);
        },
        linkChanged(target) {
          onEffect && onEffect.call(this, target);
        },
        unlinked(target) {
          onEffect && onEffect.call(this, target);
        }
      }
    },
    mixin: Behavior({
      created() {
        Object.defineProperty(this, "children", {
          get: () => this.getRelationNodes(path) || []
        });
      }
    })
  };
}
