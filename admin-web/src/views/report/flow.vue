<template>
  <div class="flow-report-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">总设备数</div><div class="stat-value">{{ report.totalDevices }}</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">在线设备</div><div class="stat-value" style="color: #67c23a">{{ report.onlineDevices }}</div><div class="stat-sub">在线率 {{ report.onlineRate }}%</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">总用水量</div><div class="stat-value">{{ (report.totalWaterFlow / 1000).toFixed(1) }} m³</div></div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">今日用水量</div><div class="stat-value">{{ (report.todayWaterFlow / 1000).toFixed(1) }} m³</div></div></el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16">
      <el-col :span="14">
        <el-card>
          <template #header>近7天用水量趋势</template>
          <div ref="trendChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card>
          <template #header>设备型号分布</template>
          <div ref="modelChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 补充统计 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">离线设备数</div><div class="stat-value" style="color: #f56c6c">{{ report.offlineDevices }}</div></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">今日上报设备</div><div class="stat-value">{{ report.todayReportDevices }}</div></div></el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover"><div class="stat-card"><div class="stat-label">总净水量</div><div class="stat-value">{{ (report.totalPureFlow / 1000).toFixed(1) }} m³</div></div></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getFlowReport } from '@/api/report'
import type { FlowReportVO } from '@/types/api'

const report = reactive<FlowReportVO>({
  totalDevices: 0, onlineDevices: 0, offlineDevices: 0, todayReportDevices: 0,
  totalWaterFlow: 0, todayWaterFlow: 0, totalPureFlow: 0, todayPureFlow: 0,
  dailyFlowTrend: [], modelDistribution: [], onlineRate: 0,
})
const trendChartRef = ref<HTMLElement>()
const modelChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let modelChart: echarts.ECharts | null = null

async function loadData() {
  try {
    const res = await getFlowReport()
    Object.assign(report, res.data)
    await nextTick()
    renderCharts()
  } catch (e) {
    ElMessage.error('加载流量报表失败')
  }
}

function renderCharts() {
  // 趋势图
  if (trendChartRef.value) {
    if (trendChart) trendChart.dispose()
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: report.dailyFlowTrend.map(d => d.date) },
      yAxis: { type: 'value', name: '用水量(L)' },
      series: [{
        name: '用水量',
        type: 'bar',
        data: report.dailyFlowTrend.map(d => d.value),
        itemStyle: { color: '#409eff' },
        barWidth: '40%',
      }],
    })
  }
  // 型号分布图
  if (modelChartRef.value) {
    if (modelChart) modelChart.dispose()
    modelChart = echarts.init(modelChartRef.value)
    modelChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: report.modelDistribution.length > 0 ? report.modelDistribution : [{ name: '暂无数据', value: 1 }],
        label: { formatter: '{b}: {c}' },
      }],
    })
  }
}

function handleResize() { trendChart?.resize(); modelChart?.resize() }

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  modelChart?.dispose()
})
</script>

<style scoped lang="scss">
.flow-report-page { padding: 16px; }
.stat-row { margin-bottom: 12px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 6px; }
.stat-value { font-size: 24px; font-weight: bold; color: #409eff; }
.stat-sub { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
