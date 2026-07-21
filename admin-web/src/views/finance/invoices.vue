<template>
  <div class="invoices-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="待开票" value="PENDING" />
            <el-option label="已开票" value="ISSUED" />
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
      <el-button type="primary" @click="showCreateDialog = true">开发票</el-button>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" stripe border>
        <el-table-column prop="invoiceNo" label="发票号" min-width="140" />
        <el-table-column prop="title" label="抬头" min-width="140" />
        <el-table-column prop="taxNo" label="税号" min-width="140" />
        <el-table-column prop="amount" label="金额" min-width="100">
          <template #default="{ row }">{{ fenToYuan(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ISSUED' ? 'success' : 'warning'">
              {{ row.status === 'ISSUED' ? '已开票' : '待开票' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="issuedAt" label="开票日期" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.issuedAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.status === 'PENDING'"
              title="确认开票？"
              @confirm="handleIssue(row)"
            >
              <template #reference>
                <el-button type="primary" link size="small">开票</el-button>
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
    <el-dialog v-model="showCreateDialog" title="开发票" width="500px" @close="resetCreateForm">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="类型" prop="type">
          <el-select v-model="createForm.type" placeholder="请选择类型">
            <el-option label="个人" value="INDIVIDUAL" />
            <el-option label="企业" value="ENTERPRISE" />
          </el-select>
        </el-form-item>
        <el-form-item label="抬头" prop="title">
          <el-input v-model="createForm.title" placeholder="请输入抬头" />
        </el-form-item>
        <el-form-item label="税号" prop="taxNo">
          <el-input v-model="createForm.taxNo" placeholder="选填" />
        </el-form-item>
        <el-form-item label="金额(元)" prop="amountYuan">
          <el-input-number v-model="createForm.amountYuan" :min="0.01" :precision="2" :step="1" />
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
import { pageInvoices, createInvoice, issueInvoice } from '@/api/finance'
import type { InvoiceVO } from '@/api/finance'
import { formatDateTime, fenToYuan } from '@/utils/format'

const tableData = ref<InvoiceVO[]>([])
const showCreateDialog = ref(false)

const searchForm = reactive({ status: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const createForm = reactive({ type: '', title: '', taxNo: '', amountYuan: 0 })

async function loadData() {
  try {
    const res = await pageInvoices({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, status: searchForm.status || undefined })
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

async function handleIssue(row: InvoiceVO) {
  try {
    await issueInvoice(row.invoiceNo)
    ElMessage.success('开票成功')
    loadData()
  } catch (e) {
    ElMessage.error('开票失败')
  }
}

function resetCreateForm() {
  createForm.type = ''
  createForm.title = ''
  createForm.taxNo = ''
  createForm.amountYuan = 0
}

async function handleCreate() {
  try {
    const amountFen = Math.round(createForm.amountYuan * 100)
    await createInvoice({ type: createForm.type, title: createForm.title, taxNo: createForm.taxNo, amount: amountFen })
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    loadData()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.invoices-page {
  padding: 16px;
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
