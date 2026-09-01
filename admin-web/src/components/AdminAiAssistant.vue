<template>
  <div class="ai-assistant">
    <el-tooltip content="打开 AI 助手" placement="left">
      <button v-if="!opened" class="assistant-launcher" type="button" @click="openPanel">
        <el-icon><ChatDotRound /></el-icon>
      </button>
    </el-tooltip>

    <section v-show="opened" class="assistant-panel" aria-label="AI 助手">
      <header class="assistant-header">
        <div class="assistant-title">
          <el-icon><MagicStick /></el-icon>
          <span>AI 助手</span>
        </div>
        <div class="assistant-actions">
          <el-tooltip content="清空本次对话" placement="top">
            <el-button text circle aria-label="清空本次对话" :loading="clearingConversation" :disabled="sending" @click="clearConversation">
              <el-icon><Delete /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="投诉、举报或提交改进建议" placement="top">
            <el-button text class="assistant-complaint-button" aria-label="投诉建议" @click="openComplaintDialog">
              投诉建议
            </el-button>
          </el-tooltip>
          <el-tooltip content="收起 AI 助手" placement="top">
            <el-button text circle aria-label="收起 AI 助手" @click="opened = false">
              <el-icon><Close /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </header>

      <main ref="messageContainer" class="assistant-messages">
        <template v-if="messages.length === 0">
          <div class="assistant-welcome">
            <div class="welcome-icon"><el-icon><MagicStick /></el-icon></div>
            <strong>你好，我是直饮水平台 AI 助手</strong>
            <p>可以查询已授权的业务概况，也可以说明后台操作流程。</p>
          </div>
          <div class="suggestion-list">
            <button v-for="item in suggestions" :key="item" type="button" class="suggestion" @click="askSuggestion(item)">
              {{ item }}
            </button>
          </div>
        </template>

        <article v-for="message in messages" :key="message.id" class="message" :class="message.role">
          <div class="message-label">{{ message.role === 'user' ? '我' : 'AI 助手' }}</div>
          <div class="message-bubble">{{ message.content }}</div>
          <div v-if="message.role === 'assistant'" class="ai-generated-label">本内容由 AI 生成</div>
          <template v-if="message.response">
            <div v-if="message.response.dataSources?.length" class="source-list">
              <div v-for="source in message.response.dataSources" :key="source.tool" class="data-source" :class="`source-${source.status.toLowerCase()}`">
                <div class="source-heading">
                  <strong>{{ source.title }}</strong>
                  <span>{{ source.status === 'OK' ? '已查询' : source.status === 'DENIED' ? '无权限' : source.status === 'NOT_FOUND' ? '未找到' : '暂不可用' }}</span>
                </div>
                <div v-if="Object.keys(source.facts || {}).length" class="source-facts">
                  <span v-for="(value, key) in source.facts" :key="key">{{ key }}：{{ value }}</span>
                </div>
                <small>{{ source.dataRange }} · {{ source.queriedAt }}</small>
              </div>
            </div>
            <div v-if="message.response.citations?.length" class="citation-list">
              <span>参考：</span>
              <span v-for="citation in message.response.citations" :key="citation.id" class="citation">{{ citation.title }}</span>
            </div>
          </template>
        </article>

        <div v-if="sending" class="message assistant">
          <div class="message-label">AI 助手</div>
          <div class="message-bubble loading"><el-icon class="is-loading"><Loading /></el-icon> 正在分析</div>
        </div>
      </main>

      <footer class="assistant-composer">
        <el-input
          v-model="question"
          type="textarea"
          :autosize="{ minRows: 2, maxRows: 4 }"
          maxlength="1000"
          show-word-limit
          resize="none"
          placeholder="输入系统操作或已授权的数据查询问题"
          :disabled="sending"
          @keydown.enter.exact.prevent="sendQuestion"
        />
        <el-button type="primary" circle :loading="sending" :disabled="!question.trim()" aria-label="发送问题" @click="sendQuestion">
          <el-icon v-if="!sending"><Position /></el-icon>
        </el-button>
      </footer>
      <div class="assistant-notice">AI 回答仅作辅助参考，不执行系统操作。</div>
    </section>

    <el-dialog v-model="consentDialogVisible" title="确认用户协议与隐私政策" width="520px" append-to-body :close-on-click-modal="false">
      <p class="consent-intro">使用后台 AI 助手前，请阅读并确认平台用户协议、隐私政策、AI 使用范围、第三方模型数据处理方式、审计留存期限和投诉举报渠道。</p>
      <el-checkbox v-model="consentChecked">
        我已阅读并同意
        <button type="button" class="dialog-policy-link" @click.stop="policyDialogVisible = true">《用户协议》和《隐私政策》</button>
      </el-checkbox>
      <template #footer>
        <el-button @click="consentDialogVisible = false">暂不使用</el-button>
        <el-button type="primary" :disabled="!consentChecked" :loading="confirmingConsent" @click="confirmConsent">确认并打开 AI 助手</el-button>
      </template>
    </el-dialog>

    <AiAssistantPolicyDialog v-model="policyDialogVisible" />

    <el-dialog v-model="complaintDialogVisible" title="投诉或举报 AI 回答" width="480px" append-to-body>
      <el-alert title="提交内容仅用于平台人工处理，不会发送给 AI 模型。预计在 3 日内反馈处理结果。" type="info" :closable="false" show-icon />
      <el-form class="complaint-form" label-position="top">
        <el-form-item label="问题类型" required>
          <el-select v-model="complaintForm.category" class="complaint-full-width">
            <el-option label="回答内容不准确" value="CONTENT_QUALITY" />
            <el-option label="数据或权限问题" value="DATA_ISSUE" />
            <el-option label="安全或隐私风险" value="SECURITY_PRIVACY" />
            <el-option label="违规使用举报" value="MISUSE_REPORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联对话请求标识">
          <el-input v-model="complaintForm.requestId" maxlength="64" placeholder="自动带入最近一次 AI 回答，可按需修改" clearable />
        </el-form-item>
        <el-form-item label="说明" required>
          <el-input v-model="complaintForm.content" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="请说明问题、影响和期望处理方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingComplaint" @click="submitComplaint">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatWithAdminAssistant, clearAdminAssistantConversation, type AdminAssistantChatResponse } from '@/api/ai'
