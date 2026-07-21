<template>
  <div class="inventory-devices-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">设备总数</div>
          <div class="stat-value" style="color: #409eff">{{ statistics.totalDevices }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">在库</div>
          <div class="stat-value" style="color: #67c23a">{{ statistics.inStockDevices }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">已分配</div>
          <div class="stat-value" style="color: #e6a23c">{{ statistics.allocatedDevices }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="仓库类型">
          <el-select v-model="searchForm.warehouseType" placeholder="全部" clearable style="width: 160px">
            <el-option label="全部" value="" />
            <el-option label="中心仓" value="CENTRAL" />
            <el-option label="区域仓" value="REGIONAL" />
            <el-option label="经销商仓" value="DEALER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="在库" value="IN_STOCK" />
            <el-option label="已分配" value="ALLOCATED" />
            <el-option label="已出库" value="OUT" />
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
        <el-table-column prop="deviceId" label="设备ID" min-width="120" show-overflow-tooltip />
        <el-table-column prop="sn" label="SN" min-width="150" show-overflow-tooltip />
        <el-table-column prop="modelName" label="型号名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="仓库类型" width="120" align="center">
          <template #default="{ row }">
            {{ statusLabel(row.warehouseType) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
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
import { getInventoryStatistics, pageDeviceStocks } from '@/api/inventory'
import type { DeviceStockVO, InventoryStatisticsVO } from '@/types/api'
import { formatDateTime, statusLabel, statusTagType } from '@/utils/format'

const loading = ref(false)
const tableData = ref<DeviceStockVO[]>([])
const total = ref(0)
const statistics = reactive<InventoryStatisticsVO>({
  totalDevices: 0,
  inStockDevices: 0,
  allocatedDevices: 0,
  totalFilters: 0,
  inStockFilters: 0,
  lowStockFilters: 0,
})

const searchForm = reactive({
  warehouseType: '',
  status: '',
  pageNum: 1,
  pageSize: 10,
})

async function fetchStatistics() {
  try {
    const res = await getInventoryStatistics()
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
    if (searchForm.warehouseType) params.warehouseType = searchForm.warehouseType
    if (searchForm.status) params.status = searchForm.status

    const res = await pageDeviceStocks(params)
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
  searchForm.warehouseType = ''
  searchForm.status = ''
  searchForm.pageNum = 1
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchStatistics()
})
</script>

<style scoped lang="scss">
.inventory-devices-page {
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
