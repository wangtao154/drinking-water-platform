<template>
  <div class="thresholds-page">
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
        <el-table-column prop="metricCode" label="测点编码" min-width="120" />
        <el-table-column prop="metricName" label="测点名称" min-width="120" />
        <el-table-column prop="thresholdValue" label="阈值值" min-width="100" />
        <el-table-column prop="operator" label="运算符" min-width="80" />
        <el-table-column prop="alertLevel" label="告警级别" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.alertLevel)">{{ statusLabel(row.alertLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="启用状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
          </template>
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

    <!-- 编辑弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑阈值" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="阈值值">
          <el-input-number v-model="editForm.thresholdValue" :precision="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="运算符">
          <el-select v-model="editForm.operator">
            <el-option label="大于" value="GT" />
            <el-option label="大于等于" value="GE" />
            <el-option label="小于" value="LT" />
            <el-option label="小于等于" value="LE" />
            <el-option label="等于" value="EQ" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警级别">
          <el-select v-model="editForm.alertLevel">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="警告" value="WARNING" />
            <el-option label="信息" value="INFO" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageThresholds, updateThreshold } from '@/api/monitor'
import type { ThresholdVO } from '@/api/monitor'
import { statusLabel, statusTagType } from '@/utils/format'

const tableData = ref<ThresholdVO[]>([])
const showEditDialog = ref(false)
const currentRow = ref<ThresholdVO | null>(null)

const searchForm = reactive({})
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const editForm = reactive({ thresholdValue: 0, operator: '', alertLevel: '' })

async function loadData() {
  try {
    const res = await pageThresholds({ pageNum: pagination.pageNum, pageSize: pagination.pageSize })
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

function openEditDialog(row: ThresholdVO) {
  currentRow.value = row
  editForm.thresholdValue = row.thresholdValue
  editForm.operator = row.operator
  editForm.alertLevel = row.alertLevel
  showEditDialog.value = true
}

async function handleUpdate() {
  try {
    await updateThreshold(currentRow.value!.id, editForm)
    ElMessage.success('更新成功')
    showEditDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('更新失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.thresholds-page {
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
