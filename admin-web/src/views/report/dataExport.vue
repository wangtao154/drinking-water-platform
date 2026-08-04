<template>
  <div class="data-export-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>历史数据导出</span>
          <el-button :icon="Refresh" @click="loadDevices">刷新设备</el-button>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="选择设备" prop="sn">
              <el-select
                v-model="form.sn"
                class="full-width"
                filterable
                placeholder="请选择设备"
                :loading="deviceLoading"
              >
                <el-option
                  v-for="device in devices"
                  :key="device.sn"
                  :label="formatDeviceLabel(device)"
                  :value="device.sn"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="统计间隔" prop="interval">
              <el-select v-model="form.interval" class="full-width" placeholder="请选择统计间隔">
                <el-option v-for="item in intervalOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="时间范围" prop="timeRange">
              <el-date-picker
                v-model="form.timeRange"
                class="full-width"
                type="datetimerange"
                value-format="YYYY-MM-DD HH:mm:ss"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                range-separator="至"
                :shortcuts="dateShortcuts"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="导出数据" prop="fields">
          <el-select
            v-model="form.fields"
            class="full-width"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :max-collapse-tags="6"
            placeholder="请选择要导出的数据点"
          >
            <el-option-group v-for="group in pointGroups" :key="group.label" :label="group.label">
              <el-option v-for="point in group.options" :key="point.value" :label="point.label" :value="point.value" />
            </el-option-group>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Download" :loading="exporting" @click="handleExport">导出 Excel</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="preview-card">
      <template #header>导出说明</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="文件格式">Excel 工作簿（.xlsx）</el-descriptions-item>
        <el-descriptions-item label="时间列">按北京时间显示</el-descriptions-item>
        <el-descriptions-item label="数据组织">每个时间点一行，每个点位一列</el-descriptions-item>
        <el-descriptions-item label="导出限制">单次最多 31 天、30 个点位</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import { pageDevices } from '@/api/device'
import { exportTelemetryReport } from '@/api/report'
import { downloadFile } from '@/utils/format'
import type { DeviceVO } from '@/types/api'

interface ExportForm {
  sn: string
  interval: string
  timeRange: string[]
  fields: string[]
}

const formRef = ref<FormInstance>()
const devices = ref<DeviceVO[]>([])
const deviceLoading = ref(false)
const exporting = ref(false)

const form = reactive<ExportForm>({
  sn: '',
  interval: '10m',
  timeRange: [],
  fields: ['P1', 'P2']
})

const rules: FormRules<ExportForm> = {
  sn: [{ required: true, message: '请选择设备', trigger: 'change' }],
  interval: [{ required: true, message: '请选择统计间隔', trigger: 'change' }],
  timeRange: [{
    validator: (_rule, value, callback) => {
      if (!Array.isArray(value) || value.length !== 2) callback(new Error('请选择时间范围'))
      else callback()
    },
    trigger: 'change'
  }],
  fields: [{
    validator: (_rule, value, callback) => {
      if (!Array.isArray(value) || value.length === 0) callback(new Error('请选择导出数据'))
      else callback()
    },
    trigger: 'change'
  }]
}

const intervalOptions = [
  { label: '1秒', value: '1s' },
  { label: '10秒', value: '10s' },
  { label: '30秒', value: '30s' },
  { label: '1分钟', value: '1m' },
  { label: '5分钟', value: '5m' },
  { label: '10分钟', value: '10m' },
  { label: '30分钟', value: '30m' },
  { label: '1小时', value: '1h' },
  { label: '6小时', value: '6h' },
  { label: '12小时', value: '12h' },
  { label: '1天', value: '1d' }
]

