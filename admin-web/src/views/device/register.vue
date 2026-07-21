<template>
  <div class="device-register-page">
    <el-card shadow="never">
      <template #header>
        <span class="card-title">设备登记</span>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 600px"
      >
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="form.deviceId" placeholder="请输入设备ID（业务编号，如 DEV-00001）" />
        </el-form-item>

        <el-form-item label="SN" prop="sn">
          <el-input
            v-model="form.sn"
            placeholder="j+6位数字，如 j082438（控制板生成）"
            maxlength="7"
          />
        </el-form-item>

        <el-form-item label="设备型号" prop="modelId">
          <el-select
            v-model="form.modelId"
            placeholder="请选择设备型号"
            filterable
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

        <el-form-item label="ICCID" prop="iccid">
          <el-input v-model="form.iccid" placeholder="请输入ICCID（选填）" />
        </el-form-item>

        <el-form-item label="IMEI" prop="imei">
          <el-input v-model="form.imei" placeholder="请输入IMEI（选填）" />
        </el-form-item>

        <el-form-item label="生产日期" prop="productionDate">
          <el-date-picker
            v-model="form.productionDate"
            type="date"
            placeholder="请选择生产日期（选填）"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="生产批次" prop="productionBatch">
          <el-input v-model="form.productionBatch" placeholder="请输入生产批次（选填）" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            提交
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button @click="handleBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { registerDevice, listDeviceModels } from '@/api/device'
import type { DeviceModelVO } from '@/types/api'

const router = useRouter()
const formRef = ref<FormInstance>()

const form = reactive({
  deviceId: '',
  sn: '',
  modelId: '' as number | string,
  iccid: '',
  imei: '',
  productionDate: '',
  productionBatch: '',
})

const rules: FormRules = {
  deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
  sn: [
    { required: true, message: '请输入设备SN', trigger: 'blur' },
    { pattern: /^j\d{6}$/, message: 'SN格式必须为j+6位数字，如j082438', trigger: 'blur' },
  ],
  modelId: [{ required: true, message: '请选择设备型号', trigger: 'change' }],
}

const submitting = ref(false)
const modelOptions = ref<DeviceModelVO[]>([])

async function loadModels() {
  try {
    const res = await listDeviceModels()
    if (res.code === 200) {
      modelOptions.value = res.data || []
    }
  } catch (e) {
    console.error('Failed to load device models', e)
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const data: Record<string, unknown> = {
        deviceId: form.deviceId,
        sn: form.sn,
        modelId: form.modelId,
      }
      if (form.iccid) data.iccid = form.iccid
      if (form.imei) data.imei = form.imei
      if (form.productionDate) data.productionDate = form.productionDate
      if (form.productionBatch) data.productionBatch = form.productionBatch

      const res = await registerDevice(data as any)
      if (res.code === 200) {
        ElMessage.success('设备登记成功')
        router.push('/devices/list')
      } else {
        ElMessage.error(res.message || '设备登记失败')
      }
    } finally {
      submitting.value = false
    }
  })
}

function handleReset() {
  formRef.value?.resetFields()
}

function handleBack() {
  router.push('/devices/list')
}

onMounted(() => {
  loadModels()
})
</script>

<style scoped lang="scss">
.device-register-page {
  padding: 16px;

  .card-title {
    font-size: 18px;
    font-weight: 600;
  }
}
</style>
