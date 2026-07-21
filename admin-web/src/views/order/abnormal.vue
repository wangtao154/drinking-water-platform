<template>
  <div class="order-abnormal-page">
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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%" :row-class-name="rowClassName">
        <el-table-column prop="orderNo" label="订单号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="deviceId" label="设备ID" min-width="120" show-overflow-tooltip />
        <el-table-column prop="packageName" label="套餐名" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.orderStatus)" size="small" effect="dark">
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
import { pageOrders } from '@/api/order'
import type { OrderVO } from '@/types/api'
import { formatDateTime, statusLabel, statusTagType, fenToYuan } from '@/utils/format'

const ABNORMAL_STATUSES = 'CANCELLED,REFUNDING,REFUNDED'

const loading = ref(false)
const tableData = ref<OrderVO[]>([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 10,
})

function rowClassName({ row }: { row: OrderVO }) {
  if (row.orderStatus === 'REFUNDING') return 'warning-row'
  if (row.orderStatus === 'REFUNDED') return 'danger-row'
  if (row.orderStatus === 'CANCELLED') return 'info-row'
  return ''
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
      orderStatus: ABNORMAL_STATUSES,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword

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
  searchForm.pageNum = 1
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.order-abnormal-page {
  padding: 16px;

  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    .pagination-wrapper {
      display: flex;
      justify-content: flex-end;
      margin-top: 16px;
    }

    :deep(.warning-row) {
      background-color: #fdf6ec;
    }

    :deep(.danger-row) {
      background-color: #fef0f0;
    }

    :deep(.info-row) {
      background-color: #f4f4f5;
    }
  }
}
</style>
