var import_component = require("../common/component");
var import_utils = require("./utils");
var import_shared = require("./shared");
var import_validator = require("../common/validator");
(0, import_component.VantComponent)({
  props: Object.assign(Object.assign(Object.assign(Object.assign({ disabled: Boolean, multiple: Boolean, uploadText: String, useBeforeRead: Boolean, afterRead: null, beforeRead: null, previewSize: {
    type: null,
    value: 80
  }, name: {
    type: null,
    value: ""
  }, accept: {
    type: String,
    value: "image"
  }, fileList: {
    type: Array,
    value: [],
    observer: "formatFileList"
  }, maxSize: {
    type: Number,
    value: Number.MAX_VALUE
  }, maxCount: {
    type: Number,
    value: 100
  }, deletable: {
    type: Boolean,
    value: true
  }, showUpload: {
    type: Boolean,
    value: true
  }, previewImage: {
    type: Boolean,
    value: true
  }, previewFullImage: {
    type: Boolean,
    value: true
  }, videoFit: {
    type: String,
    value: "contain"
  }, imageFit: {
    type: String,
    value: "scaleToFill"
  }, uploadIcon: {
    type: String,
    value: "photograph"
  } }, import_shared.imageProps), import_shared.videoProps), import_shared.mediaProps), import_shared.messageFileProps),
  data: {
    lists: [],
    isInCount: true
  },
  methods: {
    formatFileList() {
      const { fileList = [], maxCount } = this.data;
      const lists = fileList.map((item) => Object.assign(Object.assign({}, item), { isImage: (0, import_utils.isImageFile)(item), isVideo: (0, import_utils.isVideoFile)(item), deletable: (0, import_validator.isBoolean)(item.deletable) ? item.deletable : true }));
      this.setData({ lists, isInCount: lists.length < maxCount });
    },
    getDetail(index) {
      return {
        name: this.data.name,
        index: index == null ? this.data.fileList.length : index
      };
    },
    startUpload() {
      const { maxCount, multiple, lists, disabled } = this.data;
      if (disabled)
        return;
      (0, import_utils.chooseFile)(Object.assign(Object.assign({}, this.data), { maxCount: maxCount - lists.length })).then((res) => {
        this.onBeforeRead(multiple ? res : res[0]);
      }).catch((error) => {
        this.$emit("error", error);
      });
    },
    onBeforeRead(file) {
      const { beforeRead, useBeforeRead } = this.data;
      let res = true;
      if (typeof beforeRead === "function") {
        res = beforeRead(file, this.getDetail());
      }
      if (useBeforeRead) {
        res = new Promise((resolve, reject) => {
          this.$emit("before-read", Object.assign(Object.assign({ file }, this.getDetail()), { callback: (ok) => {
            ok ? resolve() : reject();
          } }));
        });
      }
      if (!res) {
        return;
      }
      if ((0, import_validator.isPromise)(res)) {
        res.then((data) => this.onAfterRead(data || file));
      } else {
        this.onAfterRead(file);
      }
    },
    onAfterRead(file) {
      const { maxSize, afterRead } = this.data;
      const oversize = Array.isArray(file) ? file.some((item) => item.size > maxSize) : file.size > maxSize;
      if (oversize) {
        this.$emit("oversize", Object.assign({ file }, this.getDetail()));
        return;
      }
      if (typeof afterRead === "function") {
        afterRead(file, this.getDetail());
      }
      this.$emit("after-read", Object.assign({ file }, this.getDetail()));
    },
    deleteItem(event) {
      const { index } = event.currentTarget.dataset;
      this.$emit("delete", Object.assign(Object.assign({}, this.getDetail(index)), { file: this.data.fileList[index] }));
    },
    onPreviewImage(event) {
      if (!this.data.previewFullImage)
        return;
      const { index } = event.currentTarget.dataset;
      const { lists, showmenu } = this.data;
      const item = lists[index];
      wx.previewImage({
        urls: lists.filter((item2) => (0, import_utils.isImageFile)(item2)).map((item2) => item2.url),
        current: item.url,
        showmenu,
        fail() {
          wx.showToast({ title: "\u9884\u89C8\u56FE\u7247\u5931\u8D25", icon: "none" });
        }
      });
    },
    onPreviewVideo(event) {
      if (!this.data.previewFullImage)
        return;
      const { index } = event.currentTarget.dataset;
      const { lists } = this.data;
      const sources = [];
      const current = lists.reduce((sum, cur, curIndex) => {
        if (!(0, import_utils.isVideoFile)(cur)) {
          return sum;
        }
        sources.push(Object.assign(Object.assign({}, cur), { type: "video" }));
        if (curIndex < index) {
          sum++;
        }
        return sum;
      }, 0);
      wx.previewMedia({
        sources,
        current,
        fail() {
          wx.showToast({ title: "\u9884\u89C8\u89C6\u9891\u5931\u8D25", icon: "none" });
        }
      });
    },
    onPreviewFile(event) {
      const { index } = event.currentTarget.dataset;
      wx.openDocument({
        filePath: this.data.lists[index].url,
        showMenu: true
      });
    },
    onClickPreview(event) {
      const { index } = event.currentTarget.dataset;
      const item = this.data.lists[index];
      this.$emit("click-preview", Object.assign(Object.assign({}, item), this.getDetail(index)));
    }
  }
});
