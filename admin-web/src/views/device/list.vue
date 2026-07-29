<template>
  <div class="device-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="设备ID">
          <el-input
            v-model="searchForm.deviceId"
            placeholder="请输入设备ID"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="SN">
          <el-input
            v-model="searchForm.sn"
            placeholder="请输入SN"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="型号名称">
          <el-select
            v-model="searchForm.modelName"
            placeholder="请选择型号"
            clearable
            filterable
            style="width: 180px"
          >
            <el-option
              v-for="item in modelOptions"
              :key="item.id"
              :label="item.modelName"
              :value="item.modelName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定客户">
          <el-input
            v-model="searchForm.customerName"
            placeholder="请输入客户名称"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="激活时间">
          <el-date-picker
            v-model="searchForm.activatedAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 380px"
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
        <el-form-item label="在线状态">
          <el-select v-model="searchForm.onlineStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="全部" :value="null" />
            <el-option label="在线" :value="1" />
            <el-option label="离线" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="生命周期状态">
          <el-select v-model="searchForm.lifecycleStatus" placeholder="全部" clearable style="width: 160px">
            <el-option label="全部" value="" />
            <el-option label="已注册" value="REGISTERED" />
            <el-option label="已激活在线" value="ACTIVATED_ONLINE" />
            <el-option label="已激活离线" value="ACTIVATED_OFFLINE" />
            <el-option label="已退机" value="RETURNED" />
            <el-option label="故障" value="FAULT" />
            <el-option label="停用" value="DISABLED" />
            <el-option label="报废" value="SCRAPPED" />
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
        <el-button type="primary" @click="handleAdd">新增设备</el-button>
        <div class="auto-refresh">
          <el-tooltip :content="autoRefresh ? '点击停止自动刷新' : '点击开始自动刷新（每10秒）'" placement="top">
            <el-button
              :type="autoRefresh ? 'danger' : 'primary'"
              :icon="autoRefresh ? VideoPause : Refresh"
              circle
              size="small"
              @click="toggleAutoRefresh"
            />
          </el-tooltip>
          <el-button :icon="Refresh" size="small" plain @click="fetchData">刷新</el-button>
        </div>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="deviceId" label="设备ID" min-width="120" show-overflow-tooltip />
        <el-table-column prop="sn" label="SN" min-width="150" show-overflow-tooltip />
        <el-table-column prop="modelName" label="型号名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="在线状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="(row.onlineStatus === 1 || row.onlineStatus === 'ONLINE') ? 'success' : 'info'" size="small">
              {{ (row.onlineStatus === 1 || row.onlineStatus === 'ONLINE') ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="生命周期状态" min-width="140" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.lifecycleStatus)" size="small">
              {{ statusLabel(row.lifecycleStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="绑定客户" min-width="140" show-overflow-tooltip />
        <el-table-column label="入库时间" min-width="170">
          <template #default="{ row }">
            {{ row.createdAt ? formatDateTime(row.createdAt, 'YYYY-MM-DD HH:mm:ss') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="激活时间" min-width="170">
          <template #default="{ row }">
            {{ row.activatedAt ? formatDateTime(row.activatedAt, 'YYYY-MM-DD HH:mm:ss') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row.deviceId)">
              查看详情
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-popconfirm
              v-if="row.customerId"
              title="确定要解绑该设备吗？解绑后设备状态将变为已退机。"
              confirm-button-text="确定解绑"
              cancel-button-text="取消"
              confirm-button-type="warning"
              @confirm="handleUnbind(row.deviceId)"
            >
              <template #reference>
                <el-button type="warning" link size="small">解绑</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              title="确定要删除该设备吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row.deviceId)"
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

    <!-- 编辑设备弹窗 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑设备"
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
        <el-form-item label="设备ID">
          <el-input v-model="editForm.deviceId" disabled />
        </el-form-item>
        <el-form-item label="SN" prop="sn">
          <el-input
            v-model="editForm.sn"
            placeholder="j+6位数字，如j082438"
            maxlength="7"
          />
        </el-form-item>
        <el-form-item label="设备型号" prop="modelId">
          <el-select
            v-model="editForm.modelId"
            placeholder="请选择设备型号"
            filterable
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in modelOptions"
              :key="item.id"
              :label="item.modelName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="ICCID" prop="iccid">
          <el-input v-model="editForm.iccid" placeholder="请输入ICCID（选填）" maxlength="32" />
        </el-form-item>
        <el-form-item label="IMEI" prop="imei">
          <el-input v-model="editForm.imei" placeholder="请输入IMEI（选填）" maxlength="32" />
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
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, VideoPause } from '@element-plus/icons-vue'
import { pageDevices, deleteDevice, getDevice, updateDevice, listDeviceModels, unbindDevice } from '@/api/device'
import type { DeviceModelVO } from '@/types/api'
import { formatDateTime, statusLabel, statusTagType } from '@/utils/format'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const autoRefresh = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

// ===== 编辑弹窗相关 =====
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editSubmitting = ref(false)
const modelOptions = ref<DeviceModelVO[]>([])
const editForm = reactive({
  id: null as number | null,
  deviceId: '',
  sn: '',
  modelId: '' as number | string | null,
  iccid: '',
  imei: '',
  remark: '',
})

const editRules: FormRules = {
  modelId: [{ required: true, message: '请选择设备型号', trigger: 'change' }],
  sn: [
    { required: true, message: '请输入设备SN', trigger: 'blur' },
    { pattern: /^j\d{6}$/, message: 'SN格式必须为j+6位数字，如j082438', trigger: 'blur' },
  ],
}

async function loadModels() {
  try {
    const res = await listDeviceModels()
    if (res.code === 200) {
      modelOptions.value = (res.data as DeviceModelVO[]) || []
    }
  } catch (e) {
    console.error('Failed to load device models', e)
  }
}

const searchForm = reactive({
  deviceId: '',
  sn: '',
  modelName: '',
  customerName: '',
  activatedAtRange: [] as string[],
  createdAtRange: [] as string[],
  onlineStatus: null as number | null,
  lifecycleStatus: '',
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
    if (searchForm.deviceId) params.deviceId = searchForm.deviceId
    if (searchForm.sn) params.sn = searchForm.sn
    if (searchForm.modelName) params.modelName = searchForm.modelName
    if (searchForm.customerName) params.customerName = searchForm.customerName
    if (searchForm.onlineStatus !== null) params.onlineStatus = searchForm.onlineStatus
    if (searchForm.lifecycleStatus) params.lifecycleStatus = searchForm.lifecycleStatus
    if (searchForm.activatedAtRange && searchForm.activatedAtRange.length === 2) {
      params.activatedAtStart = searchForm.activatedAtRange[0]
      params.activatedAtEnd = searchForm.activatedAtRange[1]
    }
    if (searchForm.createdAtRange && searchForm.createdAtRange.length === 2) {
      params.createdAtStart = searchForm.createdAtRange[0]
      params.createdAtEnd = searchForm.createdAtRange[1]
    }

    const res = await pageDevices(params)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    refreshTimer = setInterval(() => {
      fetchData()
    }, 10000)
    ElMessage.success('已开启自动刷新（每10秒）')
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    ElMessage.info('已停止自动刷新')
  }
}

function handleSearch() {
  searchForm.pageNum = 1
  fetchData()
}

function handleReset() {
  searchForm.deviceId = ''
  searchForm.sn = ''
  searchForm.modelName = ''
  searchForm.customerName = ''
  searchForm.activatedAtRange = []
  searchForm.createdAtRange = []
  searchForm.onlineStatus = null
  searchForm.lifecycleStatus = ''
  searchForm.pageNum = 1
  fetchData()
}

function handleAdd() {
  router.push('/devices/register')
}

function handleDetail(deviceId: string) {
  router.push(`/devices/detail/${deviceId}`)
}

async function handleDelete(deviceId: string) {
  try {
    const res = await deleteDevice(deviceId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch {
    // error handled by interceptor
  }
}

async function handleUnbind(deviceId: string) {
  try {
    const res = await unbindDevice(deviceId)
    if (res.code === 200) {
      ElMessage.success('设备已解绑')
      fetchData()
    }
  } catch {
    // error handled by interceptor
  }
}

async function handleEdit(row: any) {
  try {
    const res = await getDevice(row.deviceId)
    if (res.code === 200) {
      const d = res.data as any
      editForm.id = d.id ?? null
      editForm.deviceId = d.deviceId || ''
      editForm.sn = d.sn || ''
      editForm.modelId = d.modelId ?? null
      editForm.iccid = d.iccid || ''
      editForm.imei = d.imei || ''
      editForm.remark = d.remark || ''
      editDialogVisible.value = true
    }
  } catch (e) {
    // error handled by interceptor
  }
}

function handleEditClosed() {
  editFormRef.value?.resetFields()
  editForm.id = null
  editForm.deviceId = ''
  editForm.sn = ''
  editForm.modelId = null
  editForm.iccid = ''
  editForm.imei = ''
  editForm.remark = ''
}

async function handleEditSubmit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      const data: Record<string, unknown> = {
        modelId: editForm.modelId,
        sn: editForm.sn,
      }
      if (editForm.iccid) data.iccid = editForm.iccid
      if (editForm.imei) data.imei = editForm.imei
      if (editForm.remark) data.remark = editForm.remark

      const res = await updateDevice(editForm.deviceId, data)
      if (res.code === 200) {
        ElMessage.success('保存成功')
        editDialogVisible.value = false
        fetchData()
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } finally {
      editSubmitting.value = false
    }
  })
}

onMounted(() => {
  fetchData()
  loadModels()
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped lang="scss">
.device-list-page {
  padding: 16px;

  .search-card {
    margin-bottom: 16px;

    .search-form {
      display: flex;
      flex-wrap: wrap;
      gap: 0;
    }
  }

  .toolbar-card {
    margin-bottom: 16px;

    .toolbar-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .auto-refresh {
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
