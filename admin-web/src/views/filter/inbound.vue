<template>
  <div class="filter-inbound-page">
    <el-card shadow="never">
      <template #header>
        <span>滤芯登记</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 560px"
      >
        <el-form-item label="滤芯型号" prop="modelId">
          <el-select
            v-model="form.modelId"
            placeholder="请选择滤芯型号"
            filterable
            remote
            :remote-method="searchModels"
            :loading="modelLoading"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="item in modelOptions"
              :key="item.id"
              :label="item.modelName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="关联设备ID">
          <el-input
            v-model="form.deviceId"
            placeholder="选填"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            提交登记
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 登记成功结果展示 -->
    <el-card v-if="result" shadow="never" class="result-card">
      <template #header>
        <div class="result-header">
          <el-icon color="#67c23a" size="20"><CircleCheck /></el-icon>
          <span>登记成功</span>
        </div>
      </template>
      <div class="result-content">
        <div class="result-info">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="滤芯ID">
              <el-tag type="primary">{{ result.filterId }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="型号编码">{{ result.modelCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="型号名称">{{ result.modelName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生命周期状态">
              <el-tag type="success">{{ result.lifecycleStatus }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        <div v-if="result.qrCodeUrl" class="qr-section">
          <p class="qr-label">滤芯二维码</p>
          <img :src="result.qrCodeUrl" alt="QR Code" class="qr-img" />
          <el-button type="primary" link @click="downloadQR">下载二维码</el-button>
        </div>
      </div>
      <div class="result-actions">
        <el-button type="primary" @click="handleContinue">继续登记</el-button>
        <el-button @click="$router.push('/filters/list')">查看列表</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { registerFilter, pageFilterModels } from '@/api/filter'
import type { FilterModelVO } from '@/types/api'

const formRef = ref<FormInstance>()

const form = reactive({
  modelId: null as number | null,
  deviceId: '',
})

const rules: FormRules = {
  modelId: [{ required: true, message: '请选择滤芯型号', trigger: 'change' }],
}

const modelLoading = ref(false)
const modelOptions = ref<FilterModelVO[]>([])
const submitLoading = ref(false)
const result = ref<any>(null)

async function fetchModels(keyword = '') {
  modelLoading.value = true
  try {
    const res = await pageFilterModels({ pageNum: 1, pageSize: 50, keyword })
    if (res.code === 200) {
      modelOptions.value = res.data.records
    }
  } finally {
    modelLoading.value = false
  }
}

function searchModels(query: string) {
  fetchModels(query)
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data: { filterModelId: number; deviceId?: string } = {
      filterModelId: form.modelId!,
    }
    if (form.deviceId) data.deviceId = form.deviceId

    const res = await registerFilter(data)
    if (res.code === 200) {
      ElMessage.success('滤芯登记成功')
      result.value = res.data
    }
  } finally {
    submitLoading.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
  form.modelId = null
  form.deviceId = ''
}

function handleContinue() {
  result.value = null
  handleReset()
}

function downloadQR() {
  if (!result.value?.qrCodeUrl) return
  const link = document.createElement('a')
  link.href = result.value.qrCodeUrl
  link.download = `QR_${result.value.filterId}.png`
  link.click()
}

onMounted(() => {
  fetchModels()
})
</script>

<style scoped lang="scss">
.filter-inbound-page {
  padding: 16px;
}

.result-card {
  margin-top: 16px;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.result-content {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.result-info {
  flex: 1;
}

.qr-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.qr-label {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.qr-img {
  width: 180px;
  height: 180px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.result-actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .result-content {
    flex-direction: column;
  }
}
</style>
