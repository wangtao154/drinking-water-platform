<template>
  <div class="resources-page">
    <header class="page-heading">
      <div><h2>系统资源</h2><span class="muted">采集时间：{{ time(data?.sampledAt) }}</span></div>
      <div class="controls">
        <span>自动刷新 · 10秒</span><el-switch v-model="autoRefresh" aria-label="自动刷新" />
        <el-tooltip content="刷新资源数据"><el-button :icon="Refresh" :loading="loading" circle aria-label="刷新资源数据" @click="load" /></el-tooltip>
      </div>
    </header>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-alert v-if="data && data.collectorStatus !== 'OK'" :title="data.collectorStatus === 'STALE' ? '主机采集数据已过期，当前显示最后一次采集结果' : '主机采集暂不可用'" type="warning" :closable="false" show-icon />
    <el-alert v-if="data?.docker?.status === 'UNAVAILABLE'" title="Docker 指标采集暂不可用" type="warning" :closable="false" show-icon />
    <el-alert v-if="data?.redis.status === 'UNAVAILABLE'" title="Redis 指标采集暂不可用" type="warning" :closable="false" show-icon />
    <section class="metrics" v-loading="loading && !data">
      <div class="metric"><div class="metric-label"><el-icon><Cpu /></el-icon>主机 CPU</div><strong :class="{ danger: (data?.host?.cpuPercent || 0) >= 90 }">{{ percent(data?.host?.cpuPercent) }}</strong><small>{{ data?.host?.cores ?? '—' }} 个逻辑核心 · Windows 主机</small></div>
      <div class="metric"><div class="metric-label"><el-icon><Monitor /></el-icon>主机内存</div><strong :class="{ danger: (memoryPercent || 0) >= 90 }">{{ percent(memoryPercent) }}</strong><small>{{ bytes(data?.host?.usedMemoryBytes) }} / {{ bytes(data?.host?.totalMemoryBytes) }}</small></div>
      <div class="metric"><div class="metric-label"><el-icon><Coin /></el-icon>Redis 内存</div><strong>{{ bytes(data?.redis.usedBytes) }}</strong><small>上限：{{ data?.redis.maxBytes === 0 ? '未设置' : bytes(data?.redis.maxBytes) }}</small></div>
      <div class="metric"><div class="metric-label"><el-icon><Box /></el-icon>Docker 容器</div><strong>{{ data?.docker?.status === 'OK' ? `${running} / ${containers.length}` : '—' }}</strong><small>运行 / 总数 · 异常 {{ data?.docker?.status === 'OK' ? abnormal : '—' }}</small></div>
    </section>
    <section class="band">
      <div class="section-heading"><h3>资源趋势</h3><el-radio-group v-model="minutes" size="small"><el-radio-button :value="10">10分钟</el-radio-button><el-radio-button :value="30">30分钟</el-radio-button></el-radio-group></div>
      <div ref="chartElement" class="trend-chart" aria-label="主机CPU、内存与Redis内存趋势" />
    </section>
    <section class="band">
      <el-tabs v-model="tab">
        <el-tab-pane label="容器与服务" name="containers">
          <div class="table-tools"><el-input v-model="search" placeholder="容器名称" clearable :prefix-icon="Search" /><el-checkbox v-model="onlyAbnormal">仅异常</el-checkbox></div>
          <el-table :data="filteredContainers" stripe max-height="520" empty-text="暂无容器数据">
            <el-table-column prop="name" label="容器 / 服务" min-width="205" fixed />
            <el-table-column label="状态" width="105"><template #default="{ row }"><el-tag :type="isAbnormal(row.state, row.health) ? 'danger' : 'success'" size="small">{{ state(row.state) }}</el-tag></template></el-table-column>
            <el-table-column label="健康检查" width="110"><template #default="{ row }">{{ health(row.health) }}</template></el-table-column>
            <el-table-column label="CPU" width="90" sortable :sort-method="(a: any,b: any) => (a.cpuPercent ?? -1) - (b.cpuPercent ?? -1)"><template #default="{ row }">{{ percent(row.cpuPercent) }}</template></el-table-column>
            <el-table-column label="内存 / 可用上限" min-width="185"><template #default="{ row }">{{ row.memoryUsage ?? '—' }}</template></el-table-column>
            <el-table-column label="内存占比" width="100"><template #default="{ row }">{{ percent(row.memoryPercent) }}</template></el-table-column>
            <el-table-column prop="restarts" label="重启次数" width="100" sortable />
            <el-table-column prop="pids" label="进程/线程" width="100" />
            <el-table-column label="网络接收 / 发送" min-width="165"><template #default="{ row }">{{ row.networkIO ?? '—' }}</template></el-table-column>
            <el-table-column label="磁盘读 / 写" min-width="165"><template #default="{ row }">{{ row.blockIO ?? '—' }}</template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="Redis" name="redis">
          <el-descriptions :column="columns" border>
            <el-descriptions-item v-for="item in redisItems" :key="item.label" :label="item.label">{{ item.value }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="主机与磁盘" name="host">
          <div class="host-info">{{ data?.host?.name || '—' }} · {{ data?.host?.os || '—' }}</div>
          <el-table :data="data?.host?.disks || []" empty-text="暂无主机磁盘数据">
            <el-table-column prop="name" label="磁盘" width="100" />
            <el-table-column label="总容量" min-width="120"><template #default="{ row }">{{ bytes(row.totalBytes) }}</template></el-table-column>
            <el-table-column label="已用" min-width="120"><template #default="{ row }">{{ bytes(row.totalBytes - row.freeBytes) }}</template></el-table-column>
            <el-table-column label="可用" min-width="120"><template #default="{ row }">{{ bytes(row.freeBytes) }}</template></el-table-column>
            <el-table-column label="使用率" min-width="180"><template #default="{ row }"><el-progress :percentage="Math.round(ratio(row.totalBytes - row.freeBytes, row.totalBytes) || 0)" :status="(ratio(row.totalBytes - row.freeBytes, row.totalBytes) || 0) >= 90 ? 'exception' : undefined" /></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="系统服务 JVM" name="jvm">
          <el-descriptions :column="columns" border>
            <el-descriptions-item label="采集服务">system-service</el-descriptions-item>
            <el-descriptions-item label="堆内存已用">{{ bytes(data?.jvm.heapUsedBytes) }}</el-descriptions-item>
            <el-descriptions-item label="堆内存上限">{{ bytes(data?.jvm.heapMaxBytes) }}</el-descriptions-item>
            <el-descriptions-item label="线程数">{{ data?.jvm.threads ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="运行时长">{{ duration(data?.jvm.uptimeSeconds) }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onActivated, onDeactivated, onBeforeUnmount } from 'vue'
import { Cpu, Monitor, Coin, Box, Refresh, Search } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getSystemResources, type ResourceSnapshot } from '@/api/resources'

defineOptions({ name: 'SystemResources' })
const data = ref<ResourceSnapshot>()
const loading = ref(false), autoRefresh = ref(true), onlyAbnormal = ref(false)
const error = ref(''), search = ref(''), tab = ref('containers'), minutes = ref(30)
const chartElement = ref<HTMLElement>(), columns = ref(3)
let chart: echarts.ECharts | undefined, observer: ResizeObserver | undefined
let timer: ReturnType<typeof setTimeout> | undefined
let active = false
const containers = computed(() => data.value?.docker?.containers || [])
const isAbnormal = (state: string, health: string) => !['running', 'completed'].includes(state) || health === 'unhealthy'
const running = computed(() => containers.value.filter(row => row.state === 'running').length)
const abnormal = computed(() => containers.value.filter(row => isAbnormal(row.state, row.health)).length)
const filteredContainers = computed(() => containers.value.filter(row => row.name.toLowerCase().includes(search.value.toLowerCase()) && (!onlyAbnormal.value || isAbnormal(row.state, row.health))))
const ratio = (used?: number | null, total?: number | null) => used == null || !total ? null : used / total * 100
const memoryPercent = computed(() => ratio(data.value?.host?.usedMemoryBytes, data.value?.host?.totalMemoryBytes))
const percent = (value?: number | null) => value == null ? '—' : `${value.toFixed(1)}%`
function bytes(value?: number | null) {
  if (value == null || value < 0) return '—'
  const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB']
  let index = 0
  while (value >= 1024 && index < units.length - 1) { value /= 1024; index++ }
  return `${value.toFixed(index ? 2 : 0)} ${units[index]}`
}
const time = (value?: string | null) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
const duration = (value?: number | null) => value == null ? '—' : `${Math.floor(value / 86400)}天 ${Math.floor(value % 86400 / 3600)}小时 ${Math.floor(value % 3600 / 60)}分钟`
const state = (value: string) => (({ running: '运行', completed: '已完成', exited: '已停止', restarting: '重启中', paused: '暂停', created: '待启动', dead: '异常' } as Record<string,string>)[value] || value)
const health = (value: string) => (({ healthy: '健康', unhealthy: '异常', starting: '检查中', none: '未配置' } as Record<string,string>)[value] || value)
const redisItems = computed(() => {
  const r = data.value?.redis
  return [
    { label: '状态', value: r?.status === 'OK' ? '正常' : '不可用' }, { label: '版本', value: r?.version ?? '—' },
    { label: '已用内存', value: bytes(r?.usedBytes) }, { label: '内存上限', value: r?.maxBytes === 0 ? '未设置' : bytes(r?.maxBytes) },
    { label: '常驻内存 RSS', value: bytes(r?.rssBytes) }, { label: '客户端连接数', value: r?.clients ?? '—' },
    { label: '阻塞连接数', value: r?.blockedClients ?? '—' }, { label: '键数量（全部数据库）', value: r?.keys ?? '—' },
    { label: '命中率（启动以来）', value: percent(r?.hitRate) }, { label: '累计淘汰键数', value: r?.evictedKeys ?? '—' },
    { label: '每秒操作数', value: r?.opsPerSecond ?? '—' }, { label: '运行时长', value: duration(r?.uptimeSeconds) }
  ]
})

function draw() {
  if (!chartElement.value || !active) return
  chart ||= echarts.init(chartElement.value)
  const points = (data.value?.history || []).filter(p => Date.parse(p.sampledAt) >= Date.now() - minutes.value * 60000)
  chart.setOption({
    color: ['#409eff', '#16a085', '#e6a23c'], tooltip: { trigger: 'axis' },
    legend: { bottom: 0 }, grid: { left: 52, right: 64, top: 30, bottom: 58 },
    xAxis: { type: 'time' }, yAxis: [{ type: 'value', min: 0, max: 100, name: '%' }, { type: 'value', name: 'MiB' }],
    series: [
      { name: '主机 CPU', type: 'line', showSymbol: points.length < 2, data: points.map(p => [p.sampledAt, p.cpuPercent]) },
      { name: '主机内存', type: 'line', showSymbol: points.length < 2, data: points.map(p => [p.sampledAt, p.memoryPercent]) },
      { name: 'Redis 内存', type: 'line', yAxisIndex: 1, showSymbol: points.length < 2, data: points.map(p => [p.sampledAt, p.redisUsedBytes == null ? null : +(p.redisUsedBytes / 1048576).toFixed(2)]) }
    ]
  })
}
function schedule() {
  clearTimeout(timer)
  if (active && autoRefresh.value) timer = setTimeout(load, 10000)
}
async function load() {
  if (loading.value || !active) return
  clearTimeout(timer)
  loading.value = true
  try {
    const result = await getSystemResources()
    if (!active) return
    data.value = result.data
    error.value = ''
    await nextTick()
    draw()
  } catch { if (active) error.value = '资源数据刷新失败，当前显示上次结果' }
  finally { loading.value = false; schedule() }
}
function start() {
  if (active) return
  active = true
  load()
  nextTick(() => {
    if (!active || !chartElement.value) return
    observer ||= new ResizeObserver(entries => { columns.value = entries[0].contentRect.width < 650 ? 1 : 3; chart?.resize() })
    observer.observe(chartElement.value)
    draw()
  })
}
function stop() { active = false; clearTimeout(timer); observer?.disconnect() }
watch(autoRefresh, schedule)
watch(minutes, draw)
onMounted(start)
onActivated(start)
onDeactivated(stop)
onBeforeUnmount(() => { stop(); chart?.dispose() })
</script>

<style scoped>
.resources-page { padding: 16px; color: #303133; }
.page-heading,.section-heading,.controls,.table-tools { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.page-heading,.section-heading { justify-content: space-between; margin-bottom: 16px; }
h2 { font-size: 20px; margin: 0 0 6px; } h3 { font-size: 16px; margin: 0; }
.muted,.metric small { color: #909399; font-size: 12px; }
.controls { font-size: 13px; }
.metrics { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 16px; margin: 16px 0; }
.metric { background: #fff; border: 1px solid #e4e7ed; border-radius: 4px; padding: 20px; min-height: 112px; }
.metric-label { display: flex; align-items: center; gap: 8px; color: #606266; font-size: 14px; }
.metric strong { display: block; font-size: 26px; font-weight: 600; margin: 14px 0 8px; overflow-wrap: anywhere; }
.metric:nth-child(2) .el-icon { color: #16a085; }.metric:nth-child(3) .el-icon { color: #e6a23c; }
.metric small { display: block; overflow-wrap: anywhere; }.danger { color: #f56c6c; }
.band { background: #fff; padding: 20px; margin-bottom: 16px; border-top: 1px solid #ebeef5; }
.trend-chart { height: 280px; width: 100%; }
.table-tools { margin: 8px 0 16px; }.table-tools .el-input { width: 240px; max-width: 100%; }
.host-info { margin: 8px 0 18px; font-size: 14px; }
.el-alert { margin-bottom: 12px; }
@media(max-width:1100px) { .metrics { grid-template-columns: repeat(2,minmax(0,1fr)); } }
@media(max-width:600px) { .resources-page { padding: 10px; }.metric { padding: 14px; }.metric strong { font-size: 22px; }.band { padding: 12px; } }
</style>
