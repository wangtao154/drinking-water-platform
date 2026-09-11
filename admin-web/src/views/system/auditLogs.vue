<template>
  <div class="audit-logs-page">
    <el-card shadow="never" class="search-card">
      <el-tabs v-model="activeLogType" @tab-change="handleTypeChange">
        <el-tab-pane label="操作日志" name="operation">
          <el-form class="search-form" :inline="true" :model="operationSearch">
            <el-form-item label="调用人 ID"><el-input v-model="operationSearch.operatorId" placeholder="账户 ID" clearable /></el-form-item>
            <el-form-item label="操作"><el-input v-model="operationSearch.operation" placeholder="操作名称" clearable /></el-form-item>
            <el-form-item label="日期"><el-date-picker v-model="operationSearch.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="AI 助手审计" name="assistant">
          <el-form class="search-form" :inline="true" :model="assistantSearch">
            <el-form-item label="调用人 ID"><el-input v-model="assistantSearch.operatorId" placeholder="账户 ID" clearable /></el-form-item>
            <el-form-item label="模型"><el-input v-model="assistantSearch.modelName" placeholder="如 qwen3.7-plus" clearable /></el-form-item>
            <el-form-item label="结果状态">
              <el-select v-model="assistantSearch.resultStatus" placeholder="全部" clearable class="status-select">
                <el-option label="成功" value="SUCCESS" /><el-option label="本地降级" value="FALLBACK" />
                <el-option label="请求拒绝" value="REJECTED" /><el-option label="安全拦截" value="BLOCKED" />
                <el-option label="运行异常" value="ERROR" />
              </el-select>
            </el-form-item>
            <el-form-item label="日期"><el-date-picker v-model="assistantSearch.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
          </el-form>
          <div class="assistant-audit-toolbar">
            <div class="audit-notice">仅展示脱敏摘要、数据工具和结果状态，不保存完整提问或完整回答。</div>
            <el-button :loading="checkingIntegrity" @click="checkAssistantAuditIntegrity">校验完整性</el-button>
          </div>
          <el-alert v-if="assistantIntegrity" :type="assistantIntegrityType" :closable="false" show-icon class="integrity-result">
            <template #title>{{ assistantIntegrity.message }}</template>
            <template #default>
              已校验 {{ assistantIntegrity.checkedRecords }} 条，共 {{ assistantIntegrity.totalRecords }} 条；
              历史未封存 {{ assistantIntegrity.legacyUnsealedRecords }} 条。
              <span v-if="assistantIntegrity.firstProblemRecordId">首个异常记录：{{ assistantIntegrity.firstProblemRecordId }}。</span>
            </template>
          </el-alert>
        </el-tab-pane>
        <el-tab-pane label="AI 投诉与安全复核" name="complaint">
          <el-form class="search-form" :inline="true" :model="complaintSearch">
            <el-form-item label="受理编号/说明"><el-input v-model="complaintSearch.keyword" placeholder="受理编号、提交人或说明" clearable /></el-form-item>
            <el-form-item label="问题类型">
              <el-select v-model="complaintSearch.category" placeholder="全部" clearable class="status-select">
                <el-option label="回答内容不准确" value="CONTENT_QUALITY" /><el-option label="数据或权限问题" value="DATA_ISSUE" />
                <el-option label="安全或隐私风险" value="SECURITY_PRIVACY" /><el-option label="违规使用举报" value="MISUSE_REPORT" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select v-model="complaintSearch.status" placeholder="全部" clearable class="status-select">
                <el-option label="待处理" value="PENDING" /><el-option label="处理中" value="PROCESSING" />
                <el-option label="已解决" value="RESOLVED" /><el-option label="已驳回" value="REJECTED" />
              </el-select>
            </el-form-item>
            <el-form-item label="提交日期"><el-date-picker v-model="complaintSearch.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" /></el-form-item>
            <el-form-item><el-button type="primary" @click="handleSearch">搜索</el-button><el-button @click="handleReset">重置</el-button></el-form-item>
          </el-form>
          <div class="audit-notice">投诉、举报和系统自动拦截的安全复核仅用于人工处理，不会发送给 AI 模型；待处理记录的目标反馈时限为 3 日。</div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card>
      <el-table v-if="activeLogType === 'operation'" :data="operationRows" stripe border>
        <el-table-column prop="operatorName" label="用户名" min-width="120" />
        <el-table-column prop="operation" label="操作" min-width="160" />
        <el-table-column prop="requestMethod" label="方法" min-width="100" />
        <el-table-column prop="requestIp" label="IP" min-width="120" />
        <el-table-column prop="costTime" label="耗时(ms)" min-width="100" />
        <el-table-column prop="result" label="结果" min-width="90" />
        <el-table-column prop="createdAt" label="创建时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
      </el-table>

      <el-table v-else-if="activeLogType === 'assistant'" :data="assistantRows" stripe border>
        <el-table-column prop="operatorNameMasked" label="调用人" min-width="110" />
        <el-table-column prop="modelName" label="模型" min-width="130"><template #default="{ row }">{{ row.modelName || '本地降级' }}</template></el-table-column>
        <el-table-column prop="toolNames" label="数据工具" min-width="170"><template #default="{ row }">{{ row.toolNames || '-' }}</template></el-table-column>
        <el-table-column prop="resultStatus" label="结果状态" min-width="175"><template #default="{ row }"><el-space :size="6"><el-tag :type="assistantStatusType(row.resultStatus)">{{ assistantStatusLabel(row.resultStatus) }}</el-tag><el-tag v-if="isReviewBlocked(row)" type="danger" effect="dark">复核拦截</el-tag></el-space></template></el-table-column>
        <el-table-column prop="questionSummaryMasked" label="提问脱敏摘要" min-width="230" show-overflow-tooltip />
        <el-table-column prop="answerSummaryMasked" label="回答脱敏摘要" min-width="250" show-overflow-tooltip />
        <el-table-column prop="errorSummaryMasked" label="异常摘要" min-width="180" show-overflow-tooltip><template #default="{ row }">{{ row.errorSummaryMasked || '-' }}</template></el-table-column>
        <el-table-column prop="createdAt" label="调用时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column prop="retentionUntil" label="最早清理时间" min-width="170"><template #default="{ row }">{{ formatDateTime(row.retentionUntil) }}</template></el-table-column>
      </el-table>

      <el-table v-else :data="complaintRows" stripe border>
        <el-table-column prop="complaintNo" label="受理编号" min-width="190" />
        <el-table-column label="来源" min-width="110"><template #default="{ row }"><el-tag :type="isSafetyReview(row) ? 'warning' : 'info'">{{ isSafetyReview(row) ? '安全复核' : '用户提交' }}</el-tag></template></el-table-column>
        <el-table-column prop="reporterName" label="提交人" min-width="110" />
        <el-table-column prop="category" label="问题类型" min-width="140"><template #default="{ row }">{{ complaintCategoryLabel(row.category) }}</template></el-table-column>
        <el-table-column prop="content" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="requestId" label="关联请求标识" min-width="170" show-overflow-tooltip><template #default="{ row }">{{ row.requestId || '-' }}</template></el-table-column>
        <el-table-column prop="status" label="处理状态" min-width="100"><template #default="{ row }"><el-tag :type="complaintStatusType(row.status)">{{ complaintStatusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="replyDueAt" label="反馈截止" min-width="165"><template #default="{ row }">{{ formatDateTime(row.replyDueAt) }}</template></el-table-column>
        <el-table-column prop="handlerName" label="处理人" min-width="110"><template #default="{ row }">{{ row.handlerName || '-' }}</template></el-table-column>
        <el-table-column prop="handledAt" label="处理时间" min-width="165"><template #default="{ row }">{{ formatDateTime(row.handledAt) }}</template></el-table-column>
        <el-table-column label="操作" fixed="right" width="95"><template #default="{ row }"><el-button link type="primary" @click="openComplaintDialog(row)">处理</el-button></template></el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.pageNum" v-model:page-size="pagination.pageSize" :total="pagination.total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" class="pagination" @size-change="loadData" @current-change="loadData" />
    </el-card>

    <el-dialog v-model="complaintDialogVisible" title="处理 AI 投诉举报" width="580px" append-to-body>
      <el-descriptions v-if="selectedComplaint" :column="1" border class="complaint-detail">
        <el-descriptions-item label="受理编号">{{ selectedComplaint.complaintNo }}</el-descriptions-item>
        <el-descriptions-item label="问题类型">{{ complaintCategoryLabel(selectedComplaint.category) }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ isSafetyReview(selectedComplaint) ? '系统自动安全复核' : '用户投诉或举报' }}</el-descriptions-item>
        <el-descriptions-item label="提交说明">{{ selectedComplaint.content }}</el-descriptions-item>
        <el-descriptions-item label="关联请求">{{ selectedComplaint.requestId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="反馈截止">{{ formatDateTime(selectedComplaint.replyDueAt) }}</el-descriptions-item>
      </el-descriptions>
      <el-form class="handle-form" label-position="top">
        <el-form-item label="处理状态" required>
          <el-select v-model="complaintHandleForm.status" class="status-select">
            <el-option label="待处理" value="PENDING" /><el-option label="处理中" value="PROCESSING" />
            <el-option label="已解决" value="RESOLVED" /><el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理答复">
          <el-input v-model="complaintHandleForm.handleReply" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="解决或驳回时必须填写答复" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingComplaint" @click="saveComplaint">保存处理结果</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiAssistantAuditIntegrity, handleAiAssistantComplaint, pageAiAssistantAuditLogs, pageAiAssistantComplaints, pageAuditLogs } from '@/api/system'
import type { AiAssistantAuditIntegrityVO, AiAssistantAuditLogVO, AiAssistantComplaintStatus, AiAssistantComplaintVO, AuditLogVO } from '@/api/system'
import { formatDateTime } from '@/utils/format'

const activeLogType = ref<'operation' | 'assistant' | 'complaint'>('operation')
const operationRows = ref<AuditLogVO[]>([])
const assistantRows = ref<AiAssistantAuditLogVO[]>([])
const complaintRows = ref<AiAssistantComplaintVO[]>([])
const operationSearch = reactive({ operatorId: '', operation: '', dateRange: [] as string[] })
const assistantSearch = reactive({ operatorId: '', modelName: '', resultStatus: '', dateRange: [] as string[] })
const complaintSearch = reactive({ keyword: '', category: '', status: '', dateRange: [] as string[] })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const complaintDialogVisible = ref(false)
const savingComplaint = ref(false)
const selectedComplaint = ref<AiAssistantComplaintVO>()
const complaintHandleForm = reactive<{ status: AiAssistantComplaintStatus; handleReply: string }>({ status: 'PENDING', handleReply: '' })
const checkingIntegrity = ref(false)
const assistantIntegrity = ref<AiAssistantAuditIntegrityVO>()
const REVIEW_BLOCKED_REQUEST_IDS = new Set(['59057518-f911-436b-9e35-e83604f9ff5a'])

function parseOperatorId(value: string) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined
}

async function loadData() {
  try {
    if (activeLogType.value === 'operation') {
      const res = await pageAuditLogs({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, operatorId: parseOperatorId(operationSearch.operatorId), operation: operationSearch.operation || undefined, startTime: operationSearch.dateRange[0], endTime: operationSearch.dateRange[1] })
      operationRows.value = res.data.records
      pagination.total = res.data.total
      return
    }
    if (activeLogType.value === 'assistant') {
      const res = await pageAiAssistantAuditLogs({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, operatorId: parseOperatorId(assistantSearch.operatorId), modelName: assistantSearch.modelName || undefined, resultStatus: assistantSearch.resultStatus || undefined, startTime: assistantSearch.dateRange[0], endTime: assistantSearch.dateRange[1] })
      assistantRows.value = res.data.records
      pagination.total = res.data.total
      return
    }
    const res = await pageAiAssistantComplaints({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, keyword: complaintSearch.keyword || undefined, category: complaintSearch.category as any || undefined, status: complaintSearch.status as any || undefined, startTime: complaintSearch.dateRange[0], endTime: complaintSearch.dateRange[1] })
    complaintRows.value = res.data.records
    pagination.total = res.data.total
  } catch {
    ElMessage.error(activeLogType.value === 'complaint' ? '加载 AI 投诉举报失败' : '加载审计日志失败')
  }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleTypeChange() { pagination.pageNum = 1; pagination.total = 0; loadData() }
const assistantIntegrityType = computed(() => {
  if (assistantIntegrity.value?.status === 'VERIFIED') return 'success'
  if (assistantIntegrity.value?.status === 'LEGACY_UNSEALED') return 'warning'
  return 'error'
})
async function checkAssistantAuditIntegrity() {
  checkingIntegrity.value = true
  try {
    const res = await getAiAssistantAuditIntegrity()
    assistantIntegrity.value = res.data
    if (res.data.status === 'FAILED') {
      ElMessage.error('AI 审计完整性校验失败，请按操作手册处理')
    } else {
      ElMessage.success('AI 审计完整性校验完成')
    }
  } finally {
    checkingIntegrity.value = false
  }
}
function handleReset() {
  if (activeLogType.value === 'operation') {
    operationSearch.operatorId = ''; operationSearch.operation = ''; operationSearch.dateRange = []
  } else if (activeLogType.value === 'assistant') {
    assistantSearch.operatorId = ''; assistantSearch.modelName = ''; assistantSearch.resultStatus = ''; assistantSearch.dateRange = []
  } else {
    complaintSearch.keyword = ''; complaintSearch.category = ''; complaintSearch.status = ''; complaintSearch.dateRange = []
  }
  pagination.pageNum = 1
  loadData()
}
function assistantStatusLabel(status?: string) {
  return ({ SUCCESS: '成功', FALLBACK: '本地降级', REJECTED: '请求拒绝', BLOCKED: '安全拦截', ERROR: '运行异常' } as Record<string, string>)[status || ''] || status || '-'
}
function assistantStatusType(status?: string) {
  return ({ SUCCESS: 'success', FALLBACK: 'warning', REJECTED: 'info', BLOCKED: 'warning', ERROR: 'danger' } as Record<string, 'success' | 'warning' | 'info' | 'danger'>)[status || ''] || 'info'
}
function isReviewBlocked(row?: AiAssistantAuditLogVO) {
  return Boolean(row?.requestId && REVIEW_BLOCKED_REQUEST_IDS.has(row.requestId))
}
function isSafetyReview(row?: AiAssistantComplaintVO) { return Boolean(row?.content?.startsWith('系统内容安全自动拦截')) }
function complaintCategoryLabel(category?: string) {
  return ({ CONTENT_QUALITY: '回答内容不准确', DATA_ISSUE: '数据或权限问题', SECURITY_PRIVACY: '安全或隐私风险', MISUSE_REPORT: '违规使用举报' } as Record<string, string>)[category || ''] || category || '-'
}
function complaintStatusLabel(status?: string) {
  return ({ PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', REJECTED: '已驳回' } as Record<string, string>)[status || ''] || status || '-'
}
function complaintStatusType(status?: string) {
  return ({ PENDING: 'warning', PROCESSING: 'primary', RESOLVED: 'success', REJECTED: 'info' } as Record<string, 'success' | 'warning' | 'info' | 'primary'>)[status || ''] || 'info'
}
function openComplaintDialog(row: AiAssistantComplaintVO) {
  selectedComplaint.value = row
  complaintHandleForm.status = row.status
  complaintHandleForm.handleReply = row.handleReply || ''
  complaintDialogVisible.value = true
}
async function saveComplaint() {
  if (!selectedComplaint.value) return
  if ((complaintHandleForm.status === 'RESOLVED' || complaintHandleForm.status === 'REJECTED') && !complaintHandleForm.handleReply.trim()) {
    ElMessage.warning('解决或驳回投诉时必须填写处理答复')
    return
  }
  savingComplaint.value = true
  try {
    await handleAiAssistantComplaint(selectedComplaint.value.id, { status: complaintHandleForm.status, handleReply: complaintHandleForm.handleReply.trim() || undefined })
    ElMessage.success('处理结果已保存')
    complaintDialogVisible.value = false
    loadData()
  } finally {
    savingComplaint.value = false
  }
}
onMounted(loadData)
</script>

<style scoped lang="scss">
.audit-logs-page { padding: 16px; }
.search-card { margin-bottom: 12px; }
.search-form { padding-top: 4px; }
.status-select { width: 130px; }
.assistant-audit-toolbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.audit-notice { margin: -6px 0 8px; color: #909399; font-size: 13px; }
.integrity-result { margin: 0 0 12px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.complaint-detail { margin-bottom: 16px; }
.handle-form { margin-top: 12px; }
</style>
