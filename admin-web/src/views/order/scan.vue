<template>
  <div class="scan-order-page">
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">总订单</div>
          <div class="stat-value" style="color: #409eff">{{ statistics.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">已支付</div>
          <div class="stat-value" style="color: #67c23a">{{ statistics.paid }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">待支付</div>
          <div class="stat-value" style="color: #e6a23c">{{ statistics.pending }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">总金额</div>
          <div class="stat-value" style="color: #f56c6c">¥{{ fenToYuan(statistics.totalAmount) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="关键字">
          <el-input
            v-model="searchForm.keyword"
            placeholder="订单号 / SN / 设备ID / 交易号"
            clearable
            style="width: 260px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="支付状态">
          <el-select v-model="searchForm.payStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="PENDING" />
            <el-option label="已支付" value="SUCCESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="出水状态">
          <el-select v-model="searchForm.dispenseStatus" placeholder="全部" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="已支付待下发" value="PAID" />
            <el-option label="已下发" value="DISPATCHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="命令状态">
          <el-select v-model="searchForm.commandStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="待下发" value="PENDING" />
            <el-option label="已发送" value="SENT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="190" show-overflow-tooltip />
        <el-table-column prop="sn" label="SN" min-width="110" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备ID" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deviceId || '-' }}</template>
        </el-table-column>
        <el-table-column label="出水量" width="100" align="right">
          <template #default="{ row }">{{ row.targetMl || 0 }} ml</template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="{ row }">¥{{ fenToYuan(row.payAmount) }}</template>
        </el-table-column>
        <el-table-column label="支付状态" width="105" align="center">
          <template #default="{ row }">
            <el-tag :type="payStatusTag(row.payStatus)" size="small">{{ payStatusLabel(row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="出水状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="dispenseStatusTag(row.dispenseStatus)" size="small">
              {{ dispenseStatusLabel(row.dispenseStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="命令状态" width="105" align="center">
          <template #default="{ row }">
            <el-tag :type="commandStatusTag(row.commandStatus)" size="small">
              {{ commandStatusLabel(row.commandStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="110">
          <template #default="{ row }">{{ paymentProviderLabel(row.paymentProvider) }}</template>
        </el-table-column>
        <el-table-column prop="transactionId" label="微信交易号" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.transactionId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="q74Payload" label="Q74内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.q74Payload || '-' }}</template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="170">
          <template #default="{ row }">{{ row.paidAt ? formatDateTime(row.paidAt) : '-' }}</template>
        </el-table-column>
        <el-table-column label="下发时间" min-width="170">
          <template #default="{ row }">{{ row.commandSentAt ? formatDateTime(row.commandSentAt) : '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="fetchData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getScanOrderStatistics, pageScanOrders } from '@/api/scanOrder'
import type { ScanOrderStatsVO, ScanOrderVO } from '@/types/api'
import { fenToYuan, formatDateTime } from '@/utils/format'

type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'

const loading = ref(false)
const tableData = ref<ScanOrderVO[]>([])
const total = ref(0)
const statistics = reactive<ScanOrderStatsVO>({
  total: 0,
  paid: 0,
  pending: 0,
  dispatched: 0,
  sent: 0,
  totalAmount: 0,
})

const searchForm = reactive({
  keyword: '',
  payStatus: '',
  dispenseStatus: '',
  commandStatus: '',
  pageNum: 1,
  pageSize: 10,
})

async function fetchStatistics() {
  try {
    const res = await getScanOrderStatistics()
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  } catch {
    // handled by request interceptor
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await pageScanOrders({
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
      keyword: searchForm.keyword || undefined,
      payStatus: searchForm.payStatus || undefined,
      dispenseStatus: searchForm.dispenseStatus || undefined,
      commandStatus: searchForm.commandStatus || undefined,
    })
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchForm.pageNum = 1
  fetchData()
  fetchStatistics()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.payStatus = ''
  searchForm.dispenseStatus = ''
  searchForm.commandStatus = ''
  searchForm.pageNum = 1
  fetchData()
  fetchStatistics()
}

function handleSizeChange() {
  searchForm.pageNum = 1
  fetchData()
}

function payStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待支付',
    SUCCESS: '已支付',
  }
  return map[status] || status || '-'
}

function payStatusTag(status: string): TagType {
  if (status === 'SUCCESS') return 'success'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

function dispenseStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING_PAY: '待支付',
    PAID: '待下发',
    DISPATCHED: '已下发',
  }
  return map[status] || status || '-'
}

function dispenseStatusTag(status: string): TagType {
  if (status === 'DISPATCHED') return 'success'
  if (status === 'PAID') return 'warning'
  if (status === 'PENDING_PAY') return 'info'
  return 'info'
}

function commandStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待下发',
    SENT: '已发送',
  }
  return map[status] || status || '-'
}

function commandStatusTag(status: string): TagType {
  if (status === 'SENT') return 'success'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

function paymentProviderLabel(provider?: string): string {
  const map: Record<string, string> = {
    WECHAT: '微信支付',
    MOCK: '模拟支付',
  }
  return provider ? (map[provider] || provider) : '-'
}

onMounted(() => {
  fetchData()
  fetchStatistics()
})
</script>

<style scoped lang="scss">
.scan-order-page {
  padding: 16px;

  .stat-row {
    margin-bottom: 16px;

    .stat-card {
      .stat-title {
        color: #909399;
        font-size: 13px;
        margin-bottom: 8px;
      }

      .stat-value {
        font-size: 28px;
        font-weight: 700;
      }
    }
  }

  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    .pagination-wrapper {
      display: flex;
      justify-content: flex-end;
      margin-top: 16px;
    }
  }
}
</style>
