<template>
  <div class="work-order-page">
    <!-- 饼图：工单状态分布 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <span>工单状态分布</span>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <!-- 柱状图：工人工单量 TOP -->
        <el-card class="chart-card">
          <template #header>
            <span>工人工单量 TOP</span>
          </template>
          <div ref="barChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getWorkerReport } from '@/api/report'
import type { ReportWorkerVO } from '@/api/report'

const pieChartRef = ref<HTMLElement | null>(null)
const barChartRef = ref<HTMLElement | null>(null)
let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null

async function loadReport() {
  try {
    await getWorkerReport()
  } catch (e) {
    ElMessage.error('加载报表失败')
  }
}

function initPieChart() {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)

  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      name: '工单状态',
      type: 'pie',
      radius: '60%',
      data: [
        { value: 35, name: '待处理' },
        { value: 20, name: '进行中' },
        { value: 40, name: '已完成' },
        { value: 5, name: '已取消' },
      ],
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } },
    }],
  })
}

function initBarChart() {
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)

  const workers = ['张工', '李工', '王工', '赵工', '刘工', '陈工', '杨工', '周工']
  const counts = [42, 38, 35, 30, 28, 25, 22, 18]

  barChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: workers },
    yAxis: { type: 'value', name: '工单数' },
    series: [{
      name: '工单量',
      type: 'bar',
      data: counts,
      itemStyle: { color: '#409EFF' },
    }],
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  })
}

function handleResize() {
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  loadReport()
  initPieChart()
  initBarChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped lang="scss">
.work-order-page {
  padding: 16px;
}
.chart-card {
  margin-bottom: 16px;
}
.chart-container {
  width: 100%;
  height: 400px;
}
</style>
