<template>
  <section>
    <el-tabs v-model="activeTab" @tab-click="onTabClick">
      <!-- ==================== 台位列表 ==================== -->
      <el-tab-pane label="台位列表" name="table">
        <el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
          <el-form :inline="true" :model="searchMsg">
            <el-form-item>
              <el-input v-model="searchMsg.tableNo" placeholder="桌号/桌名搜索" clearable @keyup.enter.native="getList(1)"></el-input>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchMsg.typeId" placeholder="类型" clearable style="width: 110px;">
                <el-option v-for="t in typeOptions" :key="t.id" :label="t.name" :value="t.id"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchMsg.zoneId" placeholder="区域" clearable style="width: 120px;">
                <el-option v-for="z in zoneOptions" :key="z.id" :label="z.name" :value="z.id"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="searchMsg.status" placeholder="状态" clearable style="width: 100px;">
                <el-option label="空闲" :value="1"></el-option>
                <el-option label="占用" :value="2"></el-option>
                <el-option label="停用" :value="3"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="getList(1)">查询</el-button>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="openEdit({})">新增台位</el-button>
            </el-form-item>
            <el-form-item>
              <el-button type="warning" :disabled="!multipleSelection.length" @click="batchGenerateQrcode">批量生成桌码</el-button>
            </el-form-item>
          </el-form>
        </el-col>

        <el-table :data="list" v-loading="listLoading" @selection-change="sels = $event"
                  highlight-current-row style="width: 100%;" :cell-style="cellStyle" :header-cell-style="headerCellStyle">
          <el-table-column type="selection" width="45"></el-table-column>
          <el-table-column type="index" label="序号" width="55">
            <template scope="scope">
              <span>{{(searchMsg.pageNo - 1) * searchMsg.pageSize + scope.$index + 1}}</span>
            </template>
          </el-table-column>
          <el-table-column prop="tableNo" label="桌号" width="90"></el-table-column>
          <el-table-column prop="tableName" label="桌名" min-width="100"></el-table-column>
          <el-table-column prop="typeName" label="类型" width="80"></el-table-column>
          <el-table-column prop="zoneName" label="区域" width="90"></el-table-column>
          <el-table-column prop="capacity" label="容纳人数" width="80">
            <template scope="scope">{{scope.row.capacity}}人</template>
          </el-table-column>
          <el-table-column label="状态" width="70">
            <template scope="scope">
              <el-tag :type="statusTag(scope.row.status)">{{statusText(scope.row.status)}}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="桌码" width="90">
            <template scope="scope">
              <el-tag v-if="scope.row.qrScene" type="success" size="mini">已生成</el-tag>
              <el-tag v-else type="info" size="mini">未生成</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280">
            <template scope="scope">
              <el-button size="mini" type="text" @click="openEdit(scope.row)">编辑</el-button>
              <el-button size="mini" type="text" @click="generateQrcode(scope.row)">生成码</el-button>
              <el-button size="mini" type="text" @click="viewQrcode(scope.row)">查看码</el-button>
              <el-button size="mini" type="text" @click="downloadQrcode(scope.row)">下载</el-button>
              <el-button size="mini" type="text" @click="invalidateQrcode(scope.row)">作废</el-button>
              <el-button size="mini" type="text" v-if="scope.row.status === 3" @click="enableTable(scope.row)">启用</el-button>
              <el-button size="mini" type="text" v-else @click="disableTable(scope.row)">停用</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-col :span="24" class="toolbar">
          <el-pagination layout="total, prev, pager, next" :total="total" :page-size="searchMsg.pageSize"
                         :current-page="searchMsg.pageNo" @current-change="getList"></el-pagination>
        </el-col>
      </el-tab-pane>

      <!-- ==================== 台位类型 ==================== -->
      <el-tab-pane label="台位类型" name="type">
        <el-col :span="24" class="toolbar" style="padding-bottom: 0px;">
          <el-button type="primary" @click="openTypeEdit({})">新增类型</el-button>
        </el-col>
        <el-table :data="typeList" v-loading="typeLoading" highlight-current-row style="width: 100%;">
          <el-table-column type="index" label="序号" width="55"></el-table-column>
          <el-table-column prop="name" label="类型名称" width="120"></el-table-column>
          <el-table-column prop="minConsume" label="低消(元,仅展示)" width="130"></el-table-column>
          <el-table-column prop="description" label="说明" min-width="150"></el-table-column>
          <el-table-column prop="sortNumber" label="排序" width="70"></el-table-column>
          <el-table-column label="操作" width="130">
            <template scope="scope">
              <el-button size="mini" type="text" @click="openTypeEdit(scope.row)">编辑</el-button>
              <el-button size="mini" type="text" @click="deleteType(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 台位新增/编辑弹窗 -->
    <el-dialog :title="editForm.id ? '编辑台位' : '新增台位'" :visible.sync="editVisible" width="480px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="桌号" required>
          <el-input v-model="editForm.tableNo" placeholder="如 A01（店内唯一）"></el-input>
        </el-form-item>
        <el-form-item label="桌名" required>
          <el-input v-model="editForm.tableName" placeholder="如 A区·A01"></el-input>
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="editForm.typeId" style="width:100%;">
            <el-option v-for="t in typeOptions" :key="t.id" :label="t.name" :value="t.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="区域" required>
          <el-select v-model="editForm.zoneId" style="width:100%;">
            <el-option v-for="z in zoneOptions" :key="z.id" :label="z.name" :value="z.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="容纳人数">
          <el-input-number v-model="editForm.capacity" :min="1" :max="99"></el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTable">保存</el-button>
      </div>
    </el-dialog>

    <!-- 类型新增/编辑弹窗 -->
    <el-dialog :title="typeForm.id ? '编辑类型' : '新增类型'" :visible.sync="typeEditVisible" width="480px" :close-on-click-modal="false">
      <el-form :model="typeForm" label-width="90px">
        <el-form-item label="类型名称" required>
          <el-input v-model="typeForm.name" placeholder="如 散台/卡座/包间"></el-input>
        </el-form-item>
        <el-form-item label="低消(元)">
          <el-input-number v-model="typeForm.minConsume" :min="0" :precision="2" :step="50"></el-input-number>
          <span style="color:#999;margin-left:8px;">仅展示，不参与校验</span>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="typeForm.description"></el-input>
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="typeForm.sortNumber" :min="1"></el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="typeEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveType">保存</el-button>
      </div>
    </el-dialog>

    <!-- 桌码预览弹窗（按需取图，不做列表缩略图批量加载） -->
    <el-dialog :title="'桌码预览 - ' + qrPreview.tableNo" :visible.sync="qrVisible" width="420px">
      <div style="text-align:center;">
        <img v-if="qrPreview.image" :src="qrPreview.image" style="width:320px;">
        <div v-else style="color:#999;padding:20px;">加载中…</div>
      </div>
      <div slot="footer">
        <el-button @click="qrVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadQrcode(qrPreview)">下载 PNG</el-button>
      </div>
    </el-dialog>
  </section>
