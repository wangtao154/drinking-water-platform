<template>
  <div class="records-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">充值总额(元)</div><div class="stat-value">{{ fenToYuan(statTotalAmount) }}</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">充值笔数</div><div class="stat-value">{{ pagination.total }}</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">已支付</div><div class="stat-value">{{ statPaidCount }}</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">待支付</div><div class="stat-value">{{ statPendingCount }}</div></div></el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.orderStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="待支付" value="PENDING" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="已退款" value="REFUNDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="订单号/客户名" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table v-loading="loading" :data="tableData" stripe border>
        <el-table-column prop="orderNo" label="充值订单号" min-width="160" />
        <el-table-column prop="customerName" label="客户名" min-width="100" />
        <el-table-column prop="payAmount" label="充值金额" min-width="100">
          <template #default="{ row }">{{ fenToYuan(row.payAmount) }} 元</template>
        </el-table-column>
        <el-table-column prop="payMethod" label="支付方式" min-width="100">
          <template #default="{ row }">{{ payMethodLabel(row.payMethod) }}</template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus)">{{ orderStatusLabel(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.payTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageRechargeRecords } from '@/api/finance'
import type { OrderVO } from '@/types/api'
import { formatDateTime, fenToYuan } from '@/utils/format'

const tableData = ref<OrderVO[]>([])
const loading = ref(false)
const searchForm = reactive({ orderStatus: '', keyword: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const statTotalAmount = ref(0)
const statPaidCount = ref(0)
const statPendingCount = ref(0)

async function loadData() {
  loading.value = true
  try {
    const res = await pageRechargeRecords({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      orderStatus: searchForm.orderStatus || undefined,
      keyword: searchForm.keyword || undefined,
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total
    // 加载全局统计数据
    fetchStatistics()
  } catch (e) {
    ElMessage.error('加载充值记录失败')
  } finally {
    loading.value = false
  }
}

// 独立查询统计数据（基于全部数据，非当前页）
async function fetchStatistics() {
  try {
    const res = await pageRechargeRecords({
      pageNum: 1,
      pageSize: 1,
      orderStatus: searchForm.orderStatus || undefined,
      keyword: searchForm.keyword || undefined,
    })
    // 用 total 查询全部数据做统计
    const totalCount = res.data.total
    if (totalCount > 0) {
      const allRes = await pageRechargeRecords({
        pageNum: 1,
        pageSize: totalCount,
        orderStatus: searchForm.orderStatus || undefined,
        keyword: searchForm.keyword || undefined,
      })
      const allRecords = allRes.data.records || []
      statTotalAmount.value = allRecords.reduce((sum, r) => sum + (r.payAmount || 0), 0)
      statPaidCount.value = allRecords.filter(r => r.orderStatus === 'PAID').length
      statPendingCount.value = allRecords.filter(r => r.orderStatus === 'PENDING').length
    } else {
      statTotalAmount.value = 0
      statPaidCount.value = 0
      statPendingCount.value = 0
    }
  } catch { /* ignore */ }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() { searchForm.orderStatus = ''; searchForm.keyword = ''; pagination.pageNum = 1; loadData() }

function orderStatusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退款' }
  return map[s] || s
}
function orderStatusTag(s: string) {
  const map: Record<string, string> = { PENDING: 'warning', PAID: 'success', CANCELLED: 'info', REFUNDED: 'danger' }
  return map[s] || ''
}
function payMethodLabel(m: string) {
  const map: Record<string, string> = { WECHAT_PAY: '微信支付', ALIPAY: '支付宝', BALANCE: '余额支付' }
  return m ? (map[m] || m) : '-'
}

onMounted(() => { loadData() })
</script>

<style scoped lang="scss">
.records-page { padding: 16px; }
.stat-row { margin-bottom: 12px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: bold; color: #409eff; }
.search-card { margin-bottom: 12px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
