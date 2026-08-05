<template>
  <div class="ro-page">
    <el-card shadow="never" class="search-card">
      <template #header>
        <div class="card-header">
          <span>RO膜寿命 AI 预测</span>
          <el-button :icon="Refresh" :loading="deviceLoading" @click="loadDevices">刷新设备</el-button>
        </div>
      </template>

      <el-form class="search-form" :inline="true" :model="queryForm">
        <el-form-item label="设备">
          <el-select
            v-model="queryForm.sn"
            filterable
            placeholder="请选择设备"
            style="width: 360px"
            :loading="deviceLoading"
          >
            <el-option
              v-for="device in devices"
              :key="device.sn"
              :label="deviceLabel(device)"
              :value="device.sn"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="分析范围">
          <el-select v-model="queryForm.rangeDays" style="width: 120px">
            <el-option label="7天" :value="7" />
            <el-option label="30天" :value="30" />
            <el-option label="90天" :value="90" />
            <el-option label="180天" :value="180" />
          </el-select>
        </el-form-item>
        <el-form-item label="统计间隔">
          <el-select v-model="queryForm.aggregateEvery" style="width: 120px">
            <el-option label="10分钟" value="10m" />
            <el-option label="30分钟" value="30m" />
            <el-option label="1小时" value="1h" />
            <el-option label="2小时" value="2h" />
          </el-select>
        </el-form-item>
        <el-form-item label="额定纯水量">
          <el-input-number
            v-model="queryForm.ratedPureLiters"
            :min="100"
            :step="500"
            :precision="0"
            controls-position="right"
          />
          <span class="unit-text">L</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="predictLoading" @click="handlePredict">开始预测</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty v-if="!prediction && !predictLoading" description="请选择设备并开始预测" />

    <template v-if="prediction">
      <div class="prediction-source-bar">
        <el-tag
          :type="predictionSourceTagType(prediction.predictionSource)"
          effect="dark"
          round
        >
          {{ predictionSourceLabel(prediction.predictionSource) }}
        </el-tag>
        <span v-if="prediction.predictionFallbackReason" class="fallback-reason">
          {{ prediction.predictionFallbackReason }}
        </span>
      </div>

      <el-row :gutter="16" class="summary-row">
        <el-col :span="6">
          <el-card shadow="hover" :class="{ 'qwen-card': isQwenPrediction }">
            <div class="stat-item">
              <div class="stat-label">
                健康评分
                <el-tag size="small" :type="isQwenPrediction ? '' : 'warning'" effect="plain" class="source-tag">
                  {{ isQwenPrediction ? '千问' : '本地规则' }}
                </el-tag>
              </div>
              <div class="score-row">
                <el-progress
                  type="dashboard"
                  :percentage="scorePercentage"
                  :color="scoreColor"
                  :width="90"
                />
                <div>
                  <el-tag :type="riskTagType(prediction.riskLevel)" size="large">
                    {{ riskLabel(prediction.riskLevel) }}
                  </el-tag>
                  <div class="sub-text">数据状态：{{ dataStatusLabel(prediction.dataStatus) }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" :class="{ 'qwen-card': isQwenPrediction }">
            <div class="stat-item">
              <div class="stat-label">
                预计剩余寿命
                <el-tag size="small" :type="isQwenPrediction ? '' : 'warning'" effect="plain" class="source-tag">
                  {{ isQwenPrediction ? '千问' : '本地规则' }}
                </el-tag>
              </div>
              <div class="stat-value">{{ formatNumber(prediction.estimatedRemainingDays, 1) }} 天</div>
              <div class="sub-text">剩余 {{ formatNumber(prediction.estimatedRemainingLiters, 0) }} L</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-label">当前用水强度</div>
              <div class="stat-value">{{ formatNumber(prediction.dailyPureLiters, 2) }} L/天</div>
              <div class="sub-text">统计点 {{ prediction.dataPointCount || 0 }} 个</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-item">
              <div class="stat-label">膜压评估</div>
              <div class="stat-value">{{ formatNumber(prediction.membranePressureDiff, 2) }} bar</div>
              <div class="sub-text">{{ pressureAssessmentLabel(prediction) }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="summary-row">
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>设备信息</template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="设备ID">{{ prediction.deviceId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="SN">{{ prediction.sn || '-' }}</el-descriptions-item>
              <el-descriptions-item label="型号">{{ prediction.modelName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="在线状态">
                <el-tag :type="prediction.online ? 'success' : 'info'">
                  {{ prediction.online ? '在线' : '离线' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="激活时间">{{ formatDateTime(prediction.activatedAt) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>关键指标</template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="脱盐率">{{ formatNumber(prediction.desalinationRate, 2) }}%</el-descriptions-item>
              <el-descriptions-item label="废水比">{{ formatNumber(prediction.wastewaterRatio, 2) }}</el-descriptions-item>
              <el-descriptions-item label="制水状态">
                {{ prediction.waterProducing ? '正在制水' : '未制水/待机' }}
              </el-descriptions-item>
              <el-descriptions-item label="统计范围">{{ prediction.rangeDays }} 天</el-descriptions-item>
              <el-descriptions-item label="统计间隔">{{ aggregateLabel(prediction.aggregateEvery) }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never">
            <template #header>模型状态</template>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="预测来源">
                <el-tag :type="predictionSourceTagType(prediction.predictionSource)" effect="dark">
                  {{ predictionSourceLabel(prediction.predictionSource) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="prediction.predictionFallbackReason" label="兜底原因">
                <span class="fallback-text">{{ prediction.predictionFallbackReason }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="千问状态">
                <el-tag :type="qwenStatusTag(prediction.qwenAdvice?.status)">
                  {{ qwenStatusLabel(prediction.qwenAdvice?.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="模型">{{ prediction.qwenAdvice?.model || '-' }}</el-descriptions-item>
              <el-descriptions-item label="维护优先级">
                {{ prediction.qwenAdvice?.maintenancePriority || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="AI 置信度">
                {{ confidenceLabel(prediction.qwenAdvice?.confidence) || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="summary-row advice-row">
        <el-col :span="12">
          <el-card shadow="never" class="advice-card">
            <template #header>规则评估建议</template>
            <p class="advice-summary">{{ prediction.ruleAdvice?.summary || '-' }}</p>
            <div class="section-title">判断依据</div>
            <el-empty v-if="!prediction.ruleAdvice?.reasons?.length" description="暂无依据" :image-size="70" />
            <ul v-else class="advice-list">
              <li v-for="item in prediction.ruleAdvice.reasons" :key="item">{{ item }}</li>
            </ul>
            <div class="section-title">建议动作</div>
            <el-empty v-if="!prediction.ruleAdvice?.recommendedActions?.length" description="暂无建议" :image-size="70" />
            <ul v-else class="advice-list">
              <li v-for="item in prediction.ruleAdvice.recommendedActions" :key="item">{{ item }}</li>
            </ul>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="advice-card">
            <template #header>千问 AI 建议</template>
            <el-alert
              v-if="prediction.qwenAdvice?.errorMessage"
              type="warning"
              :closable="false"
              :title="prediction.qwenAdvice.errorMessage"
              class="model-alert"
            />
            <template v-if="prediction.qwenAdvice?.status === 'OK'">
              <div class="section-title">千问预测值</div>
              <el-descriptions :column="2" border size="small" class="qwen-pred-stats">
                <el-descriptions-item label="健康评分">
                  <span class="qwen-val">{{ formatNumber(prediction.qwenAdvice.healthScore, 1) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="风险等级">
                  <el-tag :type="riskTagType(prediction.qwenAdvice.riskLevel)" size="small">
                    {{ riskLabel(prediction.qwenAdvice.riskLevel) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="剩余制水">
                  <span class="qwen-val">{{ formatNumber(prediction.qwenAdvice.estimatedRemainingLiters, 0) }} L</span>
                </el-descriptions-item>
                <el-descriptions-item label="剩余天数">
                  <span class="qwen-val">{{ formatNumber(prediction.qwenAdvice.estimatedRemainingDays, 1) }} 天</span>
                </el-descriptions-item>
              </el-descriptions>
            </template>
            <p class="advice-summary">{{ prediction.qwenAdvice?.summary || '模型暂未返回摘要' }}</p>
            <div class="section-title">维修建议</div>
            <el-empty v-if="!prediction.qwenAdvice?.recommendedActions?.length" description="暂无建议" :image-size="70" />
            <ul v-else class="advice-list">
              <li v-for="item in prediction.qwenAdvice.recommendedActions" :key="item">{{ item }}</li>
            </ul>
            <div class="section-title">分析说明</div>
            <p class="reasoning-text">{{ prediction.qwenAdvice?.reasoning || '-' }}</p>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never">
        <template #header>历史指标明细</template>
        <el-table :data="metricRows" border stripe>
          <el-table-column prop="name" label="点位" min-width="130">
            <template #default="{ row }">{{ row.field }} - {{ row.name }}</template>
          </el-table-column>
          <el-table-column prop="last" label="最新值" min-width="100">
            <template #default="{ row }">{{ formatMetric(row.last, row.unit) }}</template>
          </el-table-column>
          <el-table-column prop="avg" label="平均值" min-width="100">
            <template #default="{ row }">{{ formatMetric(row.avg, row.unit) }}</template>
          </el-table-column>
          <el-table-column prop="min" label="最小值" min-width="100">
            <template #default="{ row }">{{ formatMetric(row.min, row.unit) }}</template>
          </el-table-column>
          <el-table-column prop="max" label="最大值" min-width="100">
            <template #default="{ row }">{{ formatMetric(row.max, row.unit) }}</template>
          </el-table-column>
          <el-table-column prop="delta" label="变化量" min-width="100">
            <template #default="{ row }">{{ formatMetric(row.delta, row.unit) }}</template>
          </el-table-column>
          <el-table-column prop="count" label="样本数" min-width="90" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { pageDevices } from '@/api/device'
import type { DeviceVO } from '@/types/api'
import { predictRoMembrane } from '@/api/ai'
import type { AiMetricVO, RoMembranePredictionVO } from '@/api/ai'
import { formatDateTime } from '@/utils/format'

const devices = ref<DeviceVO[]>([])
const prediction = ref<RoMembranePredictionVO | null>(null)
const deviceLoading = ref(false)
const predictLoading = ref(false)

const queryForm = reactive({
  sn: '',
  rangeDays: 7,
  aggregateEvery: '1h',
  ratedPureLiters: 12000
})

const metricOrder = ['P1', 'P2', 'P7', 'P12', 'P15', 'P18', 'P19']

const scorePercentage = computed(() => {
  const score = Number(prediction.value?.healthScore ?? 0)
  return Math.max(0, Math.min(100, Math.round(score)))
})

const isQwenPrediction = computed(() => {
  return prediction.value?.predictionSource === 'QWEN_PRIMARY'
})

const metricRows = computed<AiMetricVO[]>(() => {
  const metrics = prediction.value?.metrics || {}
  const rows = metricOrder.map((key) => metrics[key]).filter(Boolean)
  const extraRows = Object.keys(metrics)
    .filter((key) => !metricOrder.includes(key))
    .map((key) => metrics[key])
  return [...rows, ...extraRows]
})

function scoreColor(percentage: number) {
  if (percentage >= 85) return '#67C23A'
  if (percentage >= 65) return '#E6A23C'
  return '#F56C6C'
}

async function loadDevices() {
  deviceLoading.value = true
  try {
    const res = await pageDevices({ pageNum: 1, pageSize: 500 })
    devices.value = res.data?.records || []
    if (!queryForm.sn && devices.value.length > 0) {
      queryForm.sn = devices.value[0].sn
    }
  } catch (e) {
    ElMessage.error('加载设备列表失败')
  } finally {
    deviceLoading.value = false
  }
}

async function handlePredict() {
  if (!queryForm.sn) {
    ElMessage.warning('请先选择设备')
    return
  }
  predictLoading.value = true
  try {
    const res = await predictRoMembrane({
      sn: queryForm.sn,
      rangeDays: queryForm.rangeDays,
      aggregateEvery: queryForm.aggregateEvery,
      ratedPureLiters: queryForm.ratedPureLiters
    })
    prediction.value = res.data
  } catch (e) {
    prediction.value = null
    ElMessage.error('AI预测失败，请检查AI服务和设备历史数据')
  } finally {
    predictLoading.value = false
  }
}

function deviceLabel(device: DeviceVO) {
  const parts = [device.deviceId, device.sn, device.modelName].filter(Boolean)
  return parts.join(' / ')
}

function formatNumber(value: number | null | undefined, precision = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return Number(value).toFixed(precision)
}

function formatMetric(value: number | null | undefined, unit: string) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return '-'
  return `${Number(value).toFixed(2)} ${unit || ''}`.trim()
}

function riskLabel(value?: string) {
  const map: Record<string, string> = {
    LOW: '低风险',
    MEDIUM: '中风险',
    HIGH: '高风险',
    CRITICAL: '严重风险',
    UNKNOWN: '未知'
  }
  return map[value || ''] || value || '-'
}

function riskTagType(value?: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    LOW: 'success',
    MEDIUM: 'warning',
    HIGH: 'danger',
    CRITICAL: 'danger',
    UNKNOWN: 'info'
  }
  return map[value || ''] || 'info'
}

function dataStatusLabel(value?: string) {
  const map: Record<string, string> = {
    OK: '正常',
    NO_DATA: '暂无数据',
    INFLUX_ERROR: '历史库异常',
    DEVICE_NOT_FOUND: '设备不存在',
    INVALID_PARAMS: '参数异常'
  }
  return map[value || ''] || value || '-'
}

function qwenStatusLabel(value?: string) {
  const map: Record<string, string> = {
    OK: '已生成',
    SKIPPED: '未启用',
    MODEL_ERROR: '模型异常',
    CONFIG_ERROR: '配置异常',
    TIMEOUT: '调用超时'
  }
  return map[value || ''] || value || '-'
}

function qwenStatusTag(value?: string) {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    OK: 'success',
    SKIPPED: 'info',
    MODEL_ERROR: 'danger',
    CONFIG_ERROR: 'warning',
    TIMEOUT: 'warning'
  }
  return map[value || ''] || 'info'
}

function confidenceLabel(value?: string) {
  const map: Record<string, string> = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低'
  }
  return map[value || ''] || value || '-'
}

function aggregateLabel(value?: string) {
  const map: Record<string, string> = {
    '10m': '10分钟',
    '30m': '30分钟',
    '1h': '1小时',
    '2h': '2小时'
  }
  return map[value || ''] || value || '-'
}

function pressureAssessmentLabel(row: RoMembranePredictionVO) {
  if (row.pressureAssessmentMessage) return row.pressureAssessmentMessage
  const map: Record<string, string> = {
    IDLE_IGNORED: '未制水，压差仅展示不参与异常判断',
    PRODUCING_EVALUATED: '制水中，压差参与异常判断',
    PRODUCING_PRESSURE_MISSING: '检测到制水，但制水窗口压力不足，压差仅展示',
    UNKNOWN: '制水状态未知'
  }
  return map[row.pressureAssessment || ''] || '-'
}

function predictionSourceLabel(source?: string) {
  const map: Record<string, string> = {
    QWEN_PRIMARY: '千问 AI 预测',
    LOCAL_RULE: '本地规则预测',
    UNKNOWN: '未知来源'
  }
  return map[source || ''] || source || '未知来源'
}

function predictionSourceTagType(source?: string) {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    QWEN_PRIMARY: '',
    LOCAL_RULE: 'warning',
    UNKNOWN: 'info'
  }
  return map[source || ''] || 'info'
}

onMounted(async () => {
  await loadDevices()
})
</script>

<style scoped lang="scss">
.ro-page {
  padding: 16px;
}

.search-card,
.summary-row {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.unit-text {
  margin-left: 8px;
  color: #606266;
}

.stat-item {
  min-height: 100px;
}

.stat-label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
}

.stat-value {
  color: #303133;
  font-size: 26px;
  font-weight: 600;
  line-height: 34px;
}

.sub-text {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.advice-row {
  align-items: stretch;
}

.advice-card {
  height: 100%;
}

.advice-summary,
.reasoning-text {
  margin: 0 0 12px;
  color: #303133;
  line-height: 1.7;
}

.section-title {
  margin: 14px 0 8px;
  color: #606266;
  font-weight: 600;
}

.advice-list {
  margin: 0;
  padding-left: 20px;
  color: #303133;
  line-height: 1.8;
}

.model-alert {
  margin-bottom: 12px;
}

.prediction-source-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: #f0f2f5;
  border-radius: 6px;
  border-left: 4px solid #409EFF;
}

.fallback-reason {
  color: #909399;
  font-size: 13px;
}

.qwen-card {
  border: 1px solid #a0cfff;
  background: linear-gradient(135deg, #f0f7ff 0%, #fff 100%);
}

.source-tag {
  margin-left: 6px;
  vertical-align: middle;
}

.qwen-pred-stats {
  margin-bottom: 14px;
}

.qwen-val {
  color: #409EFF;
  font-weight: 600;
}

.fallback-text {
  color: #E6A23C;
  font-size: 13px;
}
</style>
