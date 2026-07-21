<template>
  <div class="app-container">
    <!-- 统计卡片 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="6" v-for="card in statCards" :key="card.title">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-title">{{ card.title }}</div>
          <div class="stat-value" :style="{ color: card.color }">
            {{ card.value }}
          </div>
          <div class="stat-sub">{{ card.sub }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>设备状态分布</template>
          <div ref="pieChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>订单状态分布</template>
          <div ref="barChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>营收概览</template>
          <div ref="revenueChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>工单统计</template>
          <div ref="workOrderChartRef" style="height: 320px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboard, getDeviceReport, getOrderReport, getFinanceReport, getWorkerReport } from '@/api/report'
import { fenToYuan } from '@/utils/format'
import type { DashboardVO, ReportDeviceVO, ReportOrderVO, ReportFinanceVO, ReportWorkerVO } from '@/types/api'

const loading = ref(false)
const pieChartRef = ref<HTMLElement>()
const barChartRef = ref<HTMLElement>()
const revenueChartRef = ref<HTMLElement>()
const workOrderChartRef = ref<HTMLElement>()

const statCards = ref([
  { title: '设备总数', value: 0, color: '#409eff', sub: '全部设备' },
  { title: '在线设备', value: 0, color: '#67c23a', sub: '当前在线' },
  { title: '总营收', value: '0.00', color: '#e6a23c', sub: '已支付订单金额（元）' },
  { title: '待处理告警', value: 0, color: '#f56c6c', sub: '未处理告警数' }
])

let pieChart: echarts.ECharts | null = null
let barChart: echarts.ECharts | null = null
let revenueChart: echarts.ECharts | null = null
let workOrderChart: echarts.ECharts | null = null

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const [dashboard, device, order, finance, worker] = await Promise.all([
      getDashboard(),
      getDeviceReport(),
      getOrderReport(),
      getFinanceReport(),
      getWorkerReport()
    ])

    // 更新统计卡片
    const d = dashboard.data
    statCards.value[0].value = d.totalDevices
    statCards.value[1].value = d.onlineDevices
    statCards.value[2].value = fenToYuan(d.totalRevenue)
    statCards.value[3].value = d.activeAlerts

    await nextTick()

    // 渲染图表
    renderPieChart(device.data)
    renderBarChart(order.data)
    renderRevenueChart(finance.data)
    renderWorkOrderChart(worker.data)
  } catch (error) {
    console.error('Dashboard load error', error)
  } finally {
    loading.value = false
  }
}

function renderPieChart(data: ReportDeviceVO) {
  if (!pieChartRef.value) return
  pieChart = echarts.init(pieChartRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      label: { formatter: '{b}: {c}' },
      data: [
        { value: data.onlineCount, name: '在线', itemStyle: { color: '#67c23a' } },
        { value: data.offlineCount, name: '离线', itemStyle: { color: '#909399' } },
        { value: data.faultCount, name: '故障', itemStyle: { color: '#f56c6c' } },
        { value: data.activatedToday, name: '今日激活', itemStyle: { color: '#409eff' } },
        { value: data.registeredToday, name: '今日登记', itemStyle: { color: '#e6a23c' } }
      ].filter(d => d.value > 0)
    }]
  })
}

function renderBarChart(data: ReportOrderVO) {
  if (!barChartRef.value) return
  barChart = echarts.init(barChartRef.value)
  barChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['待支付', '已支付', '已取消', '已退款'] },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: [data.pendingOrders, data.paidOrders, data.cancelledOrders, data.refundedOrders],
      itemStyle: {
        color: (params: any) => ['#e6a23c', '#67c23a', '#909399', '#f56c6c'][params.dataIndex]
      }
    }]
  })
}

function renderRevenueChart(data: ReportFinanceVO) {
  if (!revenueChartRef.value) return
  revenueChart = echarts.init(revenueChartRef.value)
  revenueChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['总营收', '佣金总额', '已结算佣金', '待结算佣金', '消费总额'] },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [{
      type: 'bar',
      data: [
        Number(fenToYuan(data.totalRevenue)),
        Number(fenToYuan(data.totalCommission)),
        Number(fenToYuan(data.settledCommission)),
        Number(fenToYuan(data.pendingCommission)),
        Number(fenToYuan(data.totalConsumption))
      ],
      itemStyle: { color: '#409eff' }
    }]
  })
}

function renderWorkOrderChart(data: ReportWorkerVO) {
  if (!workOrderChartRef.value) return
  workOrderChart = echarts.init(workOrderChartRef.value)
  workOrderChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '60%',
      label: { formatter: '{b}: {c}' },
      data: [
        { value: data.totalWorkOrders, name: '总工单数' },
        { value: data.completedWorkOrders, name: '已完成' },
        { value: data.pendingWorkOrders, name: '待处理' },
        { value: data.totalWorkers, name: '工人总数' },
        { value: data.activeWorkers, name: '活跃工人' }
      ]
    }]
  })
}

// 监听窗口大小
window.addEventListener('resize', () => {
  pieChart?.resize()
  barChart?.resize()
  revenueChart?.resize()
  workOrderChart?.resize()
})
</script>

<style scoped lang="scss">
.stat-card {
  .stat-title { font-size: 13px; color: #909399; margin-bottom: 8px; }
  .stat-value { font-size: 28px; font-weight: 700; }
  .stat-sub { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
}
</style>