import { acceptAiAssistantConsent, createAiAssistantComplaint, getAiAssistantConsentStatus, type AiAssistantComplaintCategory } from '@/api/system'
import AiAssistantPolicyDialog from '@/components/AiAssistantPolicyDialog.vue'
import { AI_ASSISTANT_POLICY_VERSION } from '@/constants/aiAssistantPolicy'

interface AssistantMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  response?: AdminAssistantChatResponse
}

const opened = ref(false)
const question = ref('')
const sending = ref(false)
const clearingConversation = ref(false)
const messages = ref<AssistantMessage[]>([])
const conversationId = ref<string | undefined>(undefined)
const messageContainer = ref<HTMLElement>()
const complaintDialogVisible = ref(false)
const consentDialogVisible = ref(false)
const policyDialogVisible = ref(false)
const consentChecked = ref(false)
const confirmingConsent = ref(false)
const submittingComplaint = ref(false)
const complaintForm = reactive<{ category: AiAssistantComplaintCategory; requestId: string; content: string }>({
  category: 'CONTENT_QUALITY',
  requestId: '',
  content: ''
})

const suggestions = [
  '设备历史曲线的统计间隔如何选择？',
  '工单从报修到完成的流程是什么？',
  '如何导出某台设备的历史数据？',
  '退款中订单为什么没有变成已退款？'
]

async function openPanel() {
  try {
    const result = await getAiAssistantConsentStatus()
    if (result.data.accepted && result.data.policyVersion === AI_ASSISTANT_POLICY_VERSION) {
      opened.value = true
      scrollToBottom()
      return
    }
    consentChecked.value = false
    consentDialogVisible.value = true
  } catch (error) {
    console.error('load AI consent status failed', error)
    ElMessage.warning('暂时无法确认用户协议与隐私政策状态，请稍后重试。')
  }
}

