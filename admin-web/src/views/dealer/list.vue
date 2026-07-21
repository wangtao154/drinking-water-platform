<template>
  <div class="dealer-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="经销商名称 / 编码 / 联系人"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 -->
    <el-card shadow="never" class="toolbar-card">
      <el-button type="primary" @click="handleCreate">新增经销商</el-button>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="dealerName" label="经销商名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="dealerCode" label="编码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="contactPerson" label="联系人" min-width="100" show-overflow-tooltip />
        <el-table-column prop="contactPhone" label="联系电话" min-width="130" show-overflow-tooltip />
        <el-table-column label="等级" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-popconfirm
              title="确定要删除该经销商吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
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
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
      @closed="handleDialogClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="经销商名称" prop="dealerName">
          <el-input v-model="form.dealerName" placeholder="请输入经销商名称" clearable />
        </el-form-item>
        <el-form-item label="编码" prop="dealerCode">
          <el-input v-model="form.dealerCode" placeholder="请输入编码" clearable />
        </el-form-item>
        <el-form-item label="联系人" prop="contactPerson">
          <el-input v-model="form.contactPerson" placeholder="请输入联系人" clearable />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" clearable />
        </el-form-item>
        <el-form-item label="等级" prop="level">
          <el-input-number v-model="form.level" :min="0" :max="10" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pageDealers, createDealer, updateDealer, deleteDealer } from '@/api/user'
import type { DealerVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref<DealerVO[]>([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 10,
})

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword

    const res = await pageDealers(params)
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
  loadData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.pageNum = 1
  loadData()
}

// ===== 弹窗 =====
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const submitLoading = ref(false)

const dialogTitle = computed(() => (editId.value ? '编辑经销商' : '新增经销商'))

const form = reactive({
  dealerName: '',
  dealerCode: '',
  contactPerson: '',
  contactPhone: '',
  level: 1,
  address: '',
})

const rules: FormRules = {
  dealerName: [{ required: true, message: '请输入经销商名称', trigger: 'blur' }],
  dealerCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  level: [{ required: true, message: '请输入等级', trigger: 'blur' }],
}

function handleCreate() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: DealerVO) {
  editId.value = row.id
  form.dealerName = row.dealerName
  form.dealerCode = row.dealerCode
  form.contactPerson = row.contactPerson
  form.contactPhone = row.contactPhone
  form.level = row.level
  form.address = row.address || ''
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    const res = await deleteDealer(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch {
    // error handled by interceptor
  }
}

function handleDialogClosed() {
  resetForm()
}

function resetForm() {
  formRef.value?.resetFields()
  form.dealerName = ''
  form.dealerCode = ''
  form.contactPerson = ''
  form.contactPhone = ''
  form.level = 1
  form.address = ''
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = {
      dealerName: form.dealerName,
      dealerCode: form.dealerCode,
      contactPerson: form.contactPerson,
      contactPhone: form.contactPhone,
      level: form.level,
      address: form.address,
    }

    if (editId.value) {
      const res = await updateDealer(editId.value, data)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else {
      const res = await createDealer(data)
      if (res.code === 200) {
        ElMessage.success('创建成功')
        dialogVisible.value = false
        loadData()
      }
    }
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.dealer-list-page {
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
