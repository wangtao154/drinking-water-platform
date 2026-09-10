import { get } from '@/utils/request'

export interface ResourceSnapshot {
  sampledAt: string | null
  collectorStatus: 'OK' | 'STALE' | 'UNAVAILABLE'
  host: null | { name: string; os: string; cores: number; cpuPercent: number | null; totalMemoryBytes: number; usedMemoryBytes: number; disks: { name: string; totalBytes: number; freeBytes: number }[] }
  docker: null | { status: string; containers: { name: string; state: string; health: string; restarts: number; cpuPercent: number | null; memoryUsage: string | null; memoryPercent: number | null; networkIO: string | null; blockIO: string | null; pids: number | null }[] }
  redis: { status: string; usedBytes: number | null; maxBytes: number | null; rssBytes: number | null; clients: number | null; blockedClients: number | null; keys: number | null; hitRate: number | null; evictedKeys: number | null; opsPerSecond: number | null; uptimeSeconds: number | null; version: string | null }
  jvm: { heapUsedBytes: number; heapMaxBytes: number; threads: number; uptimeSeconds: number }
  history: { sampledAt: string; cpuPercent: number | null; memoryPercent: number | null; redisUsedBytes: number | null }[]
}

const numericMetrics = new Set(['cores', 'cpuPercent', 'totalMemoryBytes', 'usedMemoryBytes', 'totalBytes', 'freeBytes',
  'restarts', 'memoryPercent', 'pids', 'usedBytes', 'maxBytes', 'rssBytes', 'clients', 'blockedClients', 'keys',
  'hitRate', 'evictedKeys', 'opsPerSecond', 'uptimeSeconds', 'heapUsedBytes', 'heapMaxBytes', 'threads', 'redisUsedBytes'])

// The shared backend serializer encodes Java Long values as strings.
function normalizeMetrics(value: unknown, key = ''): unknown {
  if (numericMetrics.has(key) && value != null) {
    const number = Number(value)
    return Number.isFinite(number) && number >= 0 ? number : null
  }
  if (Array.isArray(value)) return value.map(item => normalizeMetrics(item))
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([name, item]) => [name, normalizeMetrics(item, name)]))
  }
  return value
}

export async function getSystemResources() {
  const result = await get<ResourceSnapshot>('/v1/system/resources')
  return { ...result, data: normalizeMetrics(result.data) as ResourceSnapshot }
}
