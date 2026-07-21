<template>
  <div class="inventory-batches-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="批次类型">
          <el-select v-model="searchForm.batchType" placeholder="全部" clearable style="width: 160px">
            <el-option label="全部" value="" />
            <el-option label="设备入库" value="DEVICE_INBOUND" />
            <el-option label="滤芯入库" value="FILTER_INBOUND" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-button type="primary" @click="handleCreate">新增批次</el-button>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="batchNo" label="批次号" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ statusLabel(row.batchType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalQuantity" label="总数" width="100" align="center" />
        <el-table-column prop="remainingQuantity" label="剩余数" width="100" align="center" />
        <el-table-column label="仓库类型" width="120" align="center">
          <template #default="{ row }">
            {{ statusLabel(row.warehouseType) }}
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.remark || '-' }}
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

    <!-- 新增弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="新增批次"
      width="500px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="类型" prop="batchType">
          <el-select v-model="form.batchType" placeholder="请选择类型" style="width: 100%">
            <el-option label="设备入库" value="DEVICE_INBOUND" />
            <el-option label="滤芯入库" value="FILTER_INBOUND" />
          </el-select>
        </el-form-item>
        <el-form-item label="总数" prop="totalQuantity">
          <el-input-number v-model="form.totalQuantity" :min="1" placeholder="请输入总数" style="width: 100%" />
        </el-form-item>
        <el-form-item label="仓库类型" prop="warehouseType">
          <el-select v-model="form.warehouseType" placeholder="请选择仓库类型" style="width: 100%">
            <el-option label="中心仓" value="CENTRAL" />
            <el-option label="区域仓" value="REGIONAL" />
            <el-option label="经销商仓" value="DEALER" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pageBatches, createBatch } from '@/api/inventory'
import type { StockBatchVO } from '@/types/api'
import { formatDateTime, statusLabel } from '@/utils/format'

const loading = ref(false)
const tableData = ref<StockBatchVO[]>([])
const total = ref(0)

const searchForm = reactive({
  batchType: '',
  pageNum: 1,
  pageSize: 10,
})

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
    }
    if (searchForm.batchType) params.batchType = searchForm.batchType

    const res = await pageBatches(params)
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
  searchForm.batchType = ''
  searchForm.pageNum = 1
  fetchData()
}

// ===== 弹窗 =====
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const submitLoading = ref(false)

const form = reactive({
  batchType: '',
  totalQuantity: 1,
  warehouseType: '',
  remark: '',
})

const rules: FormRules = {
  batchType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  totalQuantity: [{ required: true, message: '请输入总数', trigger: 'blur' }],
  warehouseType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
}

function handleCreate() {
  resetForm()
  dialogVisible.value = true
}

function handleDialogClosed() {
  resetForm()
}

function resetForm() {
  formRef.value?.resetFields()
  form.batchType = ''
  form.totalQuantity = 1
  form.warehouseType = ''
  form.remark = ''
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = {
      batchType: form.batchType,
      totalQuantity: form.totalQuantity,
      warehouseType: form.warehouseType,
      remark: form.remark,
    }

    const res = await createBatch(data)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      fetchData()
    }
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.inventory-batches-page {
  padding: 16px;

  .search-card {
    margin-bottom: 16px;
  }

  .toolbar-card {
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
