<template>
  <div class="package-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="套餐名">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入套餐名"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.packageType" placeholder="全部" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="扫码" value="QR_SCAN" />
            <el-option label="租赁" value="RENTAL" />
            <el-option label="共享" value="SHARED" />
            <el-option label="钱包" value="WALLET" />
            <el-option label="安装" value="INSTALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="生效" value="ACTIVE" />
            <el-option label="失效" value="INACTIVE" />
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
      <el-button type="primary" @click="handleCreate">新增套餐</el-button>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="packageCode" label="套餐编码" min-width="140" show-overflow-tooltip />
        <el-table-column prop="packageName" label="名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ statusLabel(row.packageType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="计费模式" width="120" align="center">
          <template #default="{ row }">
            {{ statusLabel(row.chargeMode) }}
          </template>
        </el-table-column>
        <el-table-column label="价格" width="120" align="right">
          <template #default="{ row }">
            ¥{{ fenToYuan(row.price) }}
          </template>
        </el-table-column>
        <el-table-column label="原价" width="120" align="right">
          <template #default="{ row }">
            {{ row.originalPrice ? `¥${fenToYuan(row.originalPrice)}` : '-' }}
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm
              v-if="row.status === 'INACTIVE'"
              title="确认上架该套餐？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleOnline(row.id)"
            >
              <template #reference>
                <el-button type="success" link size="small">上架</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              v-if="row.status === 'ACTIVE'"
              title="确认下架该套餐？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleOffline(row.id)"
            >
              <template #reference>
                <el-button type="warning" link size="small">下架</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              title="确认删除该套餐？删除后不可恢复"
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
          @current-change="fetchData"
          @size-change="fetchData"
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
        <el-form-item label="名称" prop="packageName">
          <el-input v-model="form.packageName" placeholder="请输入套餐名称" clearable />
        </el-form-item>
        <el-form-item label="类型" prop="packageType">
          <el-select v-model="form.packageType" placeholder="请选择类型" style="width: 100%">
            <el-option label="扫码" value="QR_SCAN" />
            <el-option label="租赁" value="RENTAL" />
            <el-option label="共享" value="SHARED" />
            <el-option label="钱包" value="WALLET" />
            <el-option label="安装" value="INSTALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="计费模式" prop="chargeMode">
          <el-select v-model="form.chargeMode" placeholder="请选择计费模式" style="width: 100%">
            <el-option label="流量计费" value="FLOW_BASED" />
            <el-option label="月租" value="MONTHLY_RENT" />
            <el-option label="套餐充值" value="PACKAGE_RECHARGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格(元)" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            placeholder="请输入价格"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="原价(元)">
          <el-input-number
            v-model="form.originalPrice"
            :min="0"
            :precision="2"
            placeholder="选填"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入描述" />
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
import { pagePackages, createPackage, updatePackage, onlinePackage, offlinePackage, deletePackage } from '@/api/package'
import type { PackageVO } from '@/types/api'
import { formatDateTime, statusLabel, statusTagType, fenToYuan, yuanToFen } from '@/utils/format'

const loading = ref(false)
const tableData = ref<PackageVO[]>([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  packageType: '',
  status: '',
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
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.packageType) params.packageType = searchForm.packageType
    if (searchForm.status) params.status = searchForm.status

    const res = await pagePackages(params)
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
  searchForm.packageType = ''
  searchForm.status = ''
  searchForm.pageNum = 1
  fetchData()
}

// ===== 弹窗 =====
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const submitLoading = ref(false)

const dialogTitle = computed(() => (editId.value ? '编辑套餐' : '新增套餐'))

const form = reactive({
  packageName: '',
  packageType: '',
  chargeMode: '',
  price: 0,
  originalPrice: 0,
  description: '',
})

const rules: FormRules = {
  packageName: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  packageType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  chargeMode: [{ required: true, message: '请选择计费模式', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
}

function handleCreate() {
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: PackageVO) {
  editId.value = row.id
  form.packageName = row.packageName
  form.packageType = row.packageType
  form.chargeMode = row.chargeMode
  form.price = Number(fenToYuan(row.price))
  form.originalPrice = row.originalPrice ? Number(fenToYuan(row.originalPrice)) : 0
  form.description = row.description || ''
  dialogVisible.value = true
}

function handleDialogClosed() {
  resetForm()
}

function resetForm() {
  formRef.value?.resetFields()
  form.packageName = ''
  form.packageType = ''
  form.chargeMode = ''
  form.price = 0
  form.originalPrice = 0
  form.description = ''
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data = {
      packageName: form.packageName,
      packageType: form.packageType,
      chargeMode: form.chargeMode,
      price: yuanToFen(form.price),
      originalPrice: form.originalPrice ? yuanToFen(form.originalPrice) : 0,
      description: form.description,
    }

    if (editId.value) {
      const res = await updatePackage(editId.value, data)
      if (res.code === 200) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        fetchData()
      }
    } else {
      const res = await createPackage(data)
      if (res.code === 200) {
        ElMessage.success('创建成功')
        dialogVisible.value = false
        fetchData()
      }
    }
  } finally {
    submitLoading.value = false
  }
}

async function handleOnline(id: number) {
  try {
    const res = await onlinePackage(id)
    if (res.code === 200) {
      ElMessage.success('上架成功')
      fetchData()
    }
  } catch {
    // error handled by interceptor
  }
}

async function handleOffline(id: number) {
  try {
    const res = await offlinePackage(id)
    if (res.code === 200) {
      ElMessage.success('下架成功')
      fetchData()
    }
  } catch {
    // error handled by interceptor
  }
}

onMounted(() => {
  fetchData()
})

async function handleDelete(id: number) {
  try {
    const res = await deletePackage(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch {
    // error handled by interceptor
  }
}
</script>

<style scoped lang="scss">
.package-list-page {
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
