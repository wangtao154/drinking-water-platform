<template>
  <div class="refunds-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="退款状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待审批" value="PENDING" />
            <el-option label="已批准" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" stripe border>
        <el-table-column prop="refundNo" label="退款号" min-width="140" />
        <el-table-column prop="orderNo" label="订单号" min-width="140" />
        <el-table-column prop="amount" label="退款金额" min-width="100">
          <template #default="{ row }">{{ fenToYuan(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="140" />
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="approver" label="审批人" min-width="100" />
        <el-table-column prop="approvedAt" label="审批时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.approvedAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.status === 'PENDING'"
              title="确认批准该退款？"
              @confirm="handleApprove(row)"
            >
              <template #reference>
                <el-button type="success" link size="small">批准</el-button>
              </template>
            </el-popconfirm>
            <el-button
              v-if="row.status === 'PENDING'"
              type="danger"
              link
              size="small"
              @click="openRejectDialog(row)"
            >拒绝</el-button>
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

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="showRejectDialog" title="拒绝退款" width="500px" @close="rejectReason = ''">
      <el-form label-width="100px">
        <el-form-item label="拒绝原因">
          <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRejectDialog = false">取消</el-button>
        <el-button type="danger" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageRefunds, approveRefund, rejectRefund } from '@/api/payment'
import type { RefundVO } from '@/api/payment'
import { formatDateTime, statusLabel, statusTagType, fenToYuan } from '@/utils/format'

const tableData = ref<RefundVO[]>([])
const showRejectDialog = ref(false)
const rejectReason = ref('')
const currentRow = ref<RefundVO | null>(null)

const searchForm = reactive({ status: '', orderNo: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadData() {
  try {
    const res = await pageRefunds({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      status: searchForm.status || undefined,
      orderNo: searchForm.orderNo || undefined,
    })
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
  searchForm.orderNo = ''
  pagination.pageNum = 1
  loadData()
}

async function handleApprove(row: RefundVO) {
  try {
    await approveRefund(row.id)
    ElMessage.success('批准成功')
    loadData()
  } catch (e) {
    ElMessage.error('批准失败')
  }
}

function openRejectDialog(row: RefundVO) {
  currentRow.value = row
  rejectReason.value = ''
  showRejectDialog.value = true
}

async function handleReject() {
  if (!rejectReason.value) {
    ElMessage.warning('请输入拒绝原因')
    return
  }
  try {
    await rejectRefund(currentRow.value!.id, { reason: rejectReason.value })
    ElMessage.success('拒绝成功')
    showRejectDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('拒绝失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.refunds-page {
  padding: 16px;
}
.search-card {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
