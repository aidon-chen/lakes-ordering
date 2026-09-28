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
var utils_exports = {};
__export(utils_exports, {
  isSameSecond: () => isSameSecond,
  parseFormat: () => parseFormat,
  parseTimeData: () => parseTimeData
});
module.exports = __toCommonJS(utils_exports);
function padZero(num, targetLength = 2) {
  let str = num + "";
  while (str.length < targetLength) {
    str = "0" + str;
  }
  return str;
}
const SECOND = 1e3;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;
function parseTimeData(time) {
  const days = Math.floor(time / DAY);
  const hours = Math.floor(time % DAY / HOUR);
  const minutes = Math.floor(time % HOUR / MINUTE);
  const seconds = Math.floor(time % MINUTE / SECOND);
  const milliseconds = Math.floor(time % SECOND);
  return {
    days,
    hours,
    minutes,
    seconds,
    milliseconds
  };
}
function parseFormat(format, timeData) {
  const { days } = timeData;
  let { hours, minutes, seconds, milliseconds } = timeData;
  if (format.indexOf("DD") === -1) {
    hours += days * 24;
  } else {
    format = format.replace("DD", padZero(days));
  }
  if (format.indexOf("HH") === -1) {
    minutes += hours * 60;
  } else {
    format = format.replace("HH", padZero(hours));
  }
  if (format.indexOf("mm") === -1) {
    seconds += minutes * 60;
  } else {
    format = format.replace("mm", padZero(minutes));
  }
  if (format.indexOf("ss") === -1) {
    milliseconds += seconds * 1e3;
  } else {
    format = format.replace("ss", padZero(seconds));
  }
  return format.replace("SSS", padZero(milliseconds, 3));
}
function isSameSecond(time1, time2) {
  return Math.floor(time1 / 1e3) === Math.floor(time2 / 1e3);
}
