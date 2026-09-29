<template>
  <div class="app-container">
    <!-- 四指标卡：点击跳转对应列表 -->
    <div class="total-layout">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="total-frame" @click="gotoPage('/todayOrderList')">
            <img src="../../assets/home_order.png" class="total-icon">
            <div class="total-title">今日订单数</div>
            <div class="total-value">{{todayList.dayCountPaid || 0}}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="total-frame" @click="gotoPage('/merchantBillingRecord')">
            <img src="../../assets/home_today_amount.png" class="total-icon">
            <div class="total-title">今日实收金额</div>
            <div class="total-value">￥{{todayList.daySumActualPrice || 0}}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="total-frame" @click="gotoPage('/todayOrderList')">
            <img src="../../assets/home_order.png" class="total-icon">
            <div class="total-title">进行中订单</div>
            <div class="total-value">{{todayList.unCompletedNum || 0}}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="total-frame" @click="gotoPage('/refundOrderList')">
            <img src="../../assets/home_order.png" class="total-icon">
            <div class="total-title">待处理退款</div>
            <div class="total-value">{{todayList.waitHandleRefundNum || 0}}</div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 账户相关数据 -->
    <div class="section-layout">
      <div class="layout-title">账户相关数据</div>
      <div class="section-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="section-item">
              <span class="section-item-title">今日进账</span>
              <el-tag type="success" class="section-item-tag">￥{{todayList.daySumIncome || 0}}</el-tag>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="section-item">
              <span class="section-item-title">今日出账</span>
              <el-tag type="danger" class="section-item-tag">￥{{todayList.daySumExpense || 0}}</el-tag>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>

    <!-- 金额相关数据 -->
    <div class="section-layout">
      <div class="layout-title">金额相关数据</div>
      <div class="section-content">
        <el-row>
          <el-col :span="8" class="section-item-value">{{todayList.totalActualPrice || 0}}</el-col>
          <el-col :span="8" class="section-item-value">{{todayList.totalPackingCharges || 0}}</el-col>
          <el-col :span="8" class="section-item-value">{{todayList.totalRefundAmount || 0}}</el-col>
        </el-row>
        <el-row class="font-medium">
          <el-col :span="8" class="section-item-title-center">商品实收金额(元)</el-col>
          <el-col :span="8" class="section-item-title-center">包装费金额(元)</el-col>
          <el-col :span="8" class="section-item-title-center">退款金额(元)</el-col>
        </el-row>
      </div>
    </div>

    <!-- 待处理事项 -->
    <div class="section-layout">
      <div class="layout-title">待处理事项</div>
      <div class="section-content">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="section-item" @click="gotoPage('/refundOrderList')">
              <span class="font-medium">待处理退款申请</span>
              <el-tag type="danger" class="section-item-tag">{{todayList.handleOrderRefundCount || 0}}</el-tag>
            </div>
          </el-col>
          <!-- 评价体系不在一期范围，入口一并隐藏（2026-09-29）；恢复时取消注释即可 -->
          <!-- <el-col :span="12">
            <div class="section-item" @click="gotoPage('/appraiseList')">
              <span class="font-medium">待处理评价回复</span>
              <el-tag type="warning" class="section-item-tag">{{todayList.waitReplyAppraiseCount || 0}}</el-tag>
            </div>
          </el-col> -->
        </el-row>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'statisticGraph',
    data() {
      return {
        todayList: {}, //今日数据（实时数据看板）
      }
    },
    created() {
      let vue = this
      //查询基础统计数据（管理端口径）
      //整对象赋值以触发视图更新（Vue2 无法侦测对象新增属性）
      vue.$http.post(vue, '/rest/admin/statistics/todayStatistic', {},
        (vue, data) => {
          vue.todayList = {
            dayCountPaid: data.data.dayCountPaid,
            daySumActualPrice: data.data.daySumActualPrice,
            unCompletedNum: data.data.unCompletedNum,
            waitHandleRefundNum: data.data.waitHandleRefundNum,
            daySumIncome: data.data.daySumIncome,
            daySumExpense: data.data.daySumExpense,
            totalActualPrice: data.data.totalActualPrice,
            totalPackingCharges: data.data.totalPackingCharges,
            totalRefundAmount: data.data.totalRefundAmount,
            handleOrderRefundCount: data.data.handleOrderRefundCount,
            waitReplyAppraiseCount: data.data.waitReplyAppraiseCount
          };
        }, (error, data) => {
          vue.$message({
            showClose: true,
            message: data.message,
            type: 'error'
          });
        }
      )
    },
    methods: {
      //指标卡/待办项作为入口，点击跳转对应列表
      gotoPage(path) {
        this.$router.push({path: path})
      }
    }
  }
</script>

<style scoped>
  .app-container {
    margin-top: 20px;
  }

  .total-layout {
    margin-top: 20px;
  }

  .total-frame {
    border: 1px solid #DCDFE6;
    padding: 20px;
    height: 100px;
    cursor: pointer;
  }

  .total-icon {
    color: #409EFF;
    width: 60px;
    height: 60px;
  }

  .total-title {
    position: relative;
    font-size: 16px;
    color: #909399;
    left: 70px;
    top: -50px;
  }

  .total-value {
    position: relative;
    font-size: 18px;
    color: #606266;
    left: 70px;
    top: -40px;
  }

  .section-layout {
    margin-top: 20px;
    border: 1px solid #DCDFE6;
  }

  .layout-title {
    color: #606266;
    padding: 15px 20px;
    background: #F2F6FC;
    font-weight: bold;
  }

  .section-content {
    padding: 20px 40px;
  }

  .section-item {
    border-bottom: 1px solid #EBEEF5;
    padding: 10px;
    font-size: 20px;
    cursor: pointer;
  }

  .section-item-title {
    font-size: 20px;
  }

  .section-item-tag {
    float: right;
    font-size: 18px;
  }

  .section-item-value {
    font-size: 24px;
    text-align: center;
    color: #f56c6c;
  }

  .section-item-title-center {
    margin-top: 10px;
    text-align: center;
  }

  .font-medium {
    font-size: 14px;
  }
</style>