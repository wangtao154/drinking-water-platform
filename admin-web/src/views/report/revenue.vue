<template>
  <div class="revenue-page">
    <!-- 数字概览 -->
    <el-row :gutter="16" class="overview-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">总营收</div>
            <div class="stat-value">{{ fenToYuan(report.totalRevenue) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">分佣总额</div>
            <div class="stat-value">{{ fenToYuan(report.totalCommission) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">已结算</div>
            <div class="stat-value">{{ fenToYuan(report.settledCommission) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">待结算</div>
            <div class="stat-value">{{ fenToYuan(report.pendingCommission) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 柱状图 -->
    <el-card class="chart-card">
      <template #header>
        <span>营收 / 分佣 / 已结算 对比</span>
      </template>
      <div ref="barChartRef" class="chart-container"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getFinanceReport } from '@/api/report'
import type { ReportFinanceVO } from '@/api/report'
import { fenToYuan } from '@/utils/format'

const report = ref<ReportFinanceVO>({} as ReportFinanceVO)
const barChartRef = ref<HTMLElement | null>(null)
let barChart: echarts.ECharts | null = null

async function loadReport() {
  try {
    const res = await getFinanceReport()
    report.value = res.data
  } catch (e) {
    ElMessage.error('加载报表失败')
  }
}

function initBarChart() {
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)

  // 模拟月度数据
  const months = ['1月', '2月', '3月', '4月', '5月', '6月']
  const revenueData = [12000, 15000, 18000, 14000, 16000, 20000]
  const commissionData = [1200, 1500, 1800, 1400, 1600, 2000]
  const settledData = [10000, 13000, 16000, 12000, 14000, 18000]

  barChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['营收', '分佣', '已结算'] },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [
      { name: '营收', type: 'bar', data: revenueData },
      { name: '分佣', type: 'bar', data: commissionData },
      { name: '已结算', type: 'bar', data: settledData },
    ],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  })
}

function handleResize() {
  barChart?.resize()
}

onMounted(() => {
  loadReport()
  initBarChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
})
</script>

<style scoped lang="scss">
.revenue-page {
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
