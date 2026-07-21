<template>
  <div class="worker-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="姓名">
          <el-input
            v-model="searchForm.name"
            placeholder="请输入姓名"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="searchForm.phone"
            placeholder="请输入手机号"
            clearable
            style="width: 180px"
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
      <el-button type="primary" @click="handleCreate">新增运维人员</el-button>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="name" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="所属经销商" min-width="120">
          <template #default="{ row }">
            {{ getDealerName(row.dealerId) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="80" align="center">
          <template #default="{ row }">
            {{ row.rating ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="serviceCount" label="服务次数" width="100" align="center" />
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm
              title="确定重置密码为手机号后6位吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleResetPassword(row.id)"
            >
              <template #reference>
                <el-button type="warning" link size="small">重置密码</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              title="确定要删除该运维人员吗？"
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
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item v-if="!editId" label="密码" prop="password">
          <el-input v-model="form.password" placeholder="初始密码（建议手机号后6位）" clearable />
        </el-form-item>
        <el-form-item label="所属经销商" prop="dealerId">
          <el-select v-model="form.dealerId" placeholder="请选择经销商" filterable clearable style="width: 100%">
            <el-option
              v-for="item in dealerOptions"
              :key="item.id"
              :label="item.dealerName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
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
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pageWorkers, createWorker, updateWorker, deleteWorker, resetWorkerPassword, pageDealers } from '@/api/user'
import type { WorkerVO, DealerVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const tableData = ref<WorkerVO[]>([])
const total = ref(0)
const dealerOptions = ref<DealerVO[]>([])

const searchForm = reactive({
  name: '',
  phone: '',
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
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.phone) params.phone = searchForm.phone

    const res = await pageWorkers(params as any)
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
  searchForm.name = ''
  searchForm.phone = ''
  searchForm.pageNum = 1
  loadData()
}

// ===== 弹窗 =====
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const submitLoading = ref(false)

const dialogTitle = computed(() => (editId.value ? '编辑运维人员' : '新增运维人员'))

const form = reactive({
  name: '',
  phone: '',
  password: '',
  dealerId: 1,
  status: 'ACTIVE',
})

const rules = computed<FormRules>(() => ({
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: editId.value ? [] : [{ required: true, message: '请输入密码', trigger: 'blur' }],
  dealerId: [{ required: true, message: '请选择经销商', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}))

function handleCreate() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: WorkerVO) {
  editId.value = row.id
  form.name = row.name
  form.phone = row.phone
  form.password = ''
  form.dealerId = row.dealerId || 1
  form.status = row.status || 'ACTIVE'
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    const res = await deleteWorker(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch {
    // error handled by interceptor
  }
}

async function handleResetPassword(id: number) {
  try {
    const res = await resetWorkerPassword(id)
    if (res.code === 200) {
      ElMessage.success('密码已重置为手机号后6位')
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
  form.name = ''
  form.phone = ''
  form.password = ''
  form.dealerId = 1
  form.status = 'ACTIVE'
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (editId.value) {
      const res = await updateWorker(editId.value, {
        name: form.name,
        phone: form.phone,
        dealerId: form.dealerId,
      })
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else {
      const res = await createWorker({
        name: form.name,
        phone: form.phone,
        password: form.password,
        dealerId: form.dealerId,
      })
      if (res.code === 200) {
        ElMessage.success('创建成功，初始密码为手机号后6位')
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
  loadDealerOptions()
})

async function loadDealerOptions() {
  try {
    const res = await pageDealers({ pageNum: 1, pageSize: 200 } as any)
    if (res.code === 200) {
      dealerOptions.value = res.data.records || []
    }
  } catch {
    // ignore
  }
}

function getDealerName(dealerId: any): string {
  if (!dealerId) return '-'
  const dealer = dealerOptions.value.find((d) => String(d.id) === String(dealerId))
  return dealer?.dealerName || '-'
}
</script>

<style scoped lang="scss">
.worker-list-page {
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
