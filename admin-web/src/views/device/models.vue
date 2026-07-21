<template>
  <div class="models-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="型号编码/名称" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.category" placeholder="全部分类" clearable style="width: 150px">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openCreateDialog">新增型号</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="filteredData" stripe border v-loading="loading">
        <el-table-column prop="modelCode" label="型号编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="modelName" label="型号名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="category" label="设备分类" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="categoryTagType(row.category)">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row as any)">编辑</el-button>
            <el-popconfirm title="确认删除该设备型号？删除后已登记的设备将无法关联型号。" @confirm="handleDelete(row as any)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑设备型号' : '新增设备型号'" width="560px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="型号编码" prop="modelCode">
          <el-input v-model="formData.modelCode" :disabled="isEdit" placeholder="如: DW-RO-100" />
        </el-form-item>
        <el-form-item label="型号名称" prop="modelName">
          <el-input v-model="formData.modelName" placeholder="如: 智饮RO-100型直饮水机" />
        </el-form-item>
        <el-form-item label="设备分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" filterable allow-create style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="ENABLED">启用</el-radio>
            <el-radio value="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="型号描述（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { listDeviceModels, createDeviceModel, updateDeviceModel, deleteDeviceModel } from '@/api/device'
import type { DeviceModelVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<DeviceModelVO[]>([])
const searchForm = reactive({ keyword: '', category: '' })
const showDialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref(0)

const formData = reactive({
  modelCode: '',
  modelName: '',
  category: '',
  status: 'ENABLED',
  description: '',
})

const formRules: FormRules = {
  modelCode: [{ required: true, message: '请输入型号编码', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入型号名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择设备分类', trigger: 'change' }],
}

// 预设分类列表
const presetCategories = ['商用直饮机', '商用超滤机', '家用直饮机', '工业直饮设备']
const categories = computed(() => {
  const fromData = tableData.value.map(d => d.category).filter(Boolean)
  return [...new Set([...presetCategories, ...fromData])]
})

// 前端筛选（后端返回全量 List）
const filteredData = computed(() => {
  let list = tableData.value
  if (searchForm.keyword) {
    const kw = searchForm.keyword.toLowerCase()
    list = list.filter(d =>
      d.modelCode?.toLowerCase().includes(kw) ||
      d.modelName?.toLowerCase().includes(kw)
    )
  }
  if (searchForm.category) {
    list = list.filter(d => d.category === searchForm.category)
  }
  return list
})

function categoryTagType(category: string): string {
  const map: Record<string, string> = {
    '商用直饮机': 'primary',
    '商用超滤机': 'success',
    '家用直饮机': 'warning',
    '工业直饮设备': 'danger',
  }
  return map[category] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await listDeviceModels()
    if (res.code === 200) {
      tableData.value = res.data || []
    }
  } catch (e) {
    ElMessage.error('加载设备型号列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  // 前端筛选，无需重新请求
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.category = ''
}

function openCreateDialog() {
  isEdit.value = false
  formData.modelCode = ''
  formData.modelName = ''
  formData.category = ''
  formData.status = 'ENABLED'
  formData.description = ''
  showDialog.value = true
}

function openEditDialog(row: DeviceModelVO) {
  isEdit.value = true
  editingId.value = row.id
  formData.modelCode = row.modelCode
  formData.modelName = row.modelName
  formData.category = row.category || ''
  formData.status = row.status || 'ENABLED'
  formData.description = row.description || ''
  showDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateDeviceModel(editingId.value, { ...formData })
        ElMessage.success('更新成功')
      } else {
        await createDeviceModel({ ...formData })
        ElMessage.success('创建成功')
      }
      showDialog.value = false
      loadData()
    } catch (e) {
      // error handled by interceptor
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: DeviceModelVO) {
  try {
    await deleteDeviceModel(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => { loadData() })
</script>

<style scoped lang="scss">
.models-page { padding: 16px; }
.search-card { margin-bottom: 12px; }
</style>