const pointGroups = [
  {
    label: '水质与温度',
    options: [
      { value: 'P1', label: 'P1 - 原水TDS (PPM)' },
      { value: 'P2', label: 'P2 - 纯水TDS (PPM)' },
      { value: 'P3', label: 'P3 - 矿水TDS (PPM)' },
      { value: 'P4', label: 'P4 - 原水温度 (℃)' },
      { value: 'P5', label: 'P5 - 纯水温度 (℃)' },
      { value: 'P6', label: 'P6 - 矿水温度 (℃)' }
    ]
  },
  {
    label: '流量',
    options: [
      { value: 'P7', label: 'P7 - 纯水瞬时流量 (L/min)' },
      { value: 'P8', label: 'P8 - 净水瞬时流量 (L/min)' },
      { value: 'P9', label: 'P9 - 矿水瞬时流量 (L/min)' },
      { value: 'P10', label: 'P10 - 废水瞬时流量 (L/min)' },
      { value: 'P11', label: 'P11 - 原水瞬时流量 (L/min)' },
      { value: 'P12', label: 'P12 - 纯水累计流量 (L)' },
      { value: 'P13', label: 'P13 - 净水累计流量 (L)' },
      { value: 'P14', label: 'P14 - 矿水累计流量 (L)' },
      { value: 'P15', label: 'P15 - 废水累计流量 (L)' },
      { value: 'P16', label: 'P16 - 原水累计流量 (L)' }
    ]
  },
  {
    label: '压力',
    options: [
      { value: 'P17', label: 'P17 - 原水压力 (bar)' },
      { value: 'P18', label: 'P18 - 膜前压力 (bar)' },
      { value: 'P19', label: 'P19 - 膜后压力 (bar)' },
      { value: 'P20', label: 'P20 - 矿水压力 (bar)' }
    ]
  },
  {
    label: '状态',
    options: [
      { value: 'P23', label: 'P23 - 制水状态' },
      { value: 'P24', label: 'P24 - TDS制水状态' },
      { value: 'P25', label: 'P25 - RO强冲状态' },
      { value: 'P26', label: 'P26 - 纯水洗膜状态' },
      { value: 'P27', label: 'P27 - 超滤冲洗状态' },
      { value: 'P28', label: 'P28 - 故障报警' },
      { value: 'P29', label: 'P29 - 比例阀状态' },
      { value: 'P31', label: 'P31 - 高压开关' },
      { value: 'P32', label: 'P32 - 低压开关' },
      { value: 'P33', label: 'P33 - 漏水状态' }
    ]
  }
]

const dateShortcuts = [
  {
    text: '近1小时',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000)
      return [start, end]
    }
  },
  {
    text: '近24小时',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 24 * 3600 * 1000)
      return [start, end]
    }
  },
  {
    text: '近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 7 * 24 * 3600 * 1000)
      return [start, end]
    }
  }
]

async function loadDevices() {
  deviceLoading.value = true
  try {
    const res = await pageDevices({ current: 1, size: 200 })
    devices.value = res.data?.records || []
  } catch (e) {
    ElMessage.error('加载设备列表失败')
  } finally {
    deviceLoading.value = false
  }
}

function formatDeviceLabel(device: DeviceVO): string {
  const parts = [device.deviceId, device.sn, device.modelName].filter(Boolean)
  return parts.join(' / ')
}

function resolveFilename(disposition?: string): string {
  if (!disposition) return '设备历史数据.xlsx'
  const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (utf8Match?.[1]) return decodeURIComponent(utf8Match[1])
  const filenameMatch = /filename="?([^";]+)"?/i.exec(disposition)
  return filenameMatch?.[1] || '设备历史数据.xlsx'
}

async function handleExport() {
  if (!formRef.value) return
  await formRef.value.validate()
  exporting.value = true
  try {
    const response = await exportTelemetryReport({
      sn: form.sn,
      fields: form.fields.join(','),
      startTime: form.timeRange[0],
      endTime: form.timeRange[1],
      interval: form.interval
    }) as any
    downloadFile(response.data, resolveFilename(response.headers?.['content-disposition']))
    ElMessage.success('导出完成')
  } catch (e) {
    ElMessage.error('导出失败，请检查设备、时间范围和数据间隔')
  } finally {
    exporting.value = false
  }
}

function handleReset() {
  form.sn = ''
  form.interval = '10m'
  form.timeRange = []
  form.fields = ['P1', 'P2']
  formRef.value?.clearValidate()
}

onMounted(loadDevices)
</script>

<style scoped lang="scss">
.data-export-page {
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.full-width {
  width: 100%;
}

.preview-card {
  margin-top: 16px;
}
</style>
