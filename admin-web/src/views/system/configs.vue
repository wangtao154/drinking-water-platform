<template>
  <div class="configs-page">
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
        <el-table-column prop="configKey" label="配置Key" min-width="180" />
        <el-table-column prop="configValue" label="配置值" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column prop="type" label="类型" min-width="100" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
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
    <el-dialog v-model="showEditDialog" title="编辑配置" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="配置值">
          <el-input v-model="editForm.configValue" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" />
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
import { pageConfigs, updateConfig } from '@/api/system'
import type { SysConfigVO } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const tableData = ref<SysConfigVO[]>([])
const showEditDialog = ref(false)
const currentRow = ref<SysConfigVO | null>(null)

const searchForm = reactive({})
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const editForm = reactive({ configValue: '', description: '' })

async function loadData() {
  try {
    const res = await pageConfigs({ pageNum: pagination.pageNum, pageSize: pagination.pageSize })
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

function openEditDialog(row: SysConfigVO) {
  currentRow.value = row
  editForm.configValue = row.configValue
  editForm.description = row.description
  showEditDialog.value = true
}

async function handleUpdate() {
  try {
    await updateConfig(currentRow.value!.id, editForm)
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
.configs-page {
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
