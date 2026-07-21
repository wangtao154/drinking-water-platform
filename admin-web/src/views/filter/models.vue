<template>
  <div class="filter-models-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="型号编码/名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部分类" clearable style="width: 150px">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
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
      <div class="toolbar-content">
        <el-button type="primary" @click="openCreateDialog">新增型号</el-button>
        <el-button :icon="Refresh" size="small" plain @click="loadData">刷新</el-button>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="modelCode" label="型号编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="modelName" label="型号名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="categoryTagType(row.category)">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="filterLevel" label="级别" width="80" align="center" />
        <el-table-column label="标准寿命" min-width="160">
          <template #default="{ row }">
            <span v-if="row.standardLifeDuration">{{ row.standardLifeDuration }}个月</span>
            <span v-if="row.standardLifeDuration && row.standardLifeFlow"> / </span>
            <span v-if="row.standardLifeFlow">{{ row.standardLifeFlow }}L</span>
          </template>
        </el-table-column>
        <el-table-column label="参考售价" width="100" align="right">
          <template #default="{ row }">
            {{ row.price ? ('¥' + (row.price / 100).toFixed(2)) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除该滤芯型号？" @confirm="handleDelete(row)">
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑滤芯型号' : '新增滤芯型号'" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="型号名称" prop="modelName">
          <el-input v-model="formData.modelName" placeholder="如: PP棉滤芯-5微米" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="滤芯级别" prop="filterLevel">
          <el-input-number v-model="formData.filterLevel" :min="1" :max="6" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">1-6级，对应设备安装位置</span>
        </el-form-item>
        <el-form-item label="标准寿命(月)" prop="standardLifeDuration">
          <el-input-number v-model="formData.standardLifeDuration" :min="1" :max="120" />
        </el-form-item>
        <el-form-item label="标准寿命(升)" prop="standardLifeFlow">
          <el-input-number v-model="formData.standardLifeFlow" :min="1" :step="1000" />
        </el-form-item>
        <el-form-item label="参考售价(元)" prop="price">
          <el-input-number v-model="formData.priceYuan" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="型号描述（选填）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { pageFilterModels, createFilterModel, updateFilterModel, deleteFilterModel } from '@/api/filter'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  category: '',
  status: '',
  pageNum: 1,
  pageSize: 10,
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number>(0)

const formData = reactive({
  modelName: '',
  category: '',
  filterLevel: 1,
  standardLifeDuration: 6,
  standardLifeFlow: 3000,
  priceYuan: 0,
  status: 'ENABLED',
  description: '',
})

const formRules: FormRules = {
  modelName: [{ required: true, message: '请输入型号名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  filterLevel: [{ required: true, message: '请输入滤芯级别', trigger: 'blur' }],
  standardLifeDuration: [{ required: true, message: '请输入标准寿命(月)', trigger: 'blur' }],
  standardLifeFlow: [{ required: true, message: '请输入标准寿命(升)', trigger: 'blur' }],
}

const categories = [
  { label: 'PP棉', value: 'PP_COTTON' },
  { label: '颗粒活性炭', value: 'GRANULAR_AC' },
  { label: '压缩活性炭', value: 'COMPRESSED_AC' },
  { label: 'RO膜', value: 'RO_MEMBRANE' },
  { label: '后置活性炭', value: 'POST_AC' },
  { label: '其他', value: 'OTHER' },
]

function categoryLabel(value: string): string {
  const found = categories.find(c => c.value === value)
  return found ? found.label : value
}

function categoryTagType(category: string): string {
  const map: Record<string, string> = {
    PP_COTTON: 'primary',
    GRANULAR_AC: 'success',
    COMPRESSED_AC: 'warning',
    RO_MEMBRANE: 'danger',
    POST_AC: '',
    OTHER: 'info',
  }
  return map[category] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.category) params.category = searchForm.category
    if (searchForm.status) params.status = searchForm.status

    const res = await pageFilterModels(params)
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
  searchForm.category = ''
  searchForm.status = ''
  searchForm.pageNum = 1
  loadData()
}

function openCreateDialog() {
  isEdit.value = false
  formData.modelName = ''
  formData.category = ''
  formData.filterLevel = 1
  formData.standardLifeDuration = 6
  formData.standardLifeFlow = 3000
  formData.priceYuan = 0
  formData.status = 'ENABLED'
  formData.description = ''
  dialogVisible.value = true
}

function openEditDialog(row: any) {
  isEdit.value = true
  editingId.value = row.id
  formData.modelName = row.modelName || ''
  formData.category = row.category || ''
  formData.filterLevel = row.filterLevel || 1
  formData.standardLifeDuration = row.standardLifeDuration || 6
  formData.standardLifeFlow = row.standardLifeFlow || 3000
  formData.priceYuan = row.price ? row.price / 100 : 0
  formData.status = row.status || 'ENABLED'
  formData.description = row.description || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const data: Record<string, unknown> = {
        modelName: formData.modelName,
        category: formData.category,
        filterLevel: formData.filterLevel,
        standardLifeDuration: formData.standardLifeDuration,
        standardLifeFlow: formData.standardLifeFlow,
        price: Math.round(formData.priceYuan * 100),
        status: formData.status,
        description: formData.description,
      }

      if (isEdit.value) {
        const res = await updateFilterModel(editingId.value, data)
        if (res.code === 200) {
          ElMessage.success('更新成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res.message || '更新失败')
        }
      } else {
        const res = await createFilterModel(data)
        if (res.code === 200) {
          ElMessage.success('创建成功')
          dialogVisible.value = false
          loadData()
        } else {
          ElMessage.error(res.message || '创建失败')
        }
      }
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: any) {
  try {
    const res = await deleteFilterModel(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch {
    // error handled by interceptor
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.filter-models-page {
  padding: 16px;

  .search-card {
    margin-bottom: 16px;
  }

  .toolbar-card {
    margin-bottom: 16px;

    .toolbar-content {
      display: flex;
      gap: 8px;
      align-items: center;
    }
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
