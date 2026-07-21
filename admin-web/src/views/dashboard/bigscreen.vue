<template>
  <div class="bigscreen">
    <!-- Header -->
    <div class="bs-header">
      <div class="bs-header-left">
        <div class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </div>
      </div>
      <div class="bs-header-center">
        <span class="bs-title">直饮水平台数据可视化大屏</span>
      </div>
      <div class="bs-header-right">
        <span class="live-dot" :class="{ active: !firstLoading }"></span>
        <span class="live-text">{{ firstLoading ? '加载中' : '实时' }}</span>
        <span class="bs-time">{{ currentTime }}</span>
      </div>
    </div>

    <!-- Body -->
    <div class="bs-body" v-loading="firstLoading">
      <!-- KPI Row -->
      <div class="kpi-row">
        <div class="kpi-card" v-for="(kpi, i) in kpiCards" :key="i" :style="{ '--accent': kpi.color }">
          <div class="kpi-icon"><el-icon><component :is="kpi.icon" /></el-icon></div>
          <div class="kpi-info">
            <div class="kpi-value">
              <span class="kpi-num">{{ kpi.value }}</span>
              <span class="kpi-unit">{{ kpi.unit }}</span>
            </div>
            <div class="kpi-label">{{ kpi.label }}</div>
          </div>
          <div class="kpi-deco"></div>
        </div>
      </div>

      <!-- Charts Row 1 -->
      <div class="chart-row top-row">
        <div class="chart-panel">
          <div class="panel-title"><span class="dot dot-cyan"></span>设备状态分布</div>
          <div ref="deviceStatusRef" class="chart-box"></div>
        </div>
        <div class="chart-panel">
          <div class="panel-title"><span class="dot dot-green"></span>近7日制水趋势</div>
          <div ref="flowTrendRef" class="chart-box"></div>
        </div>
        <div class="chart-panel">
          <div class="panel-title"><span class="dot dot-purple"></span>设备型号分布</div>
          <div ref="modelDistRef" class="chart-box"></div>
        </div>
      </div>

      <!-- Charts Row 2: Map Center -->
      <div class="chart-row map-row">
        <div class="chart-panel side-panel">
          <div class="panel-title"><span class="dot dot-yellow"></span>订单状态分布</div>
          <div ref="orderStatusRef" class="chart-box"></div>
        </div>
        <div class="chart-panel map-panel">
          <div class="panel-title"><span class="dot dot-orange"></span>区域设备分布 — 贵州省</div>
          <div ref="regionDistRef" class="chart-box"></div>
        </div>
        <div class="chart-panel side-panel">
          <div class="panel-title"><span class="dot dot-red"></span>工单概览</div>
          <div ref="workOrderRef" class="chart-box"></div>
        </div>
      </div>

      <!-- Bottom Row -->
      <div class="chart-row bottom-row">
        <div class="chart-panel">
          <div class="panel-title"><span class="dot dot-cyan"></span>营收概览</div>
          <div ref="revenueRef" class="chart-box"></div>
        </div>
        <div class="chart-panel">
          <div class="panel-title"><span class="dot dot-green"></span>实时告警信息</div>
          <div class="alert-list">
            <div class="alert-item" v-for="(alert, i) in alertList" :key="i" :class="alert.level">
              <span class="alert-time">{{ alert.time }}</span>
              <span class="alert-tag">{{ alert.tag }}</span>
              <span class="alert-msg">{{ alert.msg }}</span>
            </div>
          </div>
        </div>
        <div class="chart-panel">
          <div class="panel-title"><span class="dot dot-purple"></span>近期工单</div>
          <div class="order-list">
            <div class="order-item" v-for="(order, i) in recentOrders" :key="i">
              <span class="order-no">{{ order.no }}</span>
              <span class="order-type" :class="order.typeClass">{{ order.type }}</span>
              <span class="order-status" :class="order.statusClass">{{ order.status }}</span>
              <span class="order-time">{{ order.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getDashboard, getDeviceReport, getOrderReport, getFinanceReport, getWorkerReport, getFlowReport } from '@/api/report'
import { fenToYuan } from '@/utils/format'
import guizhouGeo from '@/assets/guizhou.json'

const router = useRouter()

// Only show full-page loading on very first load
const firstLoading = ref(true)
let chartsReady = false

// Chart refs
const deviceStatusRef = ref<HTMLElement>()
const flowTrendRef = ref<HTMLElement>()
const modelDistRef = ref<HTMLElement>()
const regionDistRef = ref<HTMLElement>()
const orderStatusRef = ref<HTMLElement>()
const workOrderRef = ref<HTMLElement>()
const revenueRef = ref<HTMLElement>()

// Persistent chart instances (created once, updated in-place)
let chartDevice: echarts.ECharts | null = null
let chartFlow: echarts.ECharts | null = null
let chartModel: echarts.ECharts | null = null
let chartRegion: echarts.ECharts | null = null
let chartOrder: echarts.ECharts | null = null
let chartWorker: echarts.ECharts | null = null
let chartRevenue: echarts.ECharts | null = null

// Current time
const currentTime = ref('')
let timeTimer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null
const REFRESH_INTERVAL = 15000 // 15s silent refresh

// KPI cards
const kpiCards = ref([
  { label: '设备总数', value: 0, unit: '台', icon: 'Box', color: '#00d4ff' },
  { label: '在线设备', value: 0, unit: '台', icon: 'Monitor', color: '#52c41a' },
  { label: '离线设备', value: 0, unit: '台', icon: 'Warning', color: '#fa541c' },
  { label: '今日制水量', value: '0.0', unit: '吨', icon: 'DataLine', color: '#36cfc9' },
  { label: '今日制水金额', value: '0.00', unit: '元', icon: 'Money', color: '#fadb14' },
  { label: '总客户数', value: 0, unit: '人', icon: 'User', color: '#722ed1' },
])

// Alert list (mock)
const alertList = ref([
  { time: '14:32:05', tag: '滤芯', msg: '设备J100234 滤芯寿命不足10%', level: 'warning' },
  { time: '14:28:11', tag: '离线', msg: '设备J100189 已离线超过30分钟', level: 'danger' },
  { time: '14:15:33', tag: '水质', msg: '设备J100312 TDS值超标(>50)', level: 'warning' },
  { time: '14:05:07', tag: '故障', msg: '设备J100078 泵压异常', level: 'danger' },
  { time: '13:58:22', tag: '滤芯', msg: '设备J100456 滤芯寿命不足15%', level: 'warning' },
  { time: '13:42:18', tag: '离线', msg: '设备J100201 已离线超过1小时', level: 'danger' },
  { time: '13:30:05', tag: '水质', msg: '设备J100567 余氯值偏高', level: 'warning' },
])

// Recent orders (mock)
const recentOrders = ref([
  { no: 'WO20260714-0032', type: '滤芯更换', typeClass: 't-cyan', status: '已完成', statusClass: 's-green', time: '14:30' },
  { no: 'WO20260714-0031', type: '设备故障', typeClass: 't-red', status: '进行中', statusClass: 's-yellow', time: '14:15' },
  { no: 'WO20260714-0030', type: '安装调试', typeClass: 't-blue', status: '已接单', statusClass: 's-cyan', time: '13:50' },
  { no: 'WO20260714-0029', type: '滤芯更换', typeClass: 't-cyan', status: '已完成', statusClass: 's-green', time: '13:20' },
  { no: 'WO20260714-0028', type: '设备故障', typeClass: 't-red', status: '待处理', statusClass: 's-orange', time: '12:45' },
  { no: 'WO20260714-0027', type: '定期保养', typeClass: 't-purple', status: '已完成', statusClass: 's-green', time: '11:30' },
  { no: 'WO20260714-0026', type: '安装调试', typeClass: 't-blue', status: '已完成', statusClass: 's-green', time: '10:15' },
  { no: 'WO20260714-0025', type: '滤芯更换', typeClass: 't-cyan', status: '已完成', statusClass: 's-green', time: '09:40' },
])

onMounted(() => {
  updateTime()
  timeTimer = setInterval(updateTime, 1000)
  loadData()
  refreshTimer = setInterval(loadData, REFRESH_INTERVAL)
})

onUnmounted(() => {
  if (timeTimer) clearInterval(timeTimer)
  if (refreshTimer) clearInterval(refreshTimer)
  chartDevice?.dispose()
  chartFlow?.dispose()
  chartModel?.dispose()
  chartRegion?.dispose()
  chartOrder?.dispose()
  chartWorker?.dispose()
  chartRevenue?.dispose()
  window.removeEventListener('resize', handleResize)
})

function updateTime() {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const mi = String(now.getMinutes()).padStart(2, '0')
  const s = String(now.getSeconds()).padStart(2, '0')
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  currentTime.value = `${y}-${m}-${d} 星期${weekdays[now.getDay()]} ${h}:${mi}:${s}`
}

function goBack() {
  router.push('/dashboard')
}

function handleResize() {
  chartDevice?.resize()
  chartFlow?.resize()
  chartModel?.resize()
  chartRegion?.resize()
  chartOrder?.resize()
  chartWorker?.resize()
  chartRevenue?.resize()
}

/**
 * Main data loading entry.
 * - First call: shows loading overlay, inits all charts, then populates data
 * - Subsequent calls (auto-refresh): silent — no loading, just setOption on existing instances
 */
async function loadData() {
  try {
    const [dashboard, device, order, finance, worker, flow] = await Promise.all([
      getDashboard(),
      getDeviceReport(),
      getOrderReport(),
      getFinanceReport(),
      getWorkerReport(),
      getFlowReport(),
    ])

    const d = dashboard.data
    const dv = device.data
    const od = order.data
    const fin = finance.data
    const wk = worker.data
    const fl = flow.data

    // Update KPI cards
    kpiCards.value[0].value = d.totalDevices || dv.totalDevices
    kpiCards.value[1].value = d.onlineDevices || dv.onlineCount
    kpiCards.value[2].value = d.offlineDevices || dv.offlineCount
    const todayFlowTons = ((fl.todayWaterFlow || 0) / 1000).toFixed(1)
    kpiCards.value[3].value = todayFlowTons
    const todayRevenue = Number(fenToYuan(od.todayRevenue || 0))
    kpiCards.value[4].value = todayRevenue.toFixed(2)
    kpiCards.value[5].value = d.totalCustomers || 0

    await nextTick()

    if (!chartsReady) {
      // First load: init charts
      initCharts(dv, fl, od, fin, wk)
      chartsReady = true
      firstLoading.value = false
      window.addEventListener('resize', handleResize)
    } else {
      // Silent refresh: just update options on existing instances
      updateCharts(dv, fl, od, fin, wk)
    }
  } catch (error) {
    console.error('Bigscreen load error', error)
    if (!chartsReady) {
      // First load failed — show mock data so the screen isn't blank
      loadMockData()
      chartsReady = true
      firstLoading.value = false
      window.addEventListener('resize', handleResize)
    }
    // On refresh failure: keep showing the previous data (don't clear)
  }
}

/** Initialise all chart instances with full options */
function initCharts(dv: any, fl: any, od: any, fin: any, wk: any) {
  // Register Guizhou province map
  echarts.registerMap('guizhou', guizhouGeo as any)

  if (deviceStatusRef.value) chartDevice = echarts.init(deviceStatusRef.value)
  if (flowTrendRef.value) chartFlow = echarts.init(flowTrendRef.value)
  if (modelDistRef.value) chartModel = echarts.init(modelDistRef.value)
  if (regionDistRef.value) chartRegion = echarts.init(regionDistRef.value)
  if (orderStatusRef.value) chartOrder = echarts.init(orderStatusRef.value)
  if (workOrderRef.value) chartWorker = echarts.init(workOrderRef.value)
  if (revenueRef.value) chartRevenue = echarts.init(revenueRef.value)

  setDeviceStatusOption(dv)
  setFlowTrendOption(fl)
  setModelDistOption(fl)
  setRegionDistOption()
  setOrderStatusOption(od)
  setWorkOrderOption(wk)
  setRevenueOption(fin)
}

/** Update existing chart instances with new data (no re-init) */
function updateCharts(dv: any, fl: any, od: any, fin: any, wk: any) {
  setDeviceStatusOption(dv)
  setFlowTrendOption(fl)
  setModelDistOption(fl)
  setRegionDistOption()
  setOrderStatusOption(od)
  setWorkOrderOption(wk)
  setRevenueOption(fin)
}

function loadMockData() {
  kpiCards.value[0].value = 3199
  kpiCards.value[1].value = 2415
  kpiCards.value[2].value = 784
  kpiCards.value[3].value = '1364.8'
  kpiCards.value[4].value = '5423.20'
  kpiCards.value[5].value = 2876

  initCharts(
    { totalDevices: 3199, onlineCount: 2415, offlineCount: 784, faultCount: 23, registeredToday: 12, activatedToday: 8 },
    {
      dailyFlowTrend: Array.from({ length: 7 }, (_, i) => {
        const date = new Date()
        date.setDate(date.getDate() - (6 - i))
        return { date: `${date.getMonth() + 1}/${date.getDate()}`, value: Math.floor(800 + Math.random() * 800) }
      })
    } as any,
    { totalOrders: 15420, pendingOrders: 120, paidOrders: 14800, cancelledOrders: 350, refundedOrders: 150, totalRevenue: 0, todayOrders: 156, todayRevenue: 0 },
    { totalCommission: 0, settledCommission: 0, pendingCommission: 0, totalConsumption: 0, totalRevenue: 0, pendingSettlements: 0 },
    { totalWorkers: 85, activeWorkers: 62, totalWorkOrders: 3260, pendingWorkOrders: 18, completedWorkOrders: 3100, avgRating: 4.8 },
  )
}

// Common chart colors
const COLORS = {
  cyan: '#00d4ff',
  green: '#52c41a',
  orange: '#fa541c',
  purple: '#722ed1',
  yellow: '#fadb14',
  red: '#ff4d4f',
  blue: '#1890ff',
  teal: '#36cfc9',
}

const colorPalette = [COLORS.cyan, COLORS.green, COLORS.orange, COLORS.purple, COLORS.yellow, COLORS.red, COLORS.blue, COLORS.teal]

function getBaseOption() {
  return {
    backgroundColor: 'transparent',
    textStyle: { color: '#a0c4d8', fontFamily: 'Microsoft YaHei, sans-serif' },
    tooltip: {
      backgroundColor: 'rgba(10, 25, 41, 0.9)',
      borderColor: 'rgba(0, 212, 255, 0.3)',
      textStyle: { color: '#e0e0e0' },
    },
  }
}

// ── setOption helpers (call on existing chart instance) ──

function setDeviceStatusOption(data: any) {
  if (!chartDevice) return
  const total = data.totalDevices || 0
  chartDevice.setOption({
    ...getBaseOption(),
    tooltip: { ...getBaseOption().tooltip, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 5, textStyle: { color: '#7da6c4', fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#0a1929', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}', color: '#a0c4d8', fontSize: 11 },
      labelLine: { lineStyle: { color: '#3a5066' } },
      data: [
        { value: data.onlineCount || 0, name: '在线', itemStyle: { color: COLORS.green } },
        { value: data.offlineCount || 0, name: '离线', itemStyle: { color: COLORS.orange } },
        { value: data.faultCount || 0, name: '故障', itemStyle: { color: COLORS.red } },
        { value: data.activatedToday || 0, name: '今日激活', itemStyle: { color: COLORS.cyan } },
        { value: data.registeredToday || 0, name: '今日登记', itemStyle: { color: COLORS.yellow } },
      ].filter(d => d.value > 0),
    }],
    graphic: [
      { type: 'text', left: 'center', top: '35%', style: { text: String(total), fontSize: 36, fill: '#00d4ff', fontWeight: 'bold', textAlign: 'center' } },
      { type: 'text', left: 'center', top: '48%', style: { text: '设备总数', fontSize: 12, fill: '#7da6c4', textAlign: 'center' } },
    ],
  }, { notMerge: true })
}

function setFlowTrendOption(data: any) {
  if (!chartFlow) return
  const trend = data.dailyFlowTrend || []
  const dates = trend.map((t: any) => t.date)
  const values = trend.map((t: any) => Number((t.value / 1000).toFixed(1)))

  chartFlow.setOption({
    ...getBaseOption(),
    tooltip: { ...getBaseOption().tooltip, trigger: 'axis', formatter: (p: any) => `${p[0].name}<br/>制水量: ${p[0].value} 吨` },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: dates, boundaryGap: false, axisLine: { lineStyle: { color: '#3a5066' } }, axisLabel: { color: '#7da6c4', fontSize: 11 }, axisTick: { show: false } },
    yAxis: { type: 'value', name: '吨', nameTextStyle: { color: '#7da6c4' }, axisLine: { show: false }, axisLabel: { color: '#7da6c4', fontSize: 11 }, splitLine: { lineStyle: { color: 'rgba(58, 80, 102, 0.3)', type: 'dashed' } }, axisTick: { show: false } },
    series: [{
      type: 'line', data: values, smooth: true, symbol: 'circle', symbolSize: 6,
      lineStyle: { width: 2, color: COLORS.cyan },
      itemStyle: { color: COLORS.cyan, borderColor: '#0a1929', borderWidth: 2 },
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(0, 212, 255, 0.3)' }, { offset: 1, color: 'rgba(0, 212, 255, 0.02)' }]) },
    }],
  }, { notMerge: true })
}