async function confirmConsent() {
  if (!consentChecked.value || confirmingConsent.value) return
  confirmingConsent.value = true
  try {
    await acceptAiAssistantConsent({ policyVersion: AI_ASSISTANT_POLICY_VERSION, source: 'ASSISTANT_PANEL' })
    consentDialogVisible.value = false
    opened.value = true
    scrollToBottom()
  } finally {
    confirmingConsent.value = false
  }
}

async function clearConversation() {
  if (sending.value || clearingConversation.value) return
  const activeConversationId = conversationId.value
  clearingConversation.value = true
  try {
    if (activeConversationId) {
      await clearAdminAssistantConversation(activeConversationId)
    }
    messages.value = []
    question.value = ''
    conversationId.value = undefined
    ElMessage.success('本次对话已清空')
  } catch (error) {
    console.error('clear AI conversation failed', error)
    ElMessage.warning('服务端会话清理失败，请稍后重试。')
  } finally {
    clearingConversation.value = false
  }
}

function openComplaintDialog() {
  const latestAssistantMessage = [...messages.value].reverse()
    .find((message) => message.role === 'assistant' && message.response?.requestId)
  complaintForm.category = 'CONTENT_QUALITY'
  complaintForm.requestId = latestAssistantMessage?.response?.requestId || ''
  complaintForm.content = ''
  complaintDialogVisible.value = true
}

async function submitComplaint() {
  const content = complaintForm.content.trim()
  if (!content) {
    ElMessage.warning('请填写投诉或举报说明')
    return
  }
  submittingComplaint.value = true
  try {
    const result = await createAiAssistantComplaint({
      category: complaintForm.category,
      requestId: complaintForm.requestId.trim() || undefined,
      content
    })
    complaintDialogVisible.value = false
    ElMessage.success(`已提交，受理编号：${result.data.complaintNo}`)
  } finally {
    submittingComplaint.value = false
  }
}

function askSuggestion(value: string) {
  question.value = value
  sendQuestion()
}