</template>

<script>
/**
 * A-09 台位管理与桌码：台位列表 + 台位类型（单页双 Tab）
 * 后端：/rest/merchant/tableManage（档案+桌码）、/rest/merchant/tableType（类型）
 * 状态口径：1=空闲 2=占用 3=停用；桌码操作：生成/下载/作废（清码可重生成），停用/启用台位
 * 注意：$http.post 成功回调签名 (vue, data)，data 为响应体
 */
// 缓存样式对象，避免每次渲染生成新引用
const CELL_STYLE = { padding: '6px 0' };
const HEADER_CELL_STYLE = { background: '#f5f7fa', color: '#606266' };

export default {
  data() {
    return {
      activeTab: 'table',
      // ---- 台位列表 ----
      searchMsg: { pageNo: 1, pageSize: 20, tableNo: '', typeId: null, zoneId: null, status: null },
      list: [],
      total: 0,
      listLoading: false,
      sels: [],
      multipleSelection: [],
      typeOptions: [], // 类型下拉（含筛选与表单）
      zoneOptions: [], // 区域下拉（取自台位列表数据去重）
      editVisible: false,
      editForm: { id: null, tableNo: '', tableName: '', typeId: null, zoneId: null, capacity: 4 },
      // ---- 桌码预览 ----
      qrVisible: false,
      qrPreview: { tableNo: '', image: '' },
      // ---- 台位类型 ----
      typeList: [],
      typeLoading: false,
      typeEditVisible: false,
      typeForm: { id: null, name: '', minConsume: 0, description: '', sortNumber: 1 }
    }
  },
  mounted() {
    this.getList(1);
    this.getTypeList();
  },
  methods: {
    cellStyle() { return CELL_STYLE; },
    headerCellStyle() { return HEADER_CELL_STYLE; },
    statusText(s) { return s === 1 ? '空闲' : s === 2 ? '占用' : '停用'; },
    statusTag(s) { return s === 1 ? 'success' : s === 2 ? 'warning' : 'info'; },
    onTabClick(tab) {
      // 切到类型 Tab 时刷新类型（保证与台位列表数据一致）
      if (tab.name === 'type') this.getTypeList();
    },
    // 区域选项：从台位列表记录去重（区域无独立管理页，沿用 seed 数据）
    refreshZoneOptions() {
      const seen = {};
      const zones = [];
      this.list.forEach(r => {
        if (r.zoneId && !seen[r.zoneId]) {
          seen[r.zoneId] = 1;
          zones.push({ id: r.zoneId, name: r.zoneName });
        }
      });
      this.zoneOptions = zones;
    },
    getList(pageNo) {
      this.listLoading = true;
      if (pageNo) this.searchMsg.pageNo = pageNo;
      this.$http.post(this, '/rest/merchant/tableManage/list', this.searchMsg,
        (vue, data) => {
          this.list = (data.data && data.data.records) || [];
          this.total = (data.data && data.data.total) || 0;
          this.refreshZoneOptions();
          this.listLoading = false;
        },
        (error, data) => {
          this.listLoading = false;
          this.$message.error((data && data.message) || '台位列表加载失败');
        });
    },
    getTypeList() {
      this.typeLoading = true;
      this.$http.post(this, '/rest/merchant/tableType/list', {},
        (vue, data) => {
          this.typeList = data.data || [];
          this.typeOptions = this.typeList;
          this.typeLoading = false;
        },
        (error, data) => {
          this.typeLoading = false;
          this.$message.error((data && data.message) || '台位类型加载失败');
        });
    },
    // ---- 台位档案 ----
    openEdit(row) {
      // 新增：重置表单；编辑：深拷贝回显（避免引用原行）
      this.editForm = row && row.id
        ? JSON.parse(JSON.stringify(row))
        : { id: null, tableNo: '', tableName: '', typeId: this.typeOptions[0] ? this.typeOptions[0].id : null, zoneId: this.zoneOptions[0] ? this.zoneOptions[0].id : null, capacity: 4 };
      this.editVisible = true;
    },
    saveTable() {
      const f = this.editForm;
      if (!f.tableNo || !f.tableName || !f.typeId || !f.zoneId) {
        this.$message.warning('请填写桌号、桌名，并选择类型与区域');
        return;
      }
      const url = f.id ? '/rest/merchant/tableManage/update' : '/rest/merchant/tableManage/add';
      this.$http.post(this, url, f, () => {
        this.$message.success('保存成功');
        this.editVisible = false;
        this.getList(this.searchMsg.pageNo);
      }, (error, data) => {
        this.$message.error((data && data.message) || '保存失败');
      });
    },
    disableTable(row) {
      this.$confirm('停用后该台位收银端不再显示、扫码失效，确认停用？', '提示', { type: 'warning' }).then(() => {
        this.$http.post(this, '/rest/merchant/tableManage/disable', { id: row.id }, () => {
          this.$message.success('已停用');
          this.getList(this.searchMsg.pageNo);
        }, (error, data) => {
          this.$message.error((data && data.message) || '停用失败');
        });
      }).catch(() => {});
    },
    enableTable(row) {
      this.$http.post(this, '/rest/merchant/tableManage/enable', { id: row.id }, () => {
        this.$message.success('已启用');
        this.getList(this.searchMsg.pageNo);
      }, (error, data) => {
        this.$message.error((data && data.message) || '启用失败');
      });
    },
    // ---- 桌码 ----
    generateQrcode(row) {
      this.$http.post(this, '/rest/merchant/tableManage/generateQrcode', { ids: [row.id] }, () => {
        this.$message.success('桌码已生成');
        this.getList(this.searchMsg.pageNo);
      }, (error, data) => {
        this.$message.error((data && data.message) || '桌码生成失败');
      });
    },
    batchGenerateQrcode() {
      const ids = this.sels.map(s => s.id);
      if (!ids.length) return;
      this.$http.post(this, '/rest/merchant/tableManage/generateQrcode', { ids }, () => {
        this.$message.success('已生成 ' + ids.length + ' 个桌码');
        this.getList(this.searchMsg.pageNo);
      }, (error, data) => {
        this.$message.error((data && data.message) || '桌码生成失败');
      });
    },
    downloadQrcode(row) {
      if (!row.qrScene) { this.$message.warning('该台位尚未生成桌码'); return; }
      // 预览弹窗里已有图则直接下载，否则按需拉取（后端缓存/重建）
      if (this.qrPreview.image && this.qrPreview.tableNo === row.tableNo) {
        this.saveQrcodeFile(row.tableNo, this.qrPreview.image);
        return;
      }
      this.fetchQrcodeImage(row.tableNo, (img) => this.saveQrcodeFile(row.tableNo, img));
    },
    // 查看桌码（按需取图：列表不批量加载缩略图，避免冷启动时过多微信调用）
    viewQrcode(row) {
      if (!row.qrScene) { this.$message.warning('该台位尚未生成桌码'); return; }
      this.qrPreview = { tableNo: row.tableNo, image: '' };
      this.qrVisible = true;
      this.fetchQrcodeImage(row.tableNo, (img) => { this.qrPreview.image = img; });
    },
    // 拉取桌码图（后端返回 base64 data URL）
    fetchQrcodeImage(tableNo, onOk) {
      this.$http.post(this, '/rest/merchant/tableManage/downloadQrcode', { tableNo },
        (vue, data) => { onOk(data.data); },
        (error, data) => { this.$message.error((data && data.message) || '桌码获取失败'); });
    },
    // 保存 PNG 到本地，文件名含桌号（如 A01.png）
    saveQrcodeFile(tableNo, dataUrl) {
      const a = document.createElement('a');
      a.href = dataUrl;
      a.download = tableNo + '.png';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    },
    invalidateQrcode(row) {
      this.$confirm('作废后旧码扫码将失效，可重新生成新码，确认作废？', '提示', { type: 'warning' }).then(() => {
        this.$http.post(this, '/rest/merchant/tableManage/invalidateQrcode', { id: row.id }, () => {
          this.$message.success('桌码已作废');
          this.getList(this.searchMsg.pageNo);
        }, (error, data) => {
          this.$message.error((data && data.message) || '作废失败');
        });
      }).catch(() => {});
    },
    // ---- 台位类型 ----
    openTypeEdit(row) {
      this.typeForm = row && row.id
        ? JSON.parse(JSON.stringify(row))
        : { id: null, name: '', minConsume: 0, description: '', sortNumber: 1 };
      this.typeEditVisible = true;
    },
    saveType() {
      const f = this.typeForm;
      if (!f.name) { this.$message.warning('请填写类型名称'); return; }
      const url = f.id ? '/rest/merchant/tableType/update' : '/rest/merchant/tableType/add';
      this.$http.post(this, url, f, () => {
        this.$message.success('保存成功');
        this.typeEditVisible = false;
        this.getTypeList();
      }, (error, data) => {
        this.$message.error((data && data.message) || '保存失败');
      });
    },
    deleteType(row) {
      this.$confirm('删除类型前请确认该类型下无台位，确认删除？', '提示', { type: 'warning' }).then(() => {
        this.$http.post(this, '/rest/merchant/tableType/delete', { id: row.id }, () => {
          this.$message.success('已删除');
          this.getTypeList();
        }, (error, data) => {
          this.$message.error((data && data.message) || '删除失败');
        });
      }).catch(() => {});
    }
  }
}
</script>
