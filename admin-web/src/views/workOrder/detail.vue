<template>
  <div class="work-order-detail-page" v-loading="loading">
    <!-- 工单信息 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">工单信息</span>
          <el-tag :type="statusTagType(workOrder.orderStatus || '')" size="default">
            {{ statusLabel(workOrder.orderStatus || '') }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单号">{{ workOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ statusLabel(workOrder.orderType || '') }}</el-descriptions-item>
        <el-descriptions-item label="设备SN">{{ workOrder.deviceSn || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户名">{{ workOrder.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工人名">{{ workOrder.workerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址">{{ workOrder.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ workOrder.contactPerson || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ workOrder.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ workOrder.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预约时间">
          {{ workOrder.scheduledTime ? formatDateTime(workOrder.scheduledTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="完成时间">
          {{ workOrder.completedTime ? formatDateTime(workOrder.completedTime) : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="评分">
          {{ workOrder.rating != null ? workOrder.rating : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(workOrder.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="workOrder.remark" label="备注" :span="2">
          {{ workOrder.remark }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 状态流转时间线 -->
    <el-card shadow="never" class="timeline-card">
      <template #header>
        <span class="card-title">状态流转</span>
      </template>

      <el-timeline>
        <el-timeline-item
          v-for="(item, index) in timelineItems"
          :key="index"
          :timestamp="item.time"
          :type="item.type"
          placement="top"
        >
          {{ item.label }}
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 返回按钮 -->
    <div class="footer-actions">
      <el-button @click="handleBack">返回列表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getWorkOrder } from '@/api/workOrder'
import type { WorkOrderVO } from '@/types/api'
import { formatDateTime, statusLabel, statusTagType } from '@/utils/format'

const router = useRouter()
const route = useRoute()

const id = Number(route.params.id)

const loading = ref(false)
const workOrder = reactive<Partial<WorkOrderVO>>({})

const timelineItems = computed(() => {
  const items: { label: string; time: string; type: 'primary' | 'success' | 'warning' | 'info' }[] = []

  if (workOrder.createdAt) {
    items.push({ label: '工单创建', time: formatDateTime(workOrder.createdAt), type: 'primary' })
  }
  if (workOrder.orderStatus === 'DISPATCHED' || workOrder.orderStatus === 'ACCEPTED' || workOrder.orderStatus === 'IN_PROGRESS' || workOrder.orderStatus === 'COMPLETED') {
    items.push({ label: '已派单', time: formatDateTime(workOrder.scheduledTime) || '-', type: 'warning' })
  }
  if (workOrder.orderStatus === 'ACCEPTED' || workOrder.orderStatus === 'IN_PROGRESS' || workOrder.orderStatus === 'COMPLETED') {
    items.push({ label: '已接单', time: '-', type: 'warning' })
  }
  if (workOrder.orderStatus === 'IN_PROGRESS' || workOrder.orderStatus === 'COMPLETED') {
    items.push({ label: '进行中', time: '-', type: 'warning' })
  }
  if (workOrder.orderStatus === 'COMPLETED' && workOrder.completedTime) {
    items.push({ label: '已完成', time: formatDateTime(workOrder.completedTime), type: 'success' })
  }
  if (workOrder.orderStatus === 'CANCELLED') {
    items.push({ label: '已取消', time: '-', type: 'info' })
  }

  return items
})

async function loadData() {
  loading.value = true
  try {
    const res = await getWorkOrder(id)
    if (res.code === 200) {
      Object.assign(workOrder, res.data)
    }
  } finally {
    loading.value = false
  }
}

function handleBack() {
  router.push('/work-orders/list')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.work-order-detail-page {
  padding: 16px;

  .card-title {
    font-size: 18px;
    font-weight: 600;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .info-card {
    margin-bottom: 16px;
  }

  .timeline-card {
    margin-bottom: 16px;
  }

  .footer-actions {
    padding-top: 8px;
  }
}
</style>
