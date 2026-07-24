<template>
  <div class="device-detail-page" v-loading="loading">
    <!-- 设备基本信息 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <span class="card-title">设备信息</span>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="设备ID">{{ device.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="SN">{{ device.sn }}</el-descriptions-item>
        <el-descriptions-item label="型号">{{ device.modelName }}</el-descriptions-item>
        <el-descriptions-item label="ICCID">{{ device.iccid || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IMEI">{{ device.imei || '-' }}</el-descriptions-item>
        <el-descriptions-item label="生产日期">
          {{ device.productionDate ? formatDateTime(device.productionDate, 'YYYY-MM-DD') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="生产批次">{{ device.productionBatch || '-' }}</el-descriptions-item>
        <el-descriptions-item label="在线状态">
          <el-tag :type="(device.onlineStatus === 1 || device.onlineStatus === 'ONLINE') ? 'success' : 'info'" size="small">
            {{ (device.onlineStatus === 1 || device.onlineStatus === 'ONLINE') ? '在线' : '离线' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="生命周期状态">
          <el-tag :type="statusTagType(device.lifecycleStatus)" size="small">
            {{ statusLabel(device.lifecycleStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="绑定客户">{{ device.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属经销商">{{ device.dealerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="激活时间">
          {{ device.activatedAt ? formatDateTime(device.activatedAt, 'YYYY-MM-DD HH:mm:ss') : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="入库时间">
          {{ (device.updatedAt || device.createdAt) ? formatDateTime(device.updatedAt || device.createdAt, 'YYYY-MM-DD HH:mm:ss') : '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- 二维码 -->
      <div v-if="device.qrCodeUrl" class="qrcode-section">
        <span class="qrcode-label">设备二维码：</span>
        <div class="qrcode-box">
          <el-image
            :src="device.qrCodeUrl"
            style="width: 160px; height: 160px"
            fit="contain"
            :preview-src-list="[device.qrCodeUrl]"
            preview-teleported
          />
          <el-button type="primary" link size="small" @click="downloadQR">下载二维码</el-button>
        </div>
      </div>
    </el-card>

    <!-- 绑定滤芯信息 -->
    <el-card shadow="never" class="filter-card">
      <template #header>
        <span class="card-title">绑定滤芯</span>
      </template>
      <el-table v-loading="filterLoading" :data="deviceFilters" border stripe size="small" style="width: 100%">
        <el-table-column prop="filterId" label="滤芯编号" min-width="140" />
        <el-table-column prop="modelName" label="型号" min-width="120">
          <template #default="{ row }">{{ row.modelName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="filterStatusType(row.lifecycleStatus)" size="small">
              {{ filterStatusLabel(row.lifecycleStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剩余寿命" width="120" align="center">
          <template #default="{ row }">
            <span :style="{ color: (row.remainPercentage || 0) <= 30 ? '#f56c6c' : '#67c23a' }">
              {{ row.remainPercentage || 0 }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="安装时间" min-width="170">
          <template #default="{ row }">
            {{ row.installedAt ? formatDateTime(row.installedAt, 'YYYY-MM-DD HH:mm:ss') : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="报废时间" min-width="170">
          <template #default="{ row }">
            {{ row.scrappedAt ? formatDateTime(row.scrappedAt, 'YYYY-MM-DD HH:mm:ss') : '-' }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!filterLoading && deviceFilters.length === 0" description="该设备暂无绑定滤芯" :image-size="60" />
    </el-card>

    <!-- IoT 遥测数据 -->
    <el-card shadow="never" class="telemetry-card">
      <template #header>
        <div class="telemetry-header">
          <span class="card-title">IoT 实时遥测数据</span>
          <div class="telemetry-actions">
            <el-tag v-if="telemetry?.online" type="success" size="small" effect="dark">
              <el-icon class="blink-dot" style="margin-right: 4px"><Connection /></el-icon>
              在线
            </el-tag>
            <el-tag v-else type="info" size="small">离线</el-tag>
            <el-tooltip :content="autoRefresh ? '点击停止自动刷新' : '点击开始自动刷新'" placement="top">
              <el-button
                :type="autoRefresh ? 'danger' : 'primary'"
                size="small"
                circle
                @click="toggleAutoRefresh"
              >
                <el-icon><Refresh v-if="!autoRefresh" /><VideoPause v-else /></el-icon>
              </el-button>
            </el-tooltip>
            <el-button type="primary" size="small" plain @click="loadTelemetry(device.sn!)">
              <el-icon style="margin-right: 4px"><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 遥测时间戳 -->
      <div v-if="telemetry?.timestamp" class="telemetry-timestamp">
        最后更新：{{ formatTelemetryTime(telemetry.timestamp) }}
      </div>

      <!-- 分组展示遥测数据 -->
      <div v-if="groupedPoints && groupedPoints.length > 0" class="telemetry-groups">
        <div v-for="group in groupedPoints" :key="group.category" class="telemetry-group">
          <div class="group-title">{{ group.label }}</div>
          <div class="group-items">
            <div v-for="item in group.items" :key="item.pointId" class="telemetry-item">
              <span class="telemetry-key">{{ item.name }}</span>
              <span class="telemetry-value" :class="{ 'value-on': item.isBinary && item.value === 1, 'value-off': item.isBinary && item.value === 0 }">
                {{ item.displayValue }}
              </span>
              <span v-if="item.unit" class="telemetry-unit">{{ item.unit }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无遥测数据，请确认设备已连接并发送数据" />
    </el-card>

    <!-- 历史数据曲线图 -->
    <el-card shadow="never" class="history-card">
      <template #header>
        <div class="history-header">
          <span class="card-title">历史数据曲线</span>
          <div class="history-controls">
            <el-radio-group v-model="historyRange" size="small" @change="loadHistory">
              <el-radio-button label="1h">1小时</el-radio-button>
              <el-radio-button label="6h">6小时</el-radio-button>
              <el-radio-button label="24h">24小时</el-radio-button>
              <el-radio-button label="7d">7天</el-radio-button>
              <el-radio-button label="30d">30天</el-radio-button>
            </el-radio-group>
            <el-select
              v-model="historyInterval"
              size="small"
              class="history-interval-select"
              @change="loadHistory"
            >
              <el-option label="自动间隔" value="auto" />
              <el-option label="1秒" value="1s" />
              <el-option label="10秒" value="10s" />
              <el-option label="30秒" value="30s" />
              <el-option label="1分钟" value="1m" />
              <el-option label="5分钟" value="5m" />
              <el-option label="10分钟" value="10m" />
              <el-option label="30分钟" value="30m" />
              <el-option label="1小时" value="1h" />
              <el-option label="6小时" value="6h" />
            </el-select>
          </div>
        </div>
      </template>

      <div class="history-field-select">
        <el-select
          v-model="selectedFields"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择要查看的测点（默认显示水质+流量+压力）"
          filterable
          style="width: 100%"
          @change="loadHistory"
        >
          <el-option-group
            v-for="group in chartableGroups"
            :key="group.category"
            :label="group.label"
          >
            <el-option
              v-for="pt in group.items"
              :key="pt.id"
              :label="pt.id + ' - ' + pt.name + (pt.unit ? ' (' + pt.unit + ')' : '')"
              :value="pt.id"
            />
          </el-option-group>
        </el-select>
      </div>

      <div v-loading="historyLoading" class="chart-container">
        <div ref="historyChartRef" style="width: 100%; height: 420px"></div>
        <div
          v-for="tip in pinnedHistoryTooltips"
          :key="tip.key"
          class="history-pinned-tooltip"
          :style="{ left: tip.left + 'px', top: tip.top + 'px' }"
        >
          <div class="pinned-header">
            <span class="pinned-badge">已固定</span>
            <span class="pinned-time">{{ tip.timeLabel }}</span>
            <button type="button" class="pinned-remove" @click.stop="removePinnedHistoryTooltip(tip.key)">
              取消固定
            </button>
          </div>
          <div v-for="item in tip.values" :key="item.seriesName" class="pinned-row">
            <span class="pinned-marker" :style="{ backgroundColor: item.color }"></span>
            <span class="pinned-name">{{ item.seriesName }}:</span>
            <strong>{{ item.value.toFixed(2) }}</strong>
          </div>
        </div>
        <el-empty v-if="!historyLoading && !hasHistoryData" description="暂无历史数据，请确认设备已上报数据到 InfluxDB" />
      </div>
    </el-card>

    <!-- 配置下发 -->
    <el-card shadow="never" class="command-card">
      <template #header>
        <span class="card-title">配置下发</span>
        <el-tag size="small" type="info" style="margin-left: 8px">主题: api/v2/set/{{ device.sn }}</el-tag>
      </template>

      <!-- 快捷下发：MQTT 服务器地址 + 端口 -->
      <div class="quick-set-section">
        <div class="quick-set-title">快捷下发</div>
        <div class="quick-set-grid">
          <div class="quick-set-item">
            <div class="quick-set-label">
              <el-tag size="small" type="warning">Q34</el-tag>
              MQTT 服务器地址
            </div>
            <el-input
              v-model="mqttServerInput"
              placeholder="如 cloud.juconyun.com"
              clearable
              style="width: 240px"
            />
            <el-button
              type="primary"
              size="small"
              :loading="setSending === 'Q34'"
              :disabled="!mqttServerInput.trim()"
              @click="handleQuickSet('Q34', mqttServerInput.trim(), 'MQTT服务器地址')"
            >
              下发
            </el-button>
          </div>

          <div class="quick-set-item">
            <div class="quick-set-label">
              <el-tag size="small" type="warning">Q54</el-tag>
              MQTT 端口
            </div>
            <el-input
              v-model="mqttPortInput"
              placeholder="如 1883"
              clearable
              style="width: 120px"
            />
            <el-button
              type="primary"
              size="small"
              :loading="setSending === 'Q54'"
              :disabled="!mqttPortInput.trim()"
              @click="handleQuickSet('Q54', mqttPortInput.trim(), 'MQTT端口')"
            >
              下发
            </el-button>
          </div>
        </div>
      </div>

      <el-divider content-position="left">自定义点位下发</el-divider>

      <!-- 通用配置下发 -->
      <div class="custom-set-body">
        <el-select
          v-model="customPointID"
          placeholder="选择点位"
          filterable
          allow-create
          style="width: 180px"
        >
          <el-option
            v-for="opt in settablePoints"
            :key="opt.id"
            :label="opt.id + ' - ' + opt.name"
            :value="opt.id"
          />
        </el-select>
        <el-input
          v-model="customValue"
          placeholder="要写入的值"
          clearable
          style="width: 240px; margin-left: 12px"
        />
        <el-button
          type="primary"
          :loading="setSending === 'CUSTOM'"
          :disabled="!customPointID || !customValue.trim()"
          style="margin-left: 12px"
          @click="handleCustomSet"
        >
          下发配置
        </el-button>
      </div>
    </el-card>

    <!-- 返回按钮 -->
    <div class="footer-actions">
      <el-button @click="handleBack">返回列表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, VideoPause, Connection } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDevice } from '@/api/device'
import { pageFilters } from '@/api/filter'
import { getLatestTelemetry, sendSetCommand, getHistoryTelemetry } from '@/api/iot'
import type { DeviceVO, TelemetryVO, FilterInstanceVO } from '@/types/api'
import { formatDateTime, statusLabel, statusTagType } from '@/utils/format'
import { getPointInfo, formatPointValue, CATEGORY_LABELS, type PointCategory } from '@/utils/pointMapping'

const router = useRouter()
const route = useRoute()

const deviceId = route.params.deviceId as string

const loading = ref(false)
const device = reactive<Partial<DeviceVO>>({})
const telemetry = ref<TelemetryVO | null>(null)
const autoRefresh = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

// 滤芯数据
const filterLoading = ref(false)
const deviceFilters = ref<FilterInstanceVO[]>([])

// 配置下发相关
const mqttServerInput = ref('')
const mqttPortInput = ref('')
const customPointID = ref('')
const customValue = ref('')
const setSending = ref<string | null>(null)

// 历史数据曲线相关
const historyChartRef = ref<HTMLElement>()
let historyChart: echarts.ECharts | null = null
const historyLoading = ref(false)
const historyRange = ref('24h')
const historyInterval = ref('auto')
const selectedFields = ref<string[]>(['P1', 'P2', 'P7', 'P17'])
const hasHistoryData = ref(false)
const HISTORY_CHART_COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc']
type HistoryChartSeriesSnapshot = {
  name: string
  data: (number | null)[]
  color: string
}
type HistoryPinnedTooltip = {
  key: string
  dataIndex: number
  axisValue: string | number
  anchorValue: number
  timeLabel: string
  left: number
  top: number
  values: { seriesName: string; value: number; color: string }[]
}
const pinnedHistoryTooltips = ref<HistoryPinnedTooltip[]>([])
let currentHistoryTimes: string[] = []
let currentHistorySeries: HistoryChartSeriesSnapshot[] = []

// 可绘图测点分组（仅数值型、有趋势意义的测点）
const chartableGroups = computed(() => {
  const groups: { category: string; label: string; items: { id: string; name: string; unit: string }[] }[] = []
  const categories: { key: PointCategory; label: string; ids: string[] }[] = [
    { key: 'quality', label: '水质参数', ids: ['P1', 'P2', 'P3', 'P4', 'P5', 'P6'] },
    { key: 'flow', label: '瞬时流量', ids: ['P7', 'P8', 'P9', 'P10', 'P11'] },
    { key: 'pressure', label: '压力数据', ids: ['P17', 'P18', 'P19', 'P20'] },
    { key: 'valve', label: '阀门开度', ids: ['P21', 'P22'] },
  ]
  for (const cat of categories) {
    const items = cat.ids.map(id => {
      const info = getPointInfo(id)
      return { id, name: info.name, unit: info.unit }
    })
    groups.push({ category: cat.key, label: cat.label, items })
  }
  return groups
})

// 可下发的 Q 系列点位列表
const settablePoints = computed(() => {
  const ids = [
    'Q1', 'Q2', 'Q3', 'Q4', 'Q5', 'Q6', 'Q7', 'Q8', 'Q9', 'Q10',
    'Q11', 'Q12', 'Q13', 'Q14', 'Q15',
    'Q26', 'Q27', 'Q28', 'Q29', 'Q30', 'Q31', 'Q32', 'Q33',
    'Q34', 'Q54', 'Q74',
    'Q83', 'Q84', 'Q89', 'Q90'
  ]
  return ids.map(id => ({ id, name: getPointInfo(id).name }))
})

// 计算分组遥测数据
const groupedPoints = computed(() => {
  if (!telemetry.value?.points) return []

  const groups = new Map<PointCategory, { category: PointCategory; label: string; items: any[] }>()

  for (const [pointId, value] of Object.entries(telemetry.value.points)) {
    const info = getPointInfo(pointId)
    if (!groups.has(info.category)) {
      groups.set(info.category, {
        category: info.category,
        label: CATEGORY_LABELS[info.category],
        items: []
      })
    }
    const isBinary = typeof value === 'number' && !info.unit && [0, 1].includes(value)
    groups.get(info.category)!.items.push({
      pointId,
      name: info.name,
      value,
      unit: info.unit,
      displayValue: formatPointValue(pointId, value),
      isBinary,
    })
  }

  // 按预定义类别顺序排列
  const order: PointCategory[] = ['quality', 'flow', 'pressure', 'valve', 'status', 'config']
  return order
    .filter(cat => groups.has(cat))
    .map(cat => groups.get(cat)!)
})

async function loadDevice() {
  loading.value = true
  try {
    const res = await getDevice(deviceId)
    if (res.code === 200) {
      Object.assign(device, res.data)

      // 加载滤芯数据
      loadDeviceFilters()

      // 加载遥测数据
      if (res.data.sn) {
        loadTelemetry(res.data.sn)
        // 加载历史数据曲线
        nextTick(() => loadHistory())
      }
    }
  } finally {
    loading.value = false
  }
}

async function loadDeviceFilters() {
  filterLoading.value = true
  try {
    const res = await pageFilters({ currentDeviceId: deviceId, pageSize: 50 } as any)
    if (res.code === 200) {
      deviceFilters.value = res.data?.records || []
    } else {
      deviceFilters.value = []
    }
  } catch {
    deviceFilters.value = []
  } finally {
    filterLoading.value = false
  }
}

function filterStatusLabel(status: string): string {
  const map: Record<string, string> = {
    IN_STOCK: '在库',
    PENDING_INSTALL: '待安装',
    IN_USE: '使用中',
    SCRAPPED: '已报废',
  }
  return map[status] || status || '-'
}

function filterStatusType(status: string): string {
  const map: Record<string, string> = {
    IN_STOCK: 'info',
    PENDING_INSTALL: 'warning',
    IN_USE: 'success',
    SCRAPPED: 'danger',
  }
  return map[status] || 'info'
}

async function loadTelemetry(sn: string) {
  try {
    const res = await getLatestTelemetry(sn)
    if (res.code === 200 && res.data) {
      telemetry.value = res.data
      // 同步设备在线状态：遥测数据在线 → 设备在线
      if (res.data.online && device.onlineStatus !== 1) {
        try {
          const devRes = await getDevice(deviceId)
          if (devRes.code === 200 && devRes.data) {
            Object.assign(device, devRes.data)
          }
        } catch {
          // ignore refresh failure
        }
      }
    } else {
      telemetry.value = null
    }
  } catch {
    telemetry.value = null
  }
}

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    refreshTimer = setInterval(() => {
      if (device.sn) {
        loadTelemetry(device.sn)
      }
    }, 5000)
    ElMessage.success('已开启自动刷新（每5秒）')
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
    ElMessage.info('已停止自动刷新')
  }
}

function formatTelemetryTime(timestamp: string): string {
  try {
    const date = new Date(timestamp)
    return formatDateTime(date, 'YYYY-MM-DD HH:mm:ss')
  } catch {
    return timestamp
  }
}

function formatHistoryTime(value: string | number | Date | undefined, full = false): string {
  if (value === undefined || value === null || value === '') return '-'
  const fmt = full
    ? 'YYYY-MM-DD HH:mm:ss'
    : (historyRange.value === '7d' || historyRange.value === '30d' ? 'MM-DD HH:mm' : 'HH:mm:ss')
  const formatted = formatDateTime(value, fmt)
  return formatted === 'Invalid Date' ? String(value) : formatted
}

/** 下载设备二维码 */
function downloadQR() {
  if (!device.qrCodeUrl) return
  const link = document.createElement('a')
  link.href = device.qrCodeUrl
  link.download = `QR_${device.sn || deviceId}.png`
  link.click()
}

/** 快捷下发配置（Q34/Q54 等） */
async function handleQuickSet(pointID: string, value: string, label: string) {
  if (!device.sn) return
  setSending.value = pointID
  try {
    const res = await sendSetCommand(device.sn, pointID, value)
    if (res.code === 200) {
      ElMessage.success(`${label} 下发成功（${pointID}=${value}）`)
      if (pointID === 'Q34') mqttServerInput.value = ''
      if (pointID === 'Q54') mqttPortInput.value = ''
    }
  } catch {
    ElMessage.error(`${label} 下发失败`)
  } finally {
    setSending.value = null
  }
}

/** 自定义点位下发 */
async function handleCustomSet() {
  if (!device.sn || !customPointID.value || !customValue.value.trim()) return
  setSending.value = 'CUSTOM'
  try {
    const res = await sendSetCommand(device.sn, customPointID.value, customValue.value.trim())
    if (res.code === 200) {
      ElMessage.success(`配置下发成功（${customPointID.value}=${customValue.value}）`)
      customValue.value = ''
    }
  } catch {
    ElMessage.error('配置下发失败')
  } finally {
    setSending.value = null
  }
}

function handleBack() {
  router.push('/devices/list')
}

/** 加载历史遥测数据并渲染 ECharts 曲线图 */
async function loadHistory() {
  if (!device.sn) return
  if (selectedFields.value.length === 0) {
    ElMessage.warning('请至少选择一个测点')
    return
  }

  clearPinnedHistoryTooltips(false)
  historyLoading.value = true
  try {
    const res = await getHistoryTelemetry(device.sn, {
      range: historyRange.value,
      fields: selectedFields.value.join(','),
      interval: historyInterval.value === 'auto' ? undefined : historyInterval.value,
    })

    if (res.code === 200 && res.data?.series?.length > 0) {
      hasHistoryData.value = true
      await nextTick()
      renderHistoryChart(res.data.series, res.data.interval)
    } else {
      hasHistoryData.value = false
      if (historyChart) {
        historyChart.clear()
      }
    }
  } catch {
    hasHistoryData.value = false
    ElMessage.error('加载历史数据失败')
  } finally {
    historyLoading.value = false
  }
}

function handleHistoryChartClick(params: any) {
  if (params?.componentType !== 'series' || typeof params.dataIndex !== 'number') return

  const axisValue = params.name ?? params.axisValue ?? currentHistoryTimes[params.dataIndex] ?? ''
  const key = getPinnedHistoryTooltipKey(params.dataIndex, axisValue)
  if (pinnedHistoryTooltips.value.some(item => item.key === key)) return

  const anchorValue = typeof params.value === 'number' ? params.value : getFirstHistoryValue(params.dataIndex)
  pinnedHistoryTooltips.value.push(createPinnedHistoryTooltip(params.dataIndex, axisValue, anchorValue))
}

function removePinnedHistoryTooltip(key: string) {
  pinnedHistoryTooltips.value = pinnedHistoryTooltips.value.filter(item => item.key !== key)
}

function getPinnedHistoryTooltipKey(dataIndex: number, axisValue: string | number) {
  return `${dataIndex}:${String(axisValue)}`
}

function getFirstHistoryValue(dataIndex: number) {
  for (const s of currentHistorySeries) {
    const value = s.data[dataIndex]
    if (typeof value === 'number') return value
  }
  return 0
}

function createPinnedHistoryTooltip(dataIndex: number, axisValue: string | number, anchorValue: number): HistoryPinnedTooltip {
  const key = getPinnedHistoryTooltipKey(dataIndex, axisValue)
  const values = currentHistorySeries
    .map(s => ({ seriesName: s.name, value: s.data[dataIndex], color: s.color }))
    .filter((item): item is { seriesName: string; value: number; color: string } => typeof item.value === 'number')

  return {
    key,
    dataIndex,
    axisValue,
    anchorValue,
    timeLabel: formatHistoryTime(axisValue, true),
    values,
    ...calculatePinnedHistoryTooltipPosition(axisValue, anchorValue),
  }
}

function calculatePinnedHistoryTooltipPosition(axisValue: string | number, anchorValue: number) {
  if (!historyChart) return { left: 8, top: 8 }
  const pixel = historyChart.convertToPixel({ xAxisIndex: 0, yAxisIndex: 0 }, [axisValue, anchorValue]) as number[]
  const chartDom = historyChart.getDom()
  const maxLeft = Math.max(chartDom.clientWidth - 260, 8)
  const maxTop = Math.max(chartDom.clientHeight - 150, 8)
  const left = Math.min(Math.max((pixel?.[0] ?? 0) + 12, 8), maxLeft)
  const top = Math.min(Math.max((pixel?.[1] ?? 0) - 24, 8), maxTop)
  return { left, top }
}

function updatePinnedHistoryTooltipPositions() {
  if (!historyChart || pinnedHistoryTooltips.value.length === 0) return
  pinnedHistoryTooltips.value = pinnedHistoryTooltips.value.map(item => ({
    ...item,
    ...calculatePinnedHistoryTooltipPosition(item.axisValue, item.anchorValue),
  }))
}

function clearPinnedHistoryTooltips(hide = true) {
  pinnedHistoryTooltips.value = []
  if (hide && historyChart) {
    historyChart.dispatchAction({ type: 'hideTip' })
  }
}

function handleHistoryChartViewChanged() {
  updatePinnedHistoryTooltipPositions()
}

function handleHistoryResize() {
  if (!historyChart) return
  historyChart.resize()
  updatePinnedHistoryTooltipPositions()
}

/** 使用 ECharts 渲染历史数据曲线图 */
function renderHistoryChart(series: any[], interval: string) {
  if (!historyChartRef.value) return

  if (!historyChart) {
    historyChart = echarts.init(historyChartRef.value)
  }
  historyChart.off('click', handleHistoryChartClick)
  historyChart.on('click', handleHistoryChartClick)
  historyChart.off('datazoom', handleHistoryChartViewChanged)
  historyChart.on('datazoom', handleHistoryChartViewChanged)

  // 收集所有时间戳（取并集）
  const allTimes = new Set<string>()
  for (const s of series) {
    for (const point of s.data) {
      allTimes.add(point.time)
    }
  }
  const sortedTimes = Array.from(allTimes).sort()

  // 为每条曲线构建数据（以索引对齐时间轴）
  const chartSeries = series.map((s, index) => {
    const info = getPointInfo(s.field)
    const label = info.name + (info.unit ? ` (${info.unit})` : '')
    const color = HISTORY_CHART_COLORS[index % HISTORY_CHART_COLORS.length]
    // 创建 time→value 映射
    const valueMap = new Map<string, number>()
    for (const point of s.data) {
      valueMap.set(point.time, point.value)
    }
    // 按时间轴对齐，缺失数据用 null
    const data = sortedTimes.map(t => {
      const v = valueMap.get(t)
      return v !== undefined ? v : null
    })
    return {
      name: label,
      type: 'line' as const,
      data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      showSymbol: false,
      connectNulls: false,
      itemStyle: { color },
      lineStyle: { color },
    }
  })
  currentHistoryTimes = sortedTimes
  currentHistorySeries = chartSeries.map(s => ({
    name: s.name,
    data: s.data,
    color: s.itemStyle.color,
  }))

  const rangeLabels: Record<string, string> = {
    '1h': '近1小时',
    '6h': '近6小时',
    '24h': '近24小时',
    '7d': '近7天',
    '30d': '近30天',
  }

  historyChart.setOption({
    title: {
      text: `${device.sn} - ${rangeLabels[historyRange.value] || historyRange.value}（间隔: ${interval}）`,
      left: 'center',
      textStyle: { fontSize: 14, fontWeight: 500 },
    },
    tooltip: {
      trigger: 'axis',
      triggerOn: 'mousemove',
      alwaysShowContent: false,
      confine: true,
      axisPointer: { type: 'cross' },
      formatter: (params: any) => {
        if (!params || params.length === 0) return ''
        const time = formatHistoryTime(params[0].axisValue ?? params[0].name, true)
        let html = `<div style="font-weight:600;margin-bottom:4px">${time}</div>`
        for (const p of params) {
          if (p.value === null || p.value === undefined) continue
          html += `<div>${p.marker} ${p.seriesName}: <b>${Number(p.value).toFixed(2)}</b></div>`
        }
        return html
      },
    },
    legend: {
      bottom: 0,
      type: 'scroll',
    },
    grid: {
      left: '3%',
      right: '3%',
      bottom: '12%',
      top: '15%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: sortedTimes,
      axisLabel: {
        formatter: (val: string) => formatHistoryTime(val),
        rotate: 30,
        fontSize: 10,
      },
      axisPointer: {
        label: {
          formatter: (params: any) => formatHistoryTime(params.value, true),
        },
      },
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 11 },
    },
    dataZoom: [
      { type: 'inside', start: 0, end: 100 },
      {
        type: 'slider',
        start: 0,
        end: 100,
        height: 20,
        bottom: 30,
        labelFormatter: (_value: number, valueStr: string) => formatHistoryTime(valueStr, true),
      },
    ],
    series: chartSeries,
  }, true) // true = notMerge, replace entirely
}

onMounted(() => {
  window.addEventListener('resize', handleHistoryResize)
  loadDevice()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleHistoryResize)
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  if (historyChart) {
    historyChart.off('click', handleHistoryChartClick)
    historyChart.off('datazoom', handleHistoryChartViewChanged)
    historyChart.dispose()
    historyChart = null
  }
})
</script>

<style scoped lang="scss">
.device-detail-page {
  padding: 16px;

  .card-title {
    font-size: 18px;
    font-weight: 600;
  }

  .info-card {
    margin-bottom: 16px;

    .qrcode-section {
      display: flex;
      align-items: flex-start;
      margin-top: 16px;
      padding-top: 16px;
      border-top: 1px solid #ebeef5;

      .qrcode-label {
        margin-right: 12px;
        line-height: 32px;
        color: #606266;
      }

      .qrcode-box {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
      }
    }
  }

  .filter-card {
    margin-bottom: 16px;
  }

  .telemetry-card {
    margin-bottom: 16px;

    .telemetry-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .telemetry-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .telemetry-timestamp {
      color: #909399;
      font-size: 13px;
      margin-bottom: 16px;
    }

    .telemetry-groups {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .telemetry-group {
      .group-title {
        font-size: 14px;
        font-weight: 600;
        color: #409eff;
        margin-bottom: 10px;
        padding-left: 8px;
        border-left: 3px solid #409eff;
      }

      .group-items {
        display: flex;
        flex-wrap: wrap;
        gap: 12px;
      }
    }

    .telemetry-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 10px 18px;
      background: #f5f7fa;
      border-radius: 8px;
      min-width: 110px;
      transition: all 0.3s;

      &:hover {
        background: #ecf5ff;
        transform: translateY(-2px);
      }

      .telemetry-key {
        font-size: 12px;
        color: #909399;
        margin-bottom: 6px;
      }

      .telemetry-value {
        font-size: 20px;
        font-weight: 700;
        color: #303133;

        &.value-on {
          color: #67c23a;
        }

        &.value-off {
          color: #f56c6c;
        }
      }

      .telemetry-unit {
        font-size: 11px;
        color: #c0c4cc;
        margin-top: 2px;
      }
    }
  }

  .history-card {
    margin-bottom: 16px;

    .history-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }

    .history-controls {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }

    .history-interval-select {
      width: 108px;
    }

    .history-field-select {
      margin-bottom: 16px;
    }

    .chart-container {
      position: relative;
      min-height: 420px;

      .history-pinned-tooltip {
        position: absolute;
        z-index: 5;
        width: 320px;
        max-height: 180px;
        overflow: auto;
        padding: 10px 12px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        background: rgba(255, 255, 255, 0.96);
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.14);
        pointer-events: auto;

        .pinned-header {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 8px;
          margin-bottom: 6px;
          min-height: 24px;

          .pinned-badge {
            flex: 0 0 auto;
            padding: 0 5px;
            border-radius: 3px;
            background: #ecf5ff;
            color: #409eff;
            font-size: 12px;
            line-height: 20px;
          }

          .pinned-time {
            flex: 1 1 170px;
            white-space: nowrap;
            color: #606266;
            font-size: 14px;
            font-weight: 600;
            line-height: 20px;
          }

          .pinned-remove {
            flex: 0 0 auto;
            margin-left: auto;
            padding: 0;
            border: 0;
            background: transparent;
            color: #f56c6c;
            font-size: 12px;
            line-height: 20px;
            cursor: pointer;

            &:hover {
              color: #d93026;
              text-decoration: underline;
            }
          }
        }

        .pinned-row {
          display: flex;
          align-items: center;
          gap: 6px;
          min-height: 22px;
          color: #606266;
          font-size: 13px;
          line-height: 20px;
          white-space: nowrap;

          .pinned-marker {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            flex: 0 0 auto;
          }

          .pinned-name {
            overflow: hidden;
            text-overflow: ellipsis;
          }

          strong {
            color: #303133;
            margin-left: auto;
          }
        }
      }

      .el-empty {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
      }
    }
  }

  .command-card {
    margin-bottom: 16px;

    .quick-set-section {
      .quick-set-title {
        font-size: 14px;
        font-weight: 600;
        color: #606266;
        margin-bottom: 12px;
      }

      .quick-set-grid {
        display: flex;
        gap: 32px;
        flex-wrap: wrap;
      }

      .quick-set-item {
        display: flex;
        align-items: center;
        gap: 8px;

        .quick-set-label {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 13px;
          color: #606266;
          white-space: nowrap;
        }
      }
    }

    .custom-set-body {
      display: flex;
      align-items: center;
    }
  }

  .footer-actions {
    padding-top: 8px;
  }
}

.blink-dot {
  animation: blink 1.5s infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
