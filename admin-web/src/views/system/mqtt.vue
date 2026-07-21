<template>
  <div class="mqtt-page">
    <!-- MQTT 列表 -->
    <el-card class="toolbar-card">
      <div class="toolbar">
        <h3 class="title">MQTT</h3>
        <div class="actions">
          <el-button type="primary" :icon="Refresh" @click="loadData" :loading="loading">刷新</el-button>
          <el-button type="success" :icon="Position" @click="openCreateDialog" plain>新增连接</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" stripe border v-loading="loading">
        <el-table-column prop="id" label="ID" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="id-text">{{ row.id || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160">
          <template #default="{ row }">
            <el-icon class="mr-1"><Connection /></el-icon>
            <span>{{ row.name || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="broker" label="Broker" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.broker || '-' }}</template>
        </el-table-column>
        <el-table-column prop="runningStatus" label="运行状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRunningTagType(row.runningStatus)" size="small">
              {{ row.runningStatus || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="linkStatus" label="链路状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getLinkTagType(row.linkStatus)" size="small">
              {{ row.linkStatus || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ row.createdAt ? formatDateTime(row.createdAt) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="160">
          <template #default="{ row }">{{ row.updatedAt ? formatDateTime(row.updatedAt) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewDetail(row as any)">
              <el-icon><View /></el-icon>
            </el-button>
            <el-button type="primary" link size="small" @click="openEditDialog(row as any)">
              <el-icon><Edit /></el-icon>
            </el-button>
            <el-button type="success" link size="small" @click="handleReload(row as any)">
              <el-icon><RefreshRight /></el-icon>
            </el-button>
            <el-button type="warning" link size="small" @click="handleTest(row as any)">
              <el-icon><CircleCheck /></el-icon>
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && tableData.length === 0" description="暂无 MQTT 配置" />
    </el-card>

    <!-- 编辑/新增弹窗 -->
    <el-dialog
      v-model="showDialog"
      :title="isEdit ? '编辑 MQTT 配置' : '新增 MQTT 连接'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="如：MQTT服务器" />
        </el-form-item>
        <el-form-item label="MQTT地址" prop="broker">
          <el-input v-model="formData.broker" placeholder="tcp://host:1883" />
        </el-form-item>
        <el-form-item label="客户端ID">
          <el-input v-model="formData.clientId" placeholder="默认 iot-service" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="formData.username" placeholder="可选" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="formData.password" type="password" placeholder="可选" show-password />
        </el-form-item>
        <el-form-item label="心跳间隔(秒)">
          <el-input-number v-model="formData.keepAliveInterval" :min="10" :max="600" :step="10" />
        </el-form-item>
        <el-form-item label="清除会话">
          <el-switch v-model="formData.cleanSession" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="formData.enabled" />
        </el-form-item>
        <el-alert
          v-if="isEdit"
          type="info"
          :closable="false"
          show-icon
          title="保存后将自动重连 MQTT 服务器"
        />
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="MQTT 连接详情" width="640px">
      <el-descriptions :column="2" border v-if="currentDetail">
        <el-descriptions-item label="名称">{{ currentDetail.name }}</el-descriptions-item>
        <el-descriptions-item label="运行状态">
          <el-tag :type="getRunningTagType(currentDetail.runningStatus)">{{ currentDetail.runningStatus || '未知' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Broker" :span="2">{{ currentDetail.broker }}</el-descriptions-item>
        <el-descriptions-item label="客户端ID">{{ currentDetail.clientId || 'iot-service' }}</el-descriptions-item>
        <el-descriptions-item label="心跳间隔(秒)">{{ currentDetail.keepAliveInterval || 60 }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentDetail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="密码">{{ maskPassword(currentDetail.password) }}</el-descriptions-item>
        <el-descriptions-item label="清除会话">{{ currentDetail.cleanSession ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="启用">{{ currentDetail.enabled ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="链路状态">
          <el-tag :type="getLinkTagType(currentDetail.linkStatus)">{{ currentDetail.linkStatus || '未知' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentDetail.createdAt ? formatDateTime(currentDetail.createdAt) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentDetail.updatedAt ? formatDateTime(currentDetail.updatedAt) : '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Refresh, Position, View, Edit, RefreshRight, CircleCheck, Connection } from '@element-plus/icons-vue'
import { getMqttConfig, updateMqttConfig, testMqttConnection, type MqttConfigVO } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<MqttConfigVO[]>([])
const showDialog = ref(false)
const showDetailDialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const currentDetail = ref<MqttConfigVO | null>(null)

const formData = reactive<MqttConfigVO>({
  name: 'MQTT服务器',
  broker: 'tcp://dw-emqx:1883',
  clientId: 'iot-service',
  username: 'admin',
  password: '',
  keepAliveInterval: 60,
  cleanSession: false,
  enabled: true
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  broker: [
    { required: true, message: '请输入 MQTT 地址', trigger: 'blur' },
    { pattern: /^tcp:\/\/.+:\d+$/, message: '格式必须为 tcp://host:port', trigger: 'blur' }
  ]
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMqttConfig()
    if (res.code === 200 && res.data) {
      tableData.value = [res.data]
    } else {
      tableData.value = []
    }
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  isEdit.value = false
  Object.assign(formData, {
    name: 'MQTT服务器',
    broker: 'tcp://dw-emqx:1883',
    clientId: 'iot-service',
    username: 'admin',
    password: '',
    keepAliveInterval: 60,
    cleanSession: false,
    enabled: true
  })
  showDialog.value = true
}

function openEditDialog(row: MqttConfigVO) {
  isEdit.value = true
  Object.assign(formData, {
    name: row.name,
    broker: row.broker,
    clientId: row.clientId || 'iot-service',
    username: row.username,
    password: row.password,
    keepAliveInterval: row.keepAliveInterval || 60,
    cleanSession: row.cleanSession ?? false,
    enabled: row.enabled ?? true
  })
  showDialog.value = true
}

function viewDetail(row: MqttConfigVO) {
  currentDetail.value = { ...row }
  showDetailDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const res = await updateMqttConfig(formData)
    if (res.code === 200) {
      ElMessage.success('保存成功，iot-service 正在重连')
      showDialog.value = false
      // 2秒后刷新以看到 iot-service 的最新状态
      setTimeout(loadData, 2000)
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } finally {
    submitting.value = false
  }
}

async function handleReload(row: MqttConfigVO) {
  try {
    await ElMessageBox.confirm(`确认重新加载 "${row.name}" 的 MQTT 配置并立即重连？`, '提示', {
      type: 'info'
    })
  } catch { return }
  ElMessage.info('正在重新加载配置...')
  // 直接通过更新配置（保持原值）触发 reload
  const res = await updateMqttConfig({
    name: row.name,
    broker: row.broker,
    clientId: row.clientId,
    username: row.username,
    password: row.password,
    keepAliveInterval: row.keepAliveInterval,
    cleanSession: row.cleanSession,
    enabled: row.enabled
  })
  if (res.code === 200) {
    ElMessage.success('重载请求已发送')
    setTimeout(loadData, 2000)
  }
}

async function handleTest(row: MqttConfigVO) {
  const res = await testMqttConnection({
    name: row.name,
    broker: row.broker,
    clientId: row.clientId,
    username: row.username,
    password: row.password,
    keepAliveInterval: row.keepAliveInterval,
    cleanSession: row.cleanSession,
    enabled: row.enabled
  })
  if (res.code === 200) {
    if (res.data?.success) {
      ElMessage.success(res.data.message || '测试通过')
    } else {
      ElMessage.warning(res.data?.message || '测试失败')
    }
  } else {
    ElMessage.error(res.message || '测试失败')
  }
}

function getRunningTagType(status?: string): 'success' | 'danger' | 'info' | 'warning' {
  if (status === '运行') return 'success'
  if (status === '停止') return 'danger'
  return 'info'
}

function getLinkTagType(status?: string): 'success' | 'danger' | 'info' | 'warning' {
  if (status === '连接') return 'success'
  if (status === '断开') return 'danger'
  return 'info'
}

function maskPassword(pwd?: string): string {
  if (!pwd) return '-'
  if (pwd.length <= 2) return '**'
  return pwd[0] + '****' + pwd[pwd.length - 1]
}

// 每5秒自动刷新状态
let timer: number | null = null
function startAutoRefresh() {
  stopAutoRefresh()
  timer = window.setInterval(loadData, 5000)
}
function stopAutoRefresh() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(() => {
  loadData()
  startAutoRefresh()
})
onUnmounted(stopAutoRefresh)
</script>

<style scoped lang="scss">
.mqtt-page {
  padding: 16px;

  .toolbar-card {
    margin-bottom: 16px;
    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      .title {
        margin: 0;
        font-size: 18px;
        font-weight: 600;
      }
    }
  }

  .table-card {
    .id-text {
      font-family: monospace;
      font-size: 12px;
      color: #909399;
    }
    .mr-1 {
      margin-right: 4px;
      color: #409eff;
    }
  }
}
</style>
