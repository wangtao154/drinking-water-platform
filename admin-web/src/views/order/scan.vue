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
        <el-form-item label="关键词">
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
            <el-option label="已退款" value="REFUND" />
          </el-select>
        </el-form-item>
        <el-form-item label="出水状态">
          <el-select v-model="searchForm.dispenseStatus" placeholder="全部" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="PENDING_PAY" />
            <el-option label="待下发" value="PAID" />
            <el-option label="已下发" value="DISPATCHED" />
            <el-option label="异常" value="FAILED" />
            <el-option label="已退款" value="REFUNDED" />
          </el-select>
        </el-form-item>
        <el-form-item label="命令状态">
          <el-select v-model="searchForm.commandStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="待下发" value="PENDING" />
            <el-option label="已发送" value="SENT" />
            <el-option label="已确认" value="ACK" />
            <el-option label="异常" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款状态">
          <el-select v-model="searchForm.refundStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="未退款" value="NONE" />
            <el-option label="退款中" value="PROCESSING" />
            <el-option label="已退款" value="SUCCESS" />
            <el-option label="退款失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付时间">
          <el-date-picker
            v-model="searchForm.paidAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 380px"
          />
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
        <el-table-column label="退款状态" width="115" align="center">
          <template #default="{ row }">
            <el-tag :type="refundStatusTag(row.refundStatus)" size="small">
              {{ refundStatusLabel(row.refundStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退款金额" width="110" align="right">
          <template #default="{ row }">{{ row.refundAmount ? `¥${fenToYuan(row.refundAmount)}` : '-' }}</template>
        </el-table-column>
        <el-table-column label="退款时间" min-width="170">
          <template #default="{ row }">{{ row.refundSuccessAt ? formatDateTime(row.refundSuccessAt) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="refundErrorMsg" label="退款失败原因" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.refundErrorMsg || '-' }}</template>
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
        <el-table-column label="操作" fixed="right" width="110" align="center">
          <template #default="{ row }">
            <el-button
              v-if="canRetryRefund(row)"
              link
              type="primary"
              :loading="retryingOrderNo === row.orderNo"
              @click="handleRetryRefund(row)"
            >
              重试退款
            </el-button>
            <span v-else>-</span>
          </template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getScanOrderStatistics, pageScanOrders, retryScanOrderRefund } from '@/api/scanOrder'
import type { ScanOrderStatsVO, ScanOrderVO } from '@/types/api'
import { fenToYuan, formatDateTime } from '@/utils/format'

type TagType = 'success' | 'warning' | 'danger' | 'info' | 'primary'

const loading = ref(false)
const retryingOrderNo = ref('')
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
  refundStatus: '',
  paidAtRange: [] as string[],
  pageNum: 1,
  pageSize: 10,
})

function buildSearchParams() {
  return {
    keyword: searchForm.keyword || undefined,
    payStatus: searchForm.payStatus || undefined,
    dispenseStatus: searchForm.dispenseStatus || undefined,
    commandStatus: searchForm.commandStatus || undefined,
    refundStatus: searchForm.refundStatus || undefined,
    paidStartTime: searchForm.paidAtRange?.[0] || undefined,
    paidEndTime: searchForm.paidAtRange?.[1] || undefined,
  }
}

async function fetchStatistics() {
  try {
    const res = await getScanOrderStatistics(buildSearchParams())
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  } catch {
    // request interceptor already shows the error
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await pageScanOrders({
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
      ...buildSearchParams(),
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
  searchForm.refundStatus = ''
  searchForm.paidAtRange = []
  searchForm.pageNum = 1
  fetchData()
  fetchStatistics()
}

function handleSizeChange() {
  searchForm.pageNum = 1
  fetchData()
}

function canRetryRefund(row: ScanOrderVO): boolean {
  return row.payStatus === 'SUCCESS'
    && row.dispenseStatus === 'FAILED'
    && row.commandStatus === 'FAILED'
    && (!row.refundStatus || row.refundStatus === 'NONE' || row.refundStatus === 'FAILED')
}

async function handleRetryRefund(row: ScanOrderVO) {
  try {
    await ElMessageBox.confirm(`确认对订单 ${row.orderNo} 重新发起退款？`, '重试退款', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  retryingOrderNo.value = row.orderNo
  try {
    const res = await retryScanOrderRefund(row.orderNo)
    if (res.code === 200) {
      ElMessage.success('已重新发起退款')
      await fetchData()
      await fetchStatistics()
    }
  } finally {
    retryingOrderNo.value = ''
  }
}

function payStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待支付',
    SUCCESS: '已支付',
    REFUND: '已退款',
    FAIL: '支付失败',
  }
  return map[status] || status || '-'
}

function payStatusTag(status: string): TagType {
  if (status === 'SUCCESS') return 'success'
  if (status === 'REFUND') return 'info'
  if (status === 'PENDING') return 'warning'
  if (status === 'FAIL') return 'danger'
  return 'info'
}

function dispenseStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING_PAY: '待支付',
    PAID: '待下发',
    DISPATCHED: '已下发',
    FAILED: '异常',
    REFUNDED: '已退款',
  }
  return map[status] || status || '-'
}

function dispenseStatusTag(status: string): TagType {
  if (status === 'DISPATCHED') return 'success'
  if (status === 'REFUNDED') return 'info'
  if (status === 'FAILED') return 'danger'
  if (status === 'PAID') return 'warning'
  if (status === 'PENDING_PAY') return 'info'
  return 'info'
}

function commandStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待下发',
    SENT: '已发送',
    ACK: '已确认',
    FAILED: '异常',
  }
  return map[status] || status || '-'
}

function commandStatusTag(status: string): TagType {
  if (status === 'SENT' || status === 'ACK') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'info'
}

function refundStatusLabel(status?: string): string {
  const map: Record<string, string> = {
    NONE: '未退款',
    PROCESSING: '退款中',
    SUCCESS: '已退款',
    FAILED: '退款失败',
  }
  return status ? (map[status] || status) : '未退款'
}

function refundStatusTag(status?: string): TagType {
  if (status === 'SUCCESS') return 'success'
  if (status === 'PROCESSING') return 'warning'
  if (status === 'FAILED') return 'danger'
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
