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
            <el-button text circle aria-label="清空本次对话" @click="clearConversation">
              <el-icon><Delete /></el-icon>
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
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chatWithAdminAssistant, type AdminAssistantChatResponse } from '@/api/ai'

interface AssistantMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  response?: AdminAssistantChatResponse
}

const opened = ref(false)
const question = ref('')
const sending = ref(false)
const messages = ref<AssistantMessage[]>([])
const messageContainer = ref<HTMLElement>()

const suggestions = [
  '设备历史曲线的统计间隔如何选择？',
  '工单从报修到完成的流程是什么？',
  '如何导出某台设备的历史数据？',
  '退款中订单为什么没有变成已退款？'
]

function openPanel() {
  opened.value = true
  scrollToBottom()
}

function clearConversation() {
  messages.value = []
  question.value = ''
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
    const result = await chatWithAdminAssistant(content)
    const response = result.data
    messages.value.push({
      id: Date.now() + 1,
      role: 'assistant',
      content: response.answer || '暂未生成有效回答，请稍后重试。',
      response
    })
  } catch (error: any) {
    const message = error?.message?.includes('timeout') ? 'AI 助手响应超时，请稍后重试。' : 'AI 助手暂时不可用，请稍后重试。'
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

@media (max-width: 640px) {
  .ai-assistant { right: 12px; bottom: 12px; }
  .assistant-panel { height: min(620px, calc(100vh - 24px)); }
}
</style>
