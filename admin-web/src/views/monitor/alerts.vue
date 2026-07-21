<template>
  <div class="alerts-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="summary-row">
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">总数</div>
            <div class="stat-value">{{ stats.total ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">未处理</div>
            <div class="stat-value warning">{{ stats.unhandled ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">已处理</div>
            <div class="stat-value success">{{ stats.handled ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">严重</div>
            <div class="stat-value danger">{{ stats.critical ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div class="stat-item">
            <div class="stat-label">警告</div>
            <div class="stat-value warn">{{ stats.warning ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="告警级别">
          <el-select v-model="searchForm.level" placeholder="全部" clearable>
            <el-option label="严重" value="CRITICAL" />
            <el-option label="警告" value="WARNING" />
            <el-option label="信息" value="INFO" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="searchForm.handleStatus" placeholder="全部" clearable>
            <el-option label="未处理" value="UNHANDLED" />
            <el-option label="已处理" value="HANDLED" />
          </el-select>
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
        <el-table-column prop="deviceSn" label="设备SN" min-width="140" />
        <el-table-column prop="alertType" label="告警类型" min-width="120" />
        <el-table-column prop="level" label="级别" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.level)">{{ statusLabel(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="200" />
        <el-table-column prop="handleStatus" label="处理状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.handleStatus === 'HANDLED' ? 'success' : 'warning'">
              {{ row.handleStatus === 'HANDLED' ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" min-width="100" />
        <el-table-column prop="handledAt" label="处理时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.handledAt) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="120">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.handleStatus === 'UNHANDLED'"
              type="primary"
              link
              size="small"
              @click="openHandleDialog(row)"
            >处理</el-button>
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

    <!-- 处理弹窗 -->
    <el-dialog v-model="showHandleDialog" title="处理告警" width="500px" @close="handleRemark = ''">
      <el-form label-width="100px">
        <el-form-item label="处理备注">
          <el-input v-model="handleRemark" type="textarea" :rows="3" placeholder="请输入处理备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAlertAction">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pageAlerts, handleAlert, getAlertStatistics } from '@/api/monitor'
import type { AlertVO, AlertStatisticsVO } from '@/api/monitor'
import { formatDateTime, statusLabel, statusTagType } from '@/utils/format'

const stats = ref<AlertStatisticsVO>({} as AlertStatisticsVO)
const tableData = ref<AlertVO[]>([])
const showHandleDialog = ref(false)
const handleRemark = ref('')
const currentRow = ref<AlertVO | null>(null)

const searchForm = reactive({ level: '', handleStatus: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

async function loadStats() {
  try {
    const res = await getAlertStatistics()
    stats.value = res.data
  } catch (e) {
    ElMessage.error('加载统计失败')
  }
}

async function loadData() {
  try {
    const res = await pageAlerts({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      level: searchForm.level || undefined,
      handleStatus: searchForm.handleStatus || undefined,
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
  searchForm.level = ''
  searchForm.handleStatus = ''
  pagination.pageNum = 1
  loadData()
}

function openHandleDialog(row: AlertVO) {
  currentRow.value = row
  handleRemark.value = ''
  showHandleDialog.value = true
}

async function handleAlertAction() {
  if (!handleRemark.value) {
    ElMessage.warning('请输入处理备注')
    return
  }
  try {
    await handleAlert(currentRow.value!.id, { remark: handleRemark.value })
    ElMessage.success('处理成功')
    showHandleDialog.value = false
    loadData()
    loadStats()
  } catch (e) {
    ElMessage.error('处理失败')
  }
}

onMounted(() => {
  loadStats()
  loadData()
})
</script>

<style scoped lang="scss">
.alerts-page {
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
  .stat-value.danger { color: #F56C6C; }
  .stat-value.warning { color: #E6A23C; }
  .stat-value.warn { color: #E6A23C; }
  .stat-value.success { color: #67C23A; }
}
.search-card {
  margin-bottom: 12px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
