<template>
  <div class="customer-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="客户名称 / 手机号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="客户类型">
          <el-select v-model="searchForm.customerType" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="个人" value="INDIVIDUAL" />
            <el-option label="企业" value="ENTERPRISE" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="游客" value="GUEST" />
            <el-option label="正式" value="ACTIVE" />
            <el-option label="禁用" value="DISABLED" />
            <el-option label="冻结" value="FROZEN" />
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
      <el-button type="primary" @click="handleCreate">新增客户</el-button>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="customerName" label="客户名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="客户类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.customerType === 'ENTERPRISE' ? 'warning' : 'info'" size="small">
              {{ statusLabel(row.customerType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="customerStatusType(row.status)" size="small">
              {{ customerStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="所属经销商" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.dealerName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-popconfirm
              title="确定要删除该客户吗？"
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
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="form.customerName" placeholder="请输入客户名称" clearable />
        </el-form-item>
        <el-form-item label="客户类型" prop="customerType">
          <el-select v-model="form.customerType" placeholder="请选择客户类型" style="width: 100%">
            <el-option label="个人" value="INDIVIDUAL" />
            <el-option label="企业" value="ENTERPRISE" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="选填" clearable />
        </el-form-item>
        <el-form-item v-if="form.customerType === 'ENTERPRISE'" label="企业名称">
          <el-input v-model="form.enterpriseName" placeholder="请输入企业名称" clearable />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址" clearable />
        </el-form-item>
        <el-form-item label="经销商">
          <el-select
            v-model="form.dealerId"
            placeholder="请选择经销商"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in dealerOptions"
              :key="item.id"
              :label="item.dealerName"
              :value="item.id"
            />
          </el-select>
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
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { pageCustomers, createCustomer, updateCustomer, deleteCustomer } from '@/api/user'
import { pageDealers } from '@/api/user'
import type { CustomerVO, DealerVO } from '@/types/api'
import { formatDateTime, statusLabel } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const tableData = ref<CustomerVO[]>([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  customerType: '',
  status: '',
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
    if (searchForm.customerType) params.customerType = searchForm.customerType
    if (searchForm.status) params.status = searchForm.status

    const res = await pageCustomers(params)
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
  searchForm.customerType = ''
  searchForm.status = ''
  searchForm.pageNum = 1
  loadData()
}

// ===== 弹窗 =====
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const submitLoading = ref(false)
const dealerOptions = ref<DealerVO[]>([])

const dialogTitle = computed(() => (editId.value ? '编辑客户' : '新增客户'))

const form = reactive({
  customerName: '',
  customerType: 'INDIVIDUAL',
  phone: '',
  idCard: '',
  enterpriseName: '',
  address: '',
  dealerId: null as number | null,
})

const rules: FormRules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  customerType: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
}

async function fetchDealers() {
  try {
    const res = await pageDealers({ pageNum: 1, pageSize: 200 })
    if (res.code === 200) {
      dealerOptions.value = res.data.records
    }
  } catch {
    // ignore
  }
}

function handleCreate() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: CustomerVO) {
  editId.value = row.id
  form.customerName = row.customerName
  form.customerType = row.customerType
  form.phone = row.phone
  form.idCard = row.idCard || ''
  form.enterpriseName = row.enterpriseName || ''
  form.address = row.address || ''
  form.dealerId = row.dealerId ?? null
  dialogVisible.value = true
}

async function handleDelete(id: number) {
  try {
    const res = await deleteCustomer(id)
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
  form.customerName = ''
  form.customerType = 'INDIVIDUAL'
  form.phone = ''
  form.idCard = ''
  form.enterpriseName = ''
  form.address = ''
  form.dealerId = null
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data: Record<string, unknown> = {
      customerName: form.customerName,
      customerType: form.customerType,
      phone: form.phone,
      idCard: form.idCard,
      address: form.address,
      dealerId: form.dealerId,
    }
    if (form.customerType === 'ENTERPRISE') {
      data.enterpriseName = form.enterpriseName
    }

    if (editId.value) {
      const res = await updateCustomer(editId.value, data)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadData()
      }
    } else {
      const res = await createCustomer(data)
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
  fetchDealers()
})

// 客户状态辅助方法
function customerStatusText(status: string): string {
  const map: Record<string, string> = {
    GUEST: '游客',
    ACTIVE: '正式',
    DISABLED: '禁用',
    FROZEN: '冻结',
    INACTIVE: '未激活',
  }
  return map[status] || status || '-'
}

function customerStatusType(status: string): string {
  const map: Record<string, string> = {
    GUEST: 'info',
    ACTIVE: 'success',
    DISABLED: 'danger',
    FROZEN: 'warning',
    INACTIVE: 'info',
  }
  return map[status] || 'info'
}
</script>

<style scoped lang="scss">
.customer-list-page {
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
