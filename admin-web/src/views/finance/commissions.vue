<template>
  <div class="commissions-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="已结算" value="SETTLED" />
            <el-option label="待结算" value="UNSETTLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" stripe border>
        <el-table-column prop="commissionNo" label="分佣编号" min-width="140" />
        <el-table-column prop="orderNo" label="订单号" min-width="140" />
        <el-table-column prop="dealerName" label="经销商名" min-width="120" />
        <el-table-column prop="amount" label="金额" min-width="100">
          <template #default="{ row }">{{ fenToYuan(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="settledAt" label="结算时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.settledAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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
import { pageCommissions } from '@/api/finance'
import type { CommissionVO } from '@/api/finance'
import { formatDateTime, statusLabel, statusTagType, fenToYuan } from '@/utils/format'

const tableData = ref<CommissionVO[]>([])
const searchForm = reactive({ status: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadData() {
  try {
    const res = await pageCommissions({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, status: searchForm.status || undefined })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e) {
    ElMessage.error('加载列表失败')
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

function handleReset() {
  searchForm.status = ''
  pagination.pageNum = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.commissions-page {
  padding: 16px;
}
.search-card {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