function setModelDistOption(data: any) {
  if (!chartModel) return
  const dist = data.modelDistribution || []
  chartModel.setOption({
    ...getBaseOption(),
    tooltip: { ...getBaseOption().tooltip, trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 20, top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLine: { show: false }, axisLabel: { color: '#7da6c4', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(58, 80, 102, 0.2)' } } },
    yAxis: { type: 'category', data: dist.map((d: any) => d.name), axisLine: { lineStyle: { color: '#3a5066' } }, axisLabel: { color: '#7da6c4', fontSize: 11 }, axisTick: { show: false }, inverse: true },
    series: [{
      type: 'bar',
      data: dist.map((d: any, i: number) => ({ value: d.value, itemStyle: { color: colorPalette[i % colorPalette.length], borderRadius: [0, 4, 4, 0] } })),
      barWidth: '50%',
      label: { show: true, position: 'right', color: '#a0c4d8', fontSize: 11 },
    }],
  }, { notMerge: true })
}

function setRegionDistOption() {
  if (!chartRegion) return
  // Guizhou 9 cities/prefectures — mock device counts
  const regionData = [
    { name: '贵阳市', value: 856, coord: [106.7135, 26.5783] },
    { name: '遵义市', value: 620, coord: [106.9273, 27.7256] },
    { name: '毕节市', value: 485, coord: [105.2847, 27.3019] },
    { name: '六盘水市', value: 380, coord: [104.8300, 26.5937] },
    { name: '安顺市', value: 310, coord: [105.9462, 26.2453] },
    { name: '铜仁市', value: 250, coord: [109.1896, 27.7306] },
    { name: '黔南布依族苗族自治州', value: 168, coord: [107.5170, 26.2582] },
    { name: '黔东南苗族侗族自治州', value: 130, coord: [107.8834, 26.7346] },
    { name: '黔西南布依族苗族自治州', value: 95, coord: [104.9064, 25.0949] },
  ]
  const maxVal = Math.max(...regionData.map(d => d.value))

  chartRegion.setOption({
    ...getBaseOption(),
    tooltip: {
      ...getBaseOption().tooltip,
      trigger: 'item',
      formatter: (p: any) => {
        if (p.seriesType === 'scatter') {
          return `<b>${p.name}</b><br/>设备数: ${p.value} 台`
        }
        return `<b>${p.name}</b><br/>设备数: ${p.value || 0} 台`
      },
    },
    visualMap: {
      type: 'continuous',
      min: 0,
      max: maxVal,
      right: 20,
      bottom: 10,
      text: ['多', '少'],
      textStyle: { color: '#7da6c4', fontSize: 10 },
      calculable: false,
      inRange: { color: ['#0a2a4a', '#0d4a7a', '#1a6dba', '#00d4ff', '#52c41a'] },
      itemWidth: 12,
      itemHeight: 80,
    },
    geo: {
      map: 'guizhou',
      roam: false,
      zoom: 1.45,
      center: [106.8, 26.5],
      label: {
        show: true,
        color: '#a0c4d8',
        fontSize: 11,
      },
      itemStyle: {
        areaColor: '#0a2a4a',
        borderColor: 'rgba(0, 212, 255, 0.35)',
        borderWidth: 1,
        shadowColor: 'rgba(0, 212, 255, 0.2)',
        shadowBlur: 10,
      },
      emphasis: {
        label: { color: '#ffffff', fontSize: 12 },
        itemStyle: { areaColor: '#1a5a8a', borderColor: '#00d4ff', borderWidth: 2, shadowBlur: 20 },
      },
    },
    series: [
      // Map region data (choropleth)
      {
        type: 'map',
        map: 'guizhou',
        geoIndex: 0,
        data: regionData.map(d => ({ name: d.name, value: d.value })),
      },
      // Scatter points for city markers
      {
        type: 'scatter',
        coordinateSystem: 'geo',
        data: regionData.map(d => ({
          name: d.name,
          value: d.value,
          coord: d.coord,
        })),
        symbolSize: (val: number) => Math.max(8, Math.min(28, val / maxVal * 28)),
        itemStyle: {
          color: '#00d4ff',
          shadowColor: '#00d4ff',
          shadowBlur: 10,
          borderColor: '#ffffff',
          borderWidth: 1,
        },
        label: {
          show: true,
          formatter: '{c}',
          position: 'right',
          color: '#00d4ff',
          fontSize: 10,
          fontWeight: 'bold',
        },
        emphasis: {
          scale: 1.5,
          itemStyle: { color: '#52c41a', shadowColor: '#52c41a' },
        },
      },
      // Ripple effect for top cities
      {
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: regionData.filter(d => d.value >= 300).map(d => ({
          name: d.name,
          value: d.value,
          coord: d.coord,
        })),
        symbolSize: (val: number) => Math.max(6, Math.min(16, val / maxVal * 16)),
        rippleEffect: { brushType: 'stroke', scale: 3, period: 4 },
        itemStyle: { color: '#52c41a', shadowColor: '#52c41a', shadowBlur: 8 },
        label: { show: false },
        zlevel: 2,
      },
    ],
  }, { notMerge: true })
}

function setOrderStatusOption(data: any) {
  if (!chartOrder) return
  chartOrder.setOption({
    ...getBaseOption(),
    tooltip: { ...getBaseOption().tooltip, trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 5, textStyle: { color: '#7da6c4', fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
    series: [{
      type: 'pie', radius: '60%', center: ['50%', '42%'],
      itemStyle: { borderColor: '#0a1929', borderWidth: 2 },
      label: { color: '#a0c4d8', fontSize: 11, formatter: '{b}: {c}' },
      labelLine: { lineStyle: { color: '#3a5066' } },
      data: [
        { value: data.paidOrders || 0, name: '已支付', itemStyle: { color: COLORS.green } },
        { value: data.pendingOrders || 0, name: '待支付', itemStyle: { color: COLORS.yellow } },
        { value: data.cancelledOrders || 0, name: '已取消', itemStyle: { color: COLORS.orange } },
        { value: data.refundedOrders || 0, name: '已退款', itemStyle: { color: COLORS.red } },
      ].filter(d => d.value > 0),
    }],
  }, { notMerge: true })
}

function setWorkOrderOption(data: any) {
  if (!chartWorker) return
  chartWorker.setOption({
    ...getBaseOption(),
    tooltip: { ...getBaseOption().tooltip, trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 50, right: 20, top: 20, bottom: 50 },
    xAxis: { type: 'category', data: ['工单总数', '已完成', '待处理', '运维人员', '活跃人员'], axisLine: { lineStyle: { color: '#3a5066' } }, axisLabel: { color: '#7da6c4', fontSize: 10 }, axisTick: { show: false } },
    yAxis: { type: 'value', axisLine: { show: false }, axisLabel: { color: '#7da6c4', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(58, 80, 102, 0.2)' } } },
    series: [{
      type: 'bar',
      data: [
        { value: data.totalWorkOrders || 0, itemStyle: { color: COLORS.cyan } },
        { value: data.completedWorkOrders || 0, itemStyle: { color: COLORS.green } },
        { value: data.pendingWorkOrders || 0, itemStyle: { color: COLORS.orange } },
        { value: data.totalWorkers || 0, itemStyle: { color: COLORS.purple } },
        { value: data.activeWorkers || 0, itemStyle: { color: COLORS.yellow } },
      ],
      barWidth: '40%',
      label: { show: true, position: 'top', color: '#a0c4d8', fontSize: 11 },
      itemStyle: { borderRadius: [4, 4, 0, 0] },
    }],
  }, { notMerge: true })
}

function setRevenueOption(data: any) {
  if (!chartRevenue) return
  const days = Array.from({ length: 7 }, (_, i) => {
    const date = new Date()
    date.setDate(date.getDate() - (6 - i))
    return `${date.getMonth() + 1}/${date.getDate()}`
  })
  const revenue = [4280, 5120, 3890, 6340, 5870, 7230, 5423]
  const commission = [420, 520, 380, 640, 570, 720, 540]

  chartRevenue.setOption({
    ...getBaseOption(),
    tooltip: { ...getBaseOption().tooltip, trigger: 'axis' },
    legend: { top: 5, textStyle: { color: '#7da6c4', fontSize: 11 }, itemWidth: 10, itemHeight: 10 },
    grid: { left: 55, right: 55, top: 35, bottom: 30 },
    xAxis: { type: 'category', data: days, boundaryGap: true, axisLine: { lineStyle: { color: '#3a5066' } }, axisLabel: { color: '#7da6c4', fontSize: 10 }, axisTick: { show: false } },
    yAxis: [
      { type: 'value', name: '营收(元)', nameTextStyle: { color: '#7da6c4' }, axisLine: { show: false }, axisLabel: { color: '#7da6c4', fontSize: 10 }, splitLine: { lineStyle: { color: 'rgba(58, 80, 102, 0.2)' } } },
      { type: 'value', name: '佣金(元)', nameTextStyle: { color: '#7da6c4' }, axisLine: { show: false }, axisLabel: { color: '#7da6c4', fontSize: 10 }, splitLine: { show: false } },
    ],
    series: [
      {
        name: '日营收', type: 'bar', data: revenue, barWidth: '35%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: COLORS.cyan }, { offset: 1, color: 'rgba(0, 212, 255, 0.2)' }]),
          borderRadius: [4, 4, 0, 0],
        },
      },
      {
        name: '日佣金', type: 'line', yAxisIndex: 1, data: commission, smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2, color: COLORS.yellow }, itemStyle: { color: COLORS.yellow },
      },
    ],
  }, { notMerge: true })
}
</script>

<style scoped lang="scss">
.bigscreen {
  width: 100vw;
  height: 100vh;
  background: #0a1929;
  background-image:
    radial-gradient(ellipse at top, rgba(0, 212, 255, 0.08), transparent 60%),
    radial-gradient(ellipse at bottom, rgba(114, 46, 209, 0.06), transparent 60%),
    linear-gradient(rgba(0, 212, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 212, 255, 0.02) 1px, transparent 1px);
  background-size: 100% 100%, 100% 100%, 40px 40px, 40px 40px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: #e0e0e0;
  font-family: 'Microsoft YaHei', sans-serif;
}

/* Header */
.bs-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: linear-gradient(180deg, rgba(0, 212, 255, 0.08), transparent);
  border-bottom: 1px solid rgba(0, 212, 255, 0.15);
  flex-shrink: 0;

  .bs-header-left {
    flex: 1;
  }

  .bs-header-center {
    flex: 2;
    text-align: center;
  }

  .bs-header-right {
    flex: 1;
    text-align: right;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
  }

  .live-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #fa541c;
    flex-shrink: 0;

    &.active {
      background: #52c41a;
      animation: livePulse 2s infinite;
    }
  }

  .live-text {
    font-size: 12px;
    color: #7da6c4;
    flex-shrink: 0;
  }

  @keyframes livePulse {
    0% { box-shadow: 0 0 0 0 rgba(82, 196, 26, 0.6); }
    70% { box-shadow: 0 0 0 6px rgba(82, 196, 26, 0); }
    100% { box-shadow: 0 0 0 0 rgba(82, 196, 26, 0); }
  }

  .bs-title {
    font-size: 26px;
    font-weight: 700;
    background: linear-gradient(180deg, #ffffff 0%, #00d4ff 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: 4px;
    text-shadow: 0 0 20px rgba(0, 212, 255, 0.3);
  }

  .bs-time {
    font-size: 14px;
    color: #7da6c4;
    font-family: 'Courier New', monospace;
  }

  .back-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    cursor: pointer;
    color: #7da6c4;
    font-size: 13px;
    padding: 6px 12px;
    border: 1px solid rgba(0, 212, 255, 0.2);
    border-radius: 4px;
    transition: all 0.2s;

    &:hover {
      color: #00d4ff;
      border-color: rgba(0, 212, 255, 0.5);
      background: rgba(0, 212, 255, 0.05);
    }
  }
}

/* Body */
.bs-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 20px 12px;
  overflow: hidden;
}

/* KPI Row */
.kpi-row {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.kpi-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: linear-gradient(135deg, rgba(16, 42, 67, 0.6), rgba(10, 25, 41, 0.4));
  border: 1px solid rgba(0, 212, 255, 0.12);
  border-radius: 6px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;

  &:hover {
    border-color: var(--accent, #00d4ff);
    box-shadow: 0 0 16px rgba(0, 212, 255, 0.15);
  }

  .kpi-icon {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    background: rgba(0, 212, 255, 0.1);
    color: var(--accent, #00d4ff);
    font-size: 18px;
    flex-shrink: 0;
    border: 1px solid rgba(0, 212, 255, 0.2);
  }

  .kpi-info {
    flex: 1;
    min-width: 0;
  }

  .kpi-value {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }

  .kpi-num {
    font-size: 24px;
    font-weight: 700;
    color: var(--accent, #00d4ff);
    text-shadow: 0 0 12px rgba(0, 212, 255, 0.3);
    font-family: 'DIN', 'Impact', 'Courier New', sans-serif;
    line-height: 1;
  }

  .kpi-unit {
    font-size: 11px;
    color: #7da6c4;
  }

  .kpi-label {
    font-size: 11px;
    color: #7da6c4;
    margin-top: 2px;
  }

  .kpi-deco {
    position: absolute;
    top: 0;
    right: 0;
    width: 48px;
    height: 48px;
    background: radial-gradient(circle at top right, var(--accent, #00d4ff)22, transparent 70%);
    pointer-events: none;
  }
}

/* Chart Row */
.chart-row {
  display: flex;
  gap: 10px;
  flex: 1;
  min-height: 0;

  &.top-row {
    flex: 1;
    min-height: 0;
  }

  &.bottom-row {
    flex: 0 0 auto;
    height: 180px;
  }

  &.map-row {
    flex: 1.4;
    min-height: 360px;

    .side-panel {
      flex: 0 0 25%;
    }
    .map-panel {
      flex: 1 1 50%;
      min-width: 0;
    }
  }
}

.chart-panel {
  flex: 1;
  background: linear-gradient(135deg, rgba(16, 42, 67, 0.5), rgba(10, 25, 41, 0.3));
  border: 1px solid rgba(0, 212, 255, 0.1);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 2px;
    background: linear-gradient(90deg, transparent, rgba(0, 212, 255, 0.3), transparent);
  }
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  font-size: 14px;
  color: #c0d8e8;
  border-bottom: 1px solid rgba(0, 212, 255, 0.08);
  flex-shrink: 0;

  .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    display: inline-block;

    &.dot-cyan { background: #00d4ff; box-shadow: 0 0 6px #00d4ff; }
    &.dot-green { background: #52c41a; box-shadow: 0 0 6px #52c41a; }
    &.dot-purple { background: #722ed1; box-shadow: 0 0 6px #722ed1; }
    &.dot-orange { background: #fa541c; box-shadow: 0 0 6px #fa541c; }
    &.dot-yellow { background: #fadb14; box-shadow: 0 0 6px #fadb14; }
    &.dot-red { background: #ff4d4f; box-shadow: 0 0 6px #ff4d4f; }
  }
}

.chart-box {
  flex: 1;
  min-height: 0;
}

/* Alert List */
.alert-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 10px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0, 212, 255, 0.2); border-radius: 2px; }
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  font-size: 12px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.05);
  color: #a0c4d8;

  .alert-time {
    color: #5a80a0;
    font-family: 'Courier New', monospace;
    flex-shrink: 0;
  }

  .alert-tag {
    padding: 1px 6px;
    border-radius: 3px;
    font-size: 10px;
    flex-shrink: 0;
  }

  .alert-msg {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &.warning .alert-tag { background: rgba(250, 217, 24, 0.15); color: #fadb14; border: 1px solid rgba(250, 217, 24, 0.3); }
  &.danger .alert-tag { background: rgba(255, 77, 79, 0.15); color: #ff4d4f; border: 1px solid rgba(255, 77, 79, 0.3); }
  &.warning .alert-msg { color: #d4b850; }
  &.danger .alert-msg { color: #d47070; }
}

/* Order List */
.order-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px 10px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0, 212, 255, 0.2); border-radius: 2px; }
}

.order-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 8px;
  font-size: 12px;
  border-bottom: 1px solid rgba(0, 212, 255, 0.05);
  color: #a0c4d8;

  .order-no {
    color: #7da6c4;
    font-family: 'Courier New', monospace;
    flex: 0 0 130px;
    font-size: 11px;
  }

  .order-type {
    padding: 1px 6px;
    border-radius: 3px;
    font-size: 10px;
    flex-shrink: 0;
    &.t-cyan { background: rgba(0, 212, 255, 0.1); color: #00d4ff; }
    &.t-red { background: rgba(255, 77, 79, 0.1); color: #ff4d4f; }
    &.t-blue { background: rgba(24, 144, 255, 0.1); color: #1890ff; }
    &.t-purple { background: rgba(114, 46, 209, 0.1); color: #722ed1; }
  }

  .order-status {
    flex: 1;
    font-size: 11px;
    &.s-green { color: #52c41a; }
    &.s-yellow { color: #fadb14; }
    &.s-cyan { color: #00d4ff; }
    &.s-orange { color: #fa541c; }
  }

  .order-time {
    color: #5a80a0;
    font-family: 'Courier New', monospace;
    flex-shrink: 0;
  }
}
</style>
