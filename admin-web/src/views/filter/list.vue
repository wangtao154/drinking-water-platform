<template>
  <div class="filter-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="滤芯ID"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="型号">
          <el-select v-model="searchForm.modelId" placeholder="全部型号" clearable filterable style="width: 200px">
            <el-option
              v-for="m in modelOptions"
              :key="m.id"
              :label="m.modelName"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.lifecycleStatus" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="在库" value="IN_STOCK" />
            <el-option label="待安装" value="PENDING_INSTALL" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属设备">
          <el-input
            v-model="searchForm.currentDeviceId"
            placeholder="请输入设备ID"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="入库时间">
          <el-date-picker
            v-model="searchForm.createdAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 380px"
          />
        </el-form-item>
        <el-form-item label="安装时间">
          <el-date-picker
            v-model="searchForm.installedAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 380px"
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
      <div class="toolbar-content">
        <el-button type="primary" @click="handleRegister">滤芯登记</el-button>
        <el-button :icon="Refresh" size="small" plain @click="loadData">刷新</el-button>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="filterId" label="滤芯ID" min-width="150" show-overflow-tooltip />
        <el-table-column prop="modelName" label="型号名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.lifecycleStatus)" size="small">
              {{ statusLabel(row.lifecycleStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余寿命" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.remainPercentage !== null && row.remainPercentage !== undefined">
              <el-tag :type="row.remainPercentage > 30 ? 'success' : 'danger'" size="small">
                {{ row.remainPercentage }}%
              </el-tag>
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentDeviceId" label="所属设备" min-width="120" show-overflow-tooltip />
        <el-table-column label="安装时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.installedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="入库时间" min-width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt || row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleTrace(row.filterId)">
              追溯
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-popconfirm
              title="确定要删除该滤芯吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row.filterId)"
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

    <!-- 编辑弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑滤芯"
      width="560px"
      :close-on-click-modal="false"
      @closed="handleEditClosed"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="100px"
      >
        <el-form-item label="滤芯ID">
          <el-input v-model="editForm.filterId" disabled />
        </el-form-item>
        <el-form-item label="滤芯型号" prop="filterModelId">
          <el-select
            v-model="editForm.filterModelId"
            placeholder="请选择滤芯型号"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="m in modelOptions"
              :key="m.id"
              :label="m.modelName"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="生产日期" prop="productionDate">
          <el-date-picker
            v-model="editForm.productionDate"
            type="date"
            placeholder="请选择生产日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="生产批次" prop="productionBatch">
          <el-input v-model="editForm.productionBatch" placeholder="请输入生产批次" maxlength="64" />
        </el-form-item>
        <el-form-item label="生命周期" prop="lifecycleStatus">
          <el-select v-model="editForm.lifecycleStatus" placeholder="请选择状态" style="width: 100%">
            <el-option label="在库" value="IN_STOCK" />
            <el-option label="待安装" value="PENDING_INSTALL" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="editForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="handleEditSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { pageFilters, getFilter, updateFilter, deleteFilter, listAllEnabledFilterModels } from '@/api/filter'
import type { FilterModelVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const modelOptions = ref<FilterModelVO[]>([])

const searchForm = reactive({
  keyword: '',
  modelId: '' as number | string,
  lifecycleStatus: '',
  currentDeviceId: '',
  createdAtRange: [] as string[],
  installedAtRange: [] as string[],
  pageNum: 1,
  pageSize: 10,
})

// ===== 编辑弹窗 =====
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editSubmitting = ref(false)
const editForm = reactive({
  filterId: '',
  filterModelId: '' as number | string | null,
  productionDate: '',
  productionBatch: '',
  lifecycleStatus: '',
  remark: '',
})

const editRules: FormRules = {
  filterModelId: [{ required: true, message: '请选择滤芯型号', trigger: 'change' }],
  lifecycleStatus: [{ required: true, message: '请选择生命周期状态', trigger: 'change' }],
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    IN_STOCK: '在库',
    PENDING_INSTALL: '待安装',
    IN_USE: '使用中',
    SCRAPPED: '已报废',
  }
  return map[status] || status || '-'
}

function statusTagType(status: string): string {
  const map: Record<string, string> = {
    IN_STOCK: 'info',
    PENDING_INSTALL: 'warning',
    IN_USE: 'success',
    SCRAPPED: 'danger',
  }
  return map[status] || 'info'
}

async function loadModels() {
  try {
    const res = await listAllEnabledFilterModels()
    if (res.code === 200) {
      modelOptions.value = (res.data as FilterModelVO[]) || []
    }
  } catch (e) {
    console.error('Failed to load filter models', e)
  }
}

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: searchForm.pageNum,
      pageSize: searchForm.pageSize,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.modelId) params.modelId = searchForm.modelId
    if (searchForm.lifecycleStatus) params.lifecycleStatus = searchForm.lifecycleStatus
    if (searchForm.currentDeviceId) params.currentDeviceId = searchForm.currentDeviceId
    if (searchForm.createdAtRange && searchForm.createdAtRange.length === 2) {
      params.createdAtStart = searchForm.createdAtRange[0]
      params.createdAtEnd = searchForm.createdAtRange[1]
    }
    if (searchForm.installedAtRange && searchForm.installedAtRange.length === 2) {
      params.installedAtStart = searchForm.installedAtRange[0]
      params.installedAtEnd = searchForm.installedAtRange[1]
    }

    const res = await pageFilters(params)
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
  searchForm.modelId = ''
  searchForm.lifecycleStatus = ''
  searchForm.currentDeviceId = ''
  searchForm.createdAtRange = []
  searchForm.installedAtRange = []
  searchForm.pageNum = 1
  loadData()
}

function handleRegister() {
  router.push('/filters/inbound')
}

function handleTrace(filterId: string) {
  router.push(`/filters/trace/${filterId}`)
}

async function handleEdit(row: any) {
  try {
    const res = await getFilter(row.filterId)
    if (res.code === 200) {
      const d = res.data as any
      editForm.filterId = d.filterId || ''
      editForm.filterModelId = d.filterModelId ?? null
      editForm.productionDate = d.productionDate || ''
      editForm.productionBatch = d.productionBatch || ''
      editForm.lifecycleStatus = d.lifecycleStatus || ''
      editForm.remark = d.remark || ''
      editDialogVisible.value = true
    }
  } catch (e) {
    // error handled by interceptor
  }
}

function handleEditClosed() {
  editFormRef.value?.resetFields()
  editForm.filterId = ''
  editForm.filterModelId = null
  editForm.productionDate = ''
  editForm.productionBatch = ''
  editForm.lifecycleStatus = ''
  editForm.remark = ''
}

async function handleEditSubmit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      const data: Record<string, unknown> = {}
      if (editForm.filterModelId) data.filterModelId = editForm.filterModelId
      if (editForm.productionDate) data.productionDate = editForm.productionDate
      if (editForm.productionBatch) data.productionBatch = editForm.productionBatch
      if (editForm.lifecycleStatus) data.lifecycleStatus = editForm.lifecycleStatus
      if (editForm.remark) data.remark = editForm.remark

      const res = await updateFilter(editForm.filterId, data)
      if (res.code === 200) {
        ElMessage.success('保存成功')
        editDialogVisible.value = false
        loadData()
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } finally {
      editSubmitting.value = false
    }
  })
}

async function handleDelete(filterId: string) {
  try {
    const res = await deleteFilter(filterId)
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
  loadModels()
})
</script>

<style scoped lang="scss">
.filter-list-page {
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
