<template>
  <div class="customer-detail-page">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>客户详情</span>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </template>

      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="客户名称">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="客户类型">
          <el-tag :type="detail.customerType === 'ENTERPRISE' ? 'warning' : 'info'" size="small">
            {{ statusLabel(detail.customerType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ detail.idCard || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.customerType === 'ENTERPRISE'" label="企业名称">
          {{ detail.enterpriseName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="地址">{{ detail.address || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属经销商">{{ detail.dealerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCustomer } from '@/api/user'
import type { CustomerVO } from '@/types/api'
import { formatDateTime, statusLabel } from '@/utils/format'

const route = useRoute()
const id = Number(route.params.id)

const loading = ref(false)
const detail = ref<CustomerVO | null>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await getCustomer(id)
    if (res.code === 200) {
      detail.value = res.data
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.customer-detail-page {
  padding: 16px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
