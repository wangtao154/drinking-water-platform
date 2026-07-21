<template>
  <div class="settlements-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">总营收</div>
            <div class="stat-value">{{ fenToYuan(summary.totalRevenue) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">分佣总额</div>
            <div class="stat-value">{{ fenToYuan(summary.totalCommission) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">已结算</div>
            <div class="stat-value">{{ fenToYuan(summary.settledCommission) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">待结算</div>
            <div class="stat-value">{{ fenToYuan(summary.pendingCommission) }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="已结算" value="SETTLED" />
            <el-option label="待结算" value="PENDING" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 工具栏 -->
    <el-card class="toolbar-card">
      <el-button type="primary" @click="showCreateDialog = true">创建结算账单</el-button>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" stripe border>
        <el-table-column prop="billNo" label="账单号" min-width="140" />
        <el-table-column prop="dealerName" label="经销商名" min-width="120" />
        <el-table-column prop="totalAmount" label="总金额" min-width="100">
          <template #default="{ row }">{{ fenToYuan(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="commissionAmount" label="佣金金额" min-width="100">
          <template #default="{ row }">{{ fenToYuan(row.commissionAmount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SETTLED' ? 'success' : 'warning'">
              {{ row.status === 'SETTLED' ? '已结算' : '待结算' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="billStartDate" label="周期开始" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.billStartDate) }}</template>
        </el-table-column>
        <el-table-column prop="billEndDate" label="周期结束" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.billEndDate) }}</template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.status === 'PENDING'"
              title="确认结算该账单？"
              @confirm="handleSettle(row)"
            >
              <template #reference>
                <el-button type="primary" link size="small">结算</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增弹窗 -->
    <el-dialog v-model="showCreateDialog" title="创建结算账单" width="500px" @close="resetCreateForm">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="经销商ID" prop="dealerId">
          <el-input v-model="createForm.dealerId" placeholder="请输入经销商ID" />
        </el-form-item>
        <el-form-item label="周期开始日期" prop="billStartDate">
          <el-date-picker v-model="createForm.billStartDate" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="周期结束日期" prop="billEndDate">
          <el-date-picker v-model="createForm.billEndDate" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getFinanceSummary, pageSettlements, createSettlement, settleSettlement } from '@/api/finance'
import type { FinanceSummaryVO, SettlementVO } from '@/api/finance'
import { formatDateTime, fenToYuan } from '@/utils/format'

const summary = ref<FinanceSummaryVO>({} as FinanceSummaryVO)
const tableData = ref<SettlementVO[]>([])
const showCreateDialog = ref(false)

const searchForm = reactive({ status: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const createForm = reactive({ dealerId: '', billStartDate: '', billEndDate: '' })

async function loadSummary() {
  try {
    const res = await getFinanceSummary()
    summary.value = res.data
  } catch (e) {
    ElMessage.error('加载统计失败')
  }
}

async function loadData() {
  try {
    const res = await pageSettlements({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, status: searchForm.status || undefined })
    tableData.value = res.data.records
    pagination.total = res.data.total
  } catch (e) {
    ElMessage.error('加载列表失败')
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadData()
}

function handleReset() {
  searchForm.status = ''
  pagination.pageNum = 1
  loadData()
}

async function handleSettle(row: SettlementVO) {
  try {
    await settleSettlement(row.billNo)
    ElMessage.success('结算成功')
    loadData()
    loadSummary()
  } catch (e) {
    ElMessage.error('结算失败')
  }
}

function resetCreateForm() {
  createForm.dealerId = ''
  createForm.billStartDate = ''
  createForm.billEndDate = ''
}

async function handleCreate() {
  try {
    await createSettlement(createForm)
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    loadData()
    loadSummary()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

onMounted(() => {
  loadSummary()
  loadData()
})
</script>

<style scoped lang="scss">
.settlements-page {
  padding: 16px;
}
.summary-row {
  margin-bottom: 16px;
}
.stat-item {
  text-align: center;
  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-bottom: 8px;
  }
  .stat-value {
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }
}
.search-card,
.toolbar-card {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
