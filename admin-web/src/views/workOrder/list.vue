<template>
  <div class="work-order-list-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">总工单</div>
          <div class="stat-value" style="color: #409eff">{{ statistics.total }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">待处理</div>
          <div class="stat-value" style="color: #e6a23c">{{ statistics.pending }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">进行中</div>
          <div class="stat-value" style="color: #e6a23c">{{ statistics.accepted + statistics.inProgress }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">已完成</div>
          <div class="stat-value" style="color: #67c23a">{{ statistics.completed }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="工单号">
          <el-input v-model="searchForm.keyword" placeholder="请输入工单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.orderStatus" placeholder="全部" clearable style="width: 140px">
            <el-option label="待处理" value="PENDING" />
            <el-option label="已派单" value="ASSIGNED" />
            <el-option label="已接单" value="ACCEPTED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="待核验" value="COMPLETED" />
            <el-option label="已完成" value="VERIFIED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="searchForm.orderType" placeholder="全部" clearable style="width: 140px">
            <el-option label="维修" value="REPAIR" />
            <el-option label="安装预约" value="INSTALL_APPOINTMENT" />
            <el-option label="退机" value="REMOVE" />
            <el-option label="移机" value="RELOCATE" />
            <el-option label="滤芯更换" value="FILTER_REPLACE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%">
        <el-table-column prop="orderNo" label="工单号" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ workOrderTypeLabel(row.orderType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.orderStatus)" size="small">
              {{ workOrderStatusLabel(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户" min-width="100" show-overflow-tooltip />
        <el-table-column prop="customerPhone" label="电话" min-width="120" show-overflow-tooltip />
        <el-table-column prop="deviceSn" label="设备SN" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deviceSn || '-' }}</template>
        </el-table-column>
        <el-table-column prop="workerName" label="运维人员" min-width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.workerName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="创建时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button v-if="row.orderStatus === 'PENDING'" type="warning" link size="small" @click="handleDispatch(row)">派单</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="searchForm.pageNum" v-model:page-size="searchForm.pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper" background @current-change="fetchData" @size-change="fetchData" />
      </div>
    </el-card>

    <!-- 派单弹窗 -->
    <el-dialog v-model="dispatchVisible" title="派单" width="450px" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="工单号"><span>{{ currentRow?.orderNo }}</span></el-form-item>
        <el-form-item label="描述"><span>{{ currentRow?.description }}</span></el-form-item>
        <el-form-item label="运维人员">
          <el-select v-model="selectedWorkerId" placeholder="请选择运维人员" filterable style="width: 100%">
            <el-option v-for="w in workerList" :key="w.id" :label="`${w.name} (${w.phone})`" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dispatchVisible = false">取消</el-button>
        <el-button type="primary" :loading="dispatchLoading" @click="handleDispatchSubmit">确认派单</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="700px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单号">{{ detailData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.orderStatus)" size="small">{{ workOrderStatusLabel(detailData.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="类型">{{ workOrderTypeLabel(detailData.orderType) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detailData.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detailData.customerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detailData.customerPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备SN">{{ detailData.deviceSn || '-' }}</el-descriptions-item>
          <el-descriptions-item label="运维人员">{{ detailData.workerName || '-' }}{{ detailData.workerPhone ? ' (' + detailData.workerPhone + ')' : '' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ detailData.completedAt ? formatDateTime(detailData.completedAt) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ detailData.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ detailData.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 客户提交图片 -->
        <div v-if="detailData.customerImages && detailData.customerImages.length > 0" class="img-section">
          <div class="img-title">客户提交图片</div>
          <div class="img-list">
            <el-image v-for="(url, i) in detailData.customerImages" :key="i" :src="url" :preview-src-list="detailData.customerImages" fit="cover" class="img-item" />
          </div>
        </div>

        <!-- 运维完成报告 -->
        <div v-if="detailData.remark || (detailData.photoUrls && detailData.photoUrls.length > 0)" class="img-section">
          <div class="img-title">运维完成报告</div>
          <div v-if="detailData.remark" class="report-text">{{ detailData.remark }}</div>
          <div v-if="detailData.photoUrls && detailData.photoUrls.length > 0" class="img-list">
            <el-image v-for="(url, i) in detailData.photoUrls" :key="i" :src="url" :preview-src-list="detailData.photoUrls" fit="cover" class="img-item" />
          </div>
        </div>

        <!-- 客户核查反馈 -->
        <div v-if="detailData.reviewContent || detailData.rating" class="img-section">
          <div class="img-title">客户核查反馈</div>
          <div v-if="detailData.rating" class="report-text">
            评分：<el-rate v-model="detailData.rating" disabled />
          </div>
          <div v-if="detailData.reviewContent" class="report-text">{{ detailData.reviewContent }}</div>
        </div>

        <!-- 状态流转时间线 -->
        <div v-if="detailData.statusLogs && detailData.statusLogs.length > 0" class="img-section">
          <div class="img-title">状态流转记录</div>
          <el-table :data="detailData.statusLogs" border size="small" style="width: 100%">
            <el-table-column type="index" label="序号" width="60" align="center" />
            <el-table-column label="状态变更" min-width="160">
              <template #default="{ row }">
                <span v-if="row.fromStatusDesc">{{ row.fromStatusDesc }}</span>
                <span v-if="row.fromStatusDesc" style="color: #909399; margin: 0 4px;">→</span>
                <el-tag :type="statusTagType(row.toStatus)" size="small">{{ row.toStatusDesc || row.toStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operatorName" label="操作人" width="100" show-overflow-tooltip />
            <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
            <el-table-column label="时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageWorkOrders, getWorkOrderStatistics, dispatchWorkOrder, deleteWorkOrder, getWorkOrderDetail } from '@/api/workOrder'
import { pageWorkers } from '@/api/user'
import type { WorkOrderVO, WorkOrderStatisticsVO, WorkerVO } from '@/types/api'
import { formatDateTime, statusTagType } from '@/utils/format'

const loading = ref(false)
const tableData = ref<WorkOrderVO[]>([])
const total = ref(0)
const statistics = reactive<WorkOrderStatisticsVO>({ total: 0, pending: 0, dispatched: 0, accepted: 0, inProgress: 0, completed: 0, cancelled: 0 })
const searchForm = reactive({ keyword: '', orderStatus: '', orderType: '', pageNum: 1, pageSize: 10 })

async function fetchStatistics() {
  try {
    const res = await getWorkOrderStatistics()
    if (res.code === 200) Object.assign(statistics, res.data)
  } catch { /* ignore */ }
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { pageNum: searchForm.pageNum, pageSize: searchForm.pageSize }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.orderStatus) params.orderStatus = searchForm.orderStatus
    if (searchForm.orderType) params.orderType = searchForm.orderType
    const res = await pageWorkOrders(params as any)
    if (res.code === 200) { tableData.value = res.data.records; total.value = res.data.total }
  } finally { loading.value = false }
}

function handleSearch() { searchForm.pageNum = 1; fetchData() }

function workOrderStatusLabel(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待处理',
    ASSIGNED: '已派单',
    ACCEPTED: '已接单',
    IN_PROGRESS: '进行中',
    COMPLETED: '待核验',
    VERIFIED: '已完成',
    CANCELLED: '已取消'
  }
  return map[status] || status
}
function workOrderTypeLabel(type: string): string {
  const map: Record<string, string> = {
    REPAIR: '维修',
    FILTER_REPLACE: '滤芯更换',
    INSTALL_APPOINTMENT: '安装预约',
    REMOVE: '退机',
    RELOCATE: '移机'
  }
  return map[type] || type
}

function handleReset() { searchForm.keyword = ''; searchForm.orderStatus = ''; searchForm.orderType = ''; searchForm.pageNum = 1; fetchData() }

// ===== 派单 =====
const dispatchVisible = ref(false)
const dispatchLoading = ref(false)
const currentRow = ref<WorkOrderVO | null>(null)
const selectedWorkerId = ref<string | undefined>(undefined)
const workerList = ref<WorkerVO[]>([])

async function fetchWorkers() {
  try {
    const res = await pageWorkers({ pageNum: 1, pageSize: 100 })
    if (res.code === 200) workerList.value = res.data.records
  } catch { /* ignore */ }
}

function handleDispatch(row: WorkOrderVO) {
  currentRow.value = row
  selectedWorkerId.value = undefined
  dispatchVisible.value = true
}

async function handleDispatchSubmit() {
  if (!currentRow.value || !selectedWorkerId.value) { ElMessage.warning('请选择运维人员'); return }
  dispatchLoading.value = true
  try {
    const res = await dispatchWorkOrder(currentRow.value.id, selectedWorkerId.value)
    if (res.code === 200) { ElMessage.success('派单成功'); dispatchVisible.value = false; fetchData(); fetchStatistics() }
  } finally { dispatchLoading.value = false }
}

// ===== 详情 =====
const detailVisible = ref(false)
const detailData = ref<WorkOrderVO | null>(null)

async function handleDetail(row: WorkOrderVO) {
  detailVisible.value = true
  detailData.value = null
  try {
    const res = await getWorkOrderDetail(row.id)
    if (res.code === 200) detailData.value = res.data
  } catch { /* ignore */ }
}

// ===== 删除 =====
async function handleDelete(id: number) {
  try {
    const res = await deleteWorkOrder(id)
    if (res.code === 200) { ElMessage.success('删除成功'); fetchData(); fetchStatistics() }
  } catch { /* ignore */ }
}

onMounted(() => { fetchData(); fetchStatistics(); fetchWorkers() })
</script>

<style scoped lang="scss">
.work-order-list-page {
  padding: 16px;
  .stat-row { margin-bottom: 16px; .stat-card { .stat-title { font-size: 13px; color: #909399; margin-bottom: 8px; } .stat-value { font-size: 28px; font-weight: 700; } } }
  .search-card { margin-bottom: 16px; }
  .table-card { .pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; } }
  .img-section { margin-top: 20px; .img-title { font-weight: 600; font-size: 14px; margin-bottom: 10px; color: #303133; } .img-list { display: flex; flex-wrap: wrap; gap: 10px; } .img-item { width: 100px; height: 100px; border-radius: 6px; } .report-text { font-size: 14px; color: #606266; margin-bottom: 10px; line-height: 1.6; } }
}
</style>