async function sendQuestion() {
  const content = question.value.trim()
  if (!content || sending.value) return

  messages.value.push({ id: Date.now(), role: 'user', content })
  question.value = ''
  sending.value = true
  scrollToBottom()

  try {
    const result = await chatWithAdminAssistant(content, conversationId.value)
    const response = result.data
    if (response.conversationId) {
      conversationId.value = response.conversationId
    }
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: response.answer || '暂未生成有效回答，请稍后重试。',
      response
    })
  } catch (error: any) {
    const rawMessage = String(error?.message || '')
    const message = rawMessage.includes('协议') || rawMessage.includes('隐私')
      ? '请先阅读并同意用户协议与隐私政策后再使用助手。'
      : rawMessage.includes('timeout') ? 'AI 助手响应超时，请稍后重试。' : 'AI 助手暂时不可用，请稍后重试。'
    messages.value.push({ id: Date.now() + 1, role: 'assistant', content: message })
    ElMessage.warning(message)
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

function scrollToBottom() {
  nextTick(() => {
    const container = messageContainer.value
    if (container) container.scrollTop = container.scrollHeight
  })
}
</script>

<style scoped lang="scss">
.ai-assistant {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2100;
}

.assistant-launcher {
  width: 52px;
  height: 52px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: #1677ff;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(22, 119, 255, 0.32);

  &:hover { background: #4096ff; }
}

.assistant-panel {
  width: min(420px, calc(100vw - 32px));
  height: min(640px, calc(100vh - 48px));
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #d9e1ec;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 14px 38px rgba(23, 43, 77, 0.22);
}

.assistant-header {
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px 0 16px;
  border-bottom: 1px solid #edf0f5;

  .assistant-title { display: flex; align-items: center; gap: 8px; color: #1f2d3d; font-weight: 700; }
  .assistant-title .el-icon { color: #1677ff; font-size: 19px; }
  .assistant-actions { display: flex; align-items: center; }
}

.assistant-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f6f8fb;
}

.assistant-welcome {
  padding: 22px 12px 14px;
  text-align: center;
  color: #24364b;

  .welcome-icon { width: 44px; height: 44px; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 10px; border-radius: 8px; background: #e8f2ff; color: #1677ff; font-size: 23px; }
  strong { display: block; font-size: 16px; }
  p { margin: 8px 0 0; color: #718096; font-size: 13px; line-height: 1.6; }
}

.suggestion-list { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.suggestion { min-height: 54px; padding: 8px 10px; border: 1px solid #dce8f8; border-radius: 6px; background: #fff; color: #35628e; font-size: 13px; line-height: 1.45; text-align: left; cursor: pointer; }
.suggestion:hover { border-color: #86bfff; color: #1677ff; }

.message { margin-bottom: 14px; }
.message-label { margin: 0 0 5px 4px; color: #8191a7; font-size: 12px; }
.message-bubble { width: fit-content; max-width: 94%; padding: 10px 12px; border-radius: 6px; background: #fff; color: #2c3e50; font-size: 14px; line-height: 1.65; white-space: pre-wrap; word-break: break-word; box-shadow: 0 1px 2px rgba(31, 45, 61, 0.06); }
.message.user { display: flex; flex-direction: column; align-items: flex-end; }
.message.user .message-label { margin-right: 4px; }
.message.user .message-bubble { background: #1677ff; color: #fff; }
.message-bubble.loading { color: #637489; }
.ai-generated-label { width: fit-content; margin: 6px 0 0 2px; padding: 2px 7px; border: 1px solid #d7e6f8; border-radius: 10px; background: #eff6ff; color: #4b78a8; font-size: 11px; line-height: 1.4; }

.source-list { display: grid; gap: 7px; margin-top: 8px; }
.data-source { padding: 9px 10px; border: 1px solid #dfe7f0; border-radius: 6px; background: #fff; }
.data-source.source-denied, .data-source.source-unavailable { border-color: #f3d6b0; background: #fffaf2; }
.data-source.source-not_found { border-color: #e1e6ec; background: #fafbfd; }
.source-heading { display: flex; justify-content: space-between; gap: 10px; color: #425466; font-size: 12px; }
.source-heading span { color: #7e8ea3; white-space: nowrap; }
.source-facts { display: flex; flex-wrap: wrap; gap: 4px 8px; margin-top: 7px; color: #516173; font-size: 12px; line-height: 1.5; }
.data-source small { display: block; margin-top: 7px; color: #9aa7b7; font-size: 11px; }
.citation-list { margin-top: 8px; color: #8795a8; font-size: 12px; line-height: 1.6; }
.citation { margin-right: 6px; color: #4b7eaf; }

.assistant-composer { display: grid; grid-template-columns: minmax(0, 1fr) 36px; align-items: end; gap: 8px; padding: 12px 12px 8px; border-top: 1px solid #edf0f5; background: #fff; }
.assistant-composer :deep(.el-textarea__inner) { padding-right: 10px; }
.assistant-composer .el-button { width: 36px; height: 36px; }
.assistant-notice { padding: 0 12px 10px; background: #fff; color: #9aa7b7; font-size: 11px; text-align: center; }
.assistant-complaint-button { min-width: auto; padding: 4px 6px; color: #53657a; font-size: 12px; }
.assistant-complaint-button:hover { color: #1677ff; }
.complaint-form { margin-top: 16px; }
.complaint-full-width { width: 100%; }
.consent-intro { margin: 0 0 16px; color: #53657a; line-height: 1.7; }
.dialog-policy-link { margin: 0; padding: 0; border: 0; background: transparent; color: #1677ff; cursor: pointer; font: inherit; }

@media (max-width: 640px) {
  .ai-assistant { right: 12px; bottom: 12px; }
  .assistant-panel { height: min(620px, calc(100vh - 24px)); }
}
</style>
