<template>
  <div class="audit-logs-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" stripe border>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="operation" label="操作" min-width="160" />
        <el-table-column prop="method" label="方法" min-width="120" />
        <el-table-column prop="ip" label="IP" min-width="120" />
        <el-table-column prop="duration" label="耗时(ms)" min-width="100" />
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
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
import { pageAuditLogs } from '@/api/system'
import type { AuditLogVO } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const tableData = ref<AuditLogVO[]>([])
const searchForm = reactive({})
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadData() {
  try {
    const res = await pageAuditLogs({ pageNum: pagination.pageNum, pageSize: pagination.pageSize })
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
  pagination.pageNum = 1
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.audit-logs-page {
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
