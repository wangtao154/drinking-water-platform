<template>
  <div class="water-quality-page">
    <!-- 统计概览 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">设备总数</div>
            <div class="stat-value">{{ dashboard.deviceTotal ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">在线设备</div>
            <div class="stat-value">{{ dashboard.deviceOnline ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">离线设备</div>
            <div class="stat-value">{{ dashboard.deviceOffline ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">告警设备</div>
            <div class="stat-value">{{ dashboard.deviceAlert ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- TDS 趋势折线图 -->
    <el-card class="chart-card">
      <template #header>
        <span>水质 TDS 趋势</span>
      </template>
      <div ref="tdsChartRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDashboard } from '@/api/report'
import type { DashboardVO } from '@/api/report'

const dashboard = ref<DashboardVO>({} as DashboardVO)
const tdsChartRef = ref<HTMLElement | null>(null)
let tdsChart: echarts.ECharts | null = null

async function loadDashboard() {
  try {
    const res = await getDashboard()
    dashboard.value = res.data
  } catch (e) {
    ElMessage.error('加载概览失败')
  }
}

function initTdsChart() {
  if (!tdsChartRef.value) return
  tdsChart = echarts.init(tdsChartRef.value)

  // 模拟数据
  const dates = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  const tdsValues = [45, 52, 48, 60, 55, 42, 38, 50, 47, 53, 49, 44]

  tdsChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: 'TDS (ppm)' },
    series: [{
      name: 'TDS',
      type: 'line',
      data: tdsValues,
      smooth: true,
      areaStyle: { opacity: 0.3 },
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  })
}

function handleResize() {
  tdsChart?.resize()
}

onMounted(() => {
  loadDashboard()
  initTdsChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  tdsChart?.dispose()
})
</script>

<style scoped lang="scss">
.water-quality-page {
  padding: 16px;
}
.overview-row {
  margin-bottom: 16px;
}
.stat-item {
  text-align: center;
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }
  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }
}
.chart-card {
  margin-bottom: 16px;
}
.chart-container {
  width: 100%;
  height: 400px;
}
</style>
