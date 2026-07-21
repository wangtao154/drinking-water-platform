<template>
  <div class="order-list-page">
    <!-- 统计卡片 -->
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

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="订单号">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入订单号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.orderStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="PENDING" />
            <el-option label="已支付" value="PAID" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="退款中" value="REFUNDING" />
            <el-option label="已退款" value="REFUNDED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备ID" min-width="120" show-overflow-tooltip />
        <el-table-column prop="packageName" label="套餐名" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.orderStatus)" size="small">
              {{ statusLabel(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            ¥{{ fenToYuan(row.payAmount) }}
          </template>
        </el-table-column>
        <el-table-column prop="payMethod" label="支付方式" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.payMethod || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="支付时间" min-width="170">
          <template #default="{ row }">
            {{ row.payTime ? formatDateTime(row.payTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.orderStatus === 'PENDING'"
              title="确认支付该订单？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handlePay(row.orderNo)"
            >
              <template #reference>
                <el-button type="success" link size="small">支付</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              v-if="row.orderStatus === 'PENDING'"
              title="确认取消该订单？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleCancel(row.orderNo)"
            >
              <template #reference>
                <el-button type="danger" link size="small">取消</el-button>
              </template>
            </el-popconfirm>
            <span v-if="row.orderStatus !== 'PENDING'">-</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="fetchData"
          @size-change="fetchData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageOrders, payOrder, cancelOrder, getOrderStatistics } from '@/api/order'
import type { OrderVO, OrderStatisticsVO } from '@/types/api'
import { formatDateTime, statusLabel, statusTagType, fenToYuan } from '@/utils/format'

const loading = ref(false)
const tableData = ref<OrderVO[]>([])
const total = ref(0)
const statistics = reactive<OrderStatisticsVO>({
  total: 0,
  pending: 0,
  paid: 0,
  cancelled: 0,
  refunding: 0,
  refunded: 0,
  totalAmount: 0,
  refundAmount: 0,
})

const searchForm = reactive({
  keyword: '',
  orderStatus: '',
  pageNum: 1,
  pageSize: 10,
})

async function fetchStatistics() {
  try {
    const res = await getOrderStatistics()
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  } catch {
    // ignore
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.orderStatus) params.orderStatus = searchForm.orderStatus

    const res = await pageOrders(params)
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
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.orderStatus = ''
  searchForm.pageNum = 1
  fetchData()
}

async function handlePay(orderNo: string) {
  try {
    const res = await payOrder(orderNo)
    if (res.code === 200) {
      ElMessage.success('支付成功')
      fetchData()
      fetchStatistics()
    }
  } catch {
    // error handled by interceptor
  }
}

async function handleCancel(orderNo: string) {
  try {
    const res = await cancelOrder(orderNo)
    if (res.code === 200) {
      ElMessage.success('取消成功')
      fetchData()
      fetchStatistics()
    }
  } catch {
    // error handled by interceptor
  }
}

onMounted(() => {
  fetchData()
  fetchStatistics()
})
</script>

<style scoped lang="scss">
.order-list-page {
  padding: 16px;

  .stat-row {
    margin-bottom: 16px;

    .stat-card {
      .stat-title {
        font-size: 13px;
        color: #909399;
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
