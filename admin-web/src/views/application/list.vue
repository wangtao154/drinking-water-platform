<template>
  <div class="application-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :model="searchForm" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="姓名 / 手机号"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="申请类型">
          <el-select v-model="searchForm.applyType" placeholder="全部" clearable style="width: 120px">
            <el-option label="正式客户" value="CUSTOMER" />
            <el-option label="运维人员" value="WORKER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
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
        <el-table-column prop="name" label="姓名" min-width="100" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column label="申请类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.applyType === 'WORKER' ? 'warning' : 'info'" size="small">
              {{ row.applyType === 'WORKER' ? '运维人员' : '正式客户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dealerName" label="所属经销商" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.dealerName || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              link
              size="small"
              @click="handleReview(row)"
            >
              审批
            </el-button>
            <el-popconfirm
              title="确定要删除该申请吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="申请详情" width="600px">
      <el-descriptions :column="2" border v-if="currentApp">
        <el-descriptions-item label="姓名">{{ currentApp.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentApp.phone }}</el-descriptions-item>
        <el-descriptions-item label="申请类型">
          {{ currentApp.applyType === 'WORKER' ? '运维人员' : '正式客户' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentApp.status)" size="small">
            {{ statusText(currentApp.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ currentApp.idCard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户类型">
          {{ customerTypeText(currentApp.customerType) }}
        </el-descriptions-item>
        <el-descriptions-item label="单位名称">{{ currentApp.orgName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="信用代码">{{ currentApp.creditCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属经销商">{{ currentApp.dealerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址">
          {{ [currentApp.province, currentApp.city, currentApp.district, currentApp.address].filter(Boolean).join(' ') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ formatDateTime(currentApp.createdAt) }}</el-descriptions-item>
        <el-descriptions-item v-if="currentApp.reviewedAt" label="审批时间" :span="2">
          {{ formatDateTime(currentApp.reviewedAt) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentApp.reviewComment" label="审批意见" :span="2">
          {{ currentApp.reviewComment }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 审批弹窗 -->
    <el-dialog v-model="reviewVisible" title="审批申请" width="480px" :close-on-click-modal="false">
      <div v-if="currentApp" style="margin-bottom: 16px">
        <p><strong>{{ currentApp.name }}</strong> 申请成为
          <el-tag :type="currentApp.applyType === 'WORKER' ? 'warning' : 'info'" size="small">
            {{ currentApp.applyType === 'WORKER' ? '运维人员' : '正式客户' }}
          </el-tag>
        </p>
        <p style="color: #999; font-size: 13px">手机号：{{ currentApp.phone }}</p>
      </div>
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="reviewForm.approved">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model="reviewForm.reviewComment"
            type="textarea"
            :rows="3"
            placeholder="选填"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewLoading" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageApplications, reviewApplication, deleteApplication } from '@/api/user'
import { formatDateTime } from '@/utils/format'

interface ApplicationVO {
  id: number
  applicantId: number
  applyType: string
  name: string
  phone: string
  idCard?: string
  province?: string
  city?: string
  district?: string
  address?: string
  customerType?: string
  orgName?: string
  creditCode?: string
  dealerId?: number
  dealerName?: string
  status: string
  reviewComment?: string
  reviewedAt?: string
  createdAt: string
}

const loading = ref(false)
const tableData = ref<ApplicationVO[]>([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  applyType: '',
  status: '',
  page: 1,
  size: 10,
})

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: searchForm.page,
      size: searchForm.size,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.applyType) params.applyType = searchForm.applyType
    if (searchForm.status) params.status = searchForm.status

    const res = await pageApplications(params)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchForm.page = 1
  loadData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.applyType = ''
  searchForm.status = ''
  searchForm.page = 1
  loadData()
}

// 详情弹窗
const detailVisible = ref(false)
const currentApp = ref<ApplicationVO | null>(null)

function handleDetail(row: ApplicationVO) {
  currentApp.value = row
  detailVisible.value = true
}

// 审批弹窗
const reviewVisible = ref(false)
const reviewLoading = ref(false)
const reviewForm = reactive({
  approved: true,
  reviewComment: '',
})

function handleReview(row: ApplicationVO) {
  currentApp.value = row
  reviewForm.approved = true
  reviewForm.reviewComment = ''
  reviewVisible.value = true
}

async function handleDelete(id: number) {
  try {
    const res = await deleteApplication(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch {
    // error handled by interceptor
  }
}

async function submitReview() {
  if (!currentApp.value) return
  reviewLoading.value = true
  try {
    const res = await reviewApplication(currentApp.value.id, {
      approved: reviewForm.approved,
      reviewComment: reviewForm.reviewComment || undefined,
    })
    if (res.code === 200) {
      ElMessage.success(reviewForm.approved ? '已通过审批' : '已驳回申请')
      reviewVisible.value = false
      loadData()
    }
  } finally {
    reviewLoading.value = false
  }
}

// 辅助方法
function statusText(status: string): string {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回',
  }
  return map[status] || status
}

function statusTagType(status: string): string {
  const map: Record<string, string> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
  }
  return map[status] || 'info'
}

function customerTypeText(type?: string): string {
  const map: Record<string, string> = {
    PERSONAL: '个人',
    FAMILY: '家庭',
    COMPANY: '公司',
    SCHOOL: '学校',
    OTHER_ORG: '其他单位',
    INDIVIDUAL: '个人',
    ENTERPRISE: '企业',
  }
  return type ? (map[type] || type) : '-'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.application-list-page {
  padding: 16px;

  .search-card {
    margin-bottom: 16px;
  }

  .table-card {
    .pagination-wrapper {
      display: flex;
      justify-content: flex-end;
      margin-top: 16px;
    }
  }
}
</style>
