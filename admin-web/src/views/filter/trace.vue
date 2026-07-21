<template>
  <div class="filter-trace-page">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <span>滤芯追溯详情</span>
      </template>

      <!-- 基本信息 + 二维码 -->
      <div class="info-section" v-if="info">
        <el-descriptions :column="2" border class="info-card">
          <el-descriptions-item label="滤芯ID">{{ info.filterId }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ info.modelName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(info.lifecycleStatus || info.status)" size="small">
              {{ statusLabel(info.lifecycleStatus || info.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属设备">{{ info.currentDeviceId || info.deviceId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="安装时间">{{ formatDateTime(info.installedAt) }}</el-descriptions-item>
          <el-descriptions-item label="更换时间">{{ formatDateTime(info.replacedAt) }}</el-descriptions-item>
          <el-descriptions-item label="入库时间">{{ formatDateTime(info.updatedAt || info.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="info.qrCodeUrl" class="qr-box">
          <p class="qr-title">滤芯二维码</p>
          <img :src="info.qrCodeUrl" alt="QR" class="qr-img" />
          <el-button type="primary" link size="small" @click="downloadQR">下载</el-button>
        </div>
      </div>

      <!-- 生命周期时间线 -->
      <el-card shadow="never" class="timeline-card">
        <template #header>
          <span>生命周期事件</span>
        </template>
        <el-timeline v-if="traceData && traceData.length > 0">
          <el-timeline-item
            v-for="(item, index) in traceData"
            :key="index"
            :timestamp="formatDateTime(item.createdAt)"
            :type="getTimelineType(item.toStatus)"
            placement="top"
          >
            <el-card shadow="hover" class="timeline-event-card">
              <p>
                <strong>事件：</strong>
                <el-tag :type="getTimelineType(item.toStatus)" size="small">
                  {{ statusLabel(item.toStatus) || item.toStatus }}
                </el-tag>
                <span v-if="item.fromStatus" class="status-transition">
                  （{{ statusLabel(item.fromStatus) || item.fromStatus }} → {{ statusLabel(item.toStatus) || item.toStatus }}）
                </span>
              </p>
              <p v-if="item.remark"><strong>描述：</strong>{{ item.remark }}</p>
              <p v-if="item.operatorName"><strong>操作人：</strong>{{ item.operatorName }}</p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无生命周期事件" />
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { traceFilter, getFilter } from '@/api/filter'
import { formatDateTime, statusLabel, statusTagType } from '@/utils/format'

const route = useRoute()

const filterId = route.params.filterId as string

const loading = ref(false)
const info = ref<any>(null)
const traceData = ref<any[]>([])

function getTimelineType(eventType: string): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    IN_STOCK: 'primary',
    REGISTERED: 'primary',
    ALLOCATED: 'info',
    PENDING_INSTALL: 'warning',
    INSTALLED: 'success',
    IN_USE: 'success',
    ACTIVATED: 'success',
    REPLACED: 'warning',
    SCRAPPED: 'danger',
    FAULT: 'danger',
    DELETED: 'info',
  }
  return map[eventType] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const [infoRes, traceRes] = await Promise.all([
      getFilter(filterId),
      traceFilter(filterId),
    ])
    if (infoRes.code === 200) {
      info.value = infoRes.data
    }
    if (traceRes.code === 200) {
      traceData.value = Array.isArray(traceRes.data) ? traceRes.data : []
    }
  } finally {
    loading.value = false
  }
}

function downloadQR() {
  if (!info.value?.qrCodeUrl) return
  const link = document.createElement('a')
  link.href = info.value.qrCodeUrl
  link.download = `QR_${info.value.filterId}.png`
  link.click()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.filter-trace-page {
  padding: 16px;

  .info-section {
    display: flex;
    gap: 24px;
    align-items: flex-start;
    margin-bottom: 16px;
  }

  .info-card {
    flex: 1;
  }

  .qr-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
  }

  .qr-title {
    font-size: 13px;
    color: #909399;
    margin: 0;
  }

  .qr-img {
    width: 140px;
    height: 140px;
    border: 1px solid #ebeef5;
    border-radius: 4px;
  }

  .timeline-card {
    .timeline-event-card {
      p {
        margin: 4px 0;
      }

      .status-transition {
        margin-left: 4px;
        color: #909399;
        font-size: 13px;
      }
    }
  }
}

@media (max-width: 768px) {
  .info-section {
    flex-direction: column;
  }
}
</style>
