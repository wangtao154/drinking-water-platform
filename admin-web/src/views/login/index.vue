<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>直饮水平台</h1>
        <p>管理后台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" autocomplete="off" @submit.prevent="handleLogin">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="请输入账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="aiConsent" class="ai-consent-item">
          <el-checkbox v-model="form.aiConsent">
            我已阅读并同意
            <button type="button" class="policy-link" @click.stop="policyDialogVisible = true">《用户协议》</button>
            和
            <button type="button" class="policy-link" @click.stop="policyDialogVisible = true">《隐私政策》</button>
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" style="width: 100%" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <footer class="login-footer">
      <span>Copyright © 2017-2026 贵州省聚控云科技有限公司 版权所有</span>
      <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener noreferrer">黔ICP备19008617号</a>
    </footer>
    <AiAssistantPolicyDialog v-model="policyDialogVisible" />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import AiAssistantPolicyDialog from '@/components/AiAssistantPolicyDialog.vue'
import { AI_ASSISTANT_POLICY_VERSION } from '@/constants/aiAssistantPolicy'
import { acceptAiAssistantConsent } from '@/api/system'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const policyDialogVisible = ref(false)

const form = reactive({
  account: '',
  password: '',
  aiConsent: false
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  aiConsent: [{
    validator: (_rule, value, callback) => value ? callback() : callback(new Error('请先阅读并同意用户协议与隐私政策')),
    trigger: 'change'
  }]
}

async function handleLogin() {
  if (!formRef.value || loading.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({ account: form.account, password: form.password })
    try {
      await acceptAiAssistantConsent({ policyVersion: AI_ASSISTANT_POLICY_VERSION, source: 'LOGIN' })
    } catch (consentError) {
      console.warn('Platform policy confirmation recording unavailable; AI assistant remains unavailable until confirmed.', consentError)
      ElMessage.warning('用户协议与隐私政策确认记录暂不可用，已登录后台；AI 助手暂不可使用，请稍后重试。')
    }
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.replace(redirect).catch((error) => {
      console.error('login redirect error', error)
    })
  } catch (error) {
    console.error('login error', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 32px 16px 68px;
  box-sizing: border-box;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;

  h1 {
    font-size: 28px;
    color: #303133;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #909399;
  }
}

.ai-consent-item { margin-top: -4px; margin-bottom: 16px; }
.ai-consent-item :deep(.el-form-item__content) { line-height: 1.55; }
.ai-consent-item :deep(.el-checkbox__label) { color: #718096; font-size: 12px; white-space: normal; }
.policy-link { margin: 0; padding: 0; border: 0; background: transparent; color: #1677ff; cursor: pointer; font: inherit; }
.login-footer {
  position: absolute;
  right: 16px;
  bottom: 20px;
  left: 16px;
  display: flex;
  justify-content: center;
  gap: 12px;
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
  line-height: 1.5;
  text-align: center;

  a {
    color: inherit;
    text-decoration: none;

    &:hover,
    &:focus-visible { color: #fff; text-decoration: underline; }
  }
}

@media (max-width: 520px) {
  .login-container { padding-bottom: 96px; }
  .login-box { width: min(400px, 100%); padding: 28px 24px; }
  .login-footer { flex-direction: column; gap: 2px; bottom: 16px; }
}
</style>
