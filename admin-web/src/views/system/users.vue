<template>
  <div class="users-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="用户名/姓名/手机号/工号" clearable style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="openCreateDialog">新增账户</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" stripe border>
        <el-table-column prop="employeeNo" label="工号" min-width="80" />
        <el-table-column prop="username" label="用户名" min-width="100" />
        <el-table-column prop="name" label="姓名" min-width="80" />
        <el-table-column prop="roleName" label="角色" min-width="100">
          <template #default="{ row }">
            <el-tag>{{ row.roleName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="department" label="部门" min-width="120" />
        <el-table-column label="身份核验" min-width="140">
          <template #default="{ row }">
            <el-tag v-if="row.identityVerified" type="success">已核验</el-tag>
            <el-tag v-else type="info">未核验</el-tag>
            <div v-if="row.identityVerifiedAt" class="verification-time">{{ formatDateTime(row.identityVerifiedAt) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row as any)">编辑</el-button>
            <el-button type="warning" link size="small" @click="openResetDialog(row as any)">重置密码</el-button>
            <el-popconfirm
              :title="row.identityVerified ? '确认取消该账户的身份核验？' : '确认已核验该账户的真实姓名和手机号？'"
              @confirm="handleIdentityVerification(row as any, !row.identityVerified)"
            >
              <template #reference>
                <el-button :type="row.identityVerified ? 'info' : 'success'" link size="small">
                  {{ row.identityVerified ? '取消核验' : '核验身份' }}
                </el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="确认删除该账户？" @confirm="handleDelete(row as any)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑账户' : '新增账户'" width="560px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工号" prop="employeeNo">
              <el-input v-model="formData.employeeNo" placeholder="如: 000002" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" placeholder="登录用户名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="formData.password" type="password" show-password placeholder="初始密码" />
        </el-form-item>
        <el-form-item v-else label="新密码">
          <el-input v-model="formData.password" type="password" show-password placeholder="留空不修改" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="formData.name" placeholder="真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleId">
              <el-select v-model="formData.roleId" placeholder="选择角色" style="width: 100%">
                <el-option v-for="r in roleOptions" :key="r.id" :label="r.roleName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="formData.phone" placeholder="手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="formData.department" placeholder="部门" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="邮箱地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="showResetDialog" title="重置密码" width="400px">
      <el-form label-width="80px">
        <el-form-item label="账户">
          <span>{{ currentRow?.username }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetDialog = false">取消</el-button>
        <el-button type="primary" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { pageUsers, createUser, updateUser, deleteUser, resetPassword, updateUserIdentityVerification, listAllRoles } from '@/api/system'
import type { SysAccountVO, SysRoleVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const tableData = ref<SysAccountVO[]>([])
const roleOptions = ref<SysRoleVO[]>([])
const searchForm = reactive({ keyword: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const showDialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref(0)
const formData = reactive({
  employeeNo: '', username: '', password: '', roleId: undefined as number | undefined,
  name: '', phone: '', email: '', department: '',
})
const formRules = {
  employeeNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
}
const showResetDialog = ref(false)
const currentRow = ref<SysAccountVO | null>(null)
const newPassword = ref('')

async function loadData() {
  try {
    const res = await pageUsers({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total
  } catch (e) {
    ElMessage.error('加载账户列表失败')
  }
}

async function loadRoles() {
  try {
    const res = await listAllRoles()
    roleOptions.value = res.data || []
  } catch (e) {
    // ignore
  }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() { searchForm.keyword = ''; pagination.pageNum = 1; loadData() }

function openCreateDialog() {
  isEdit.value = false
  formRef.value?.clearValidate()
  Object.assign(formData, { employeeNo: '', username: '', password: '', roleId: undefined, name: '', phone: '', email: '', department: '' })
  showDialog.value = true
}

function openEditDialog(row: SysAccountVO) {
  isEdit.value = true
  formRef.value?.clearValidate()
  editingId.value = row.id
  Object.assign(formData, {
    employeeNo: row.employeeNo, username: row.username, password: '',
    roleId: row.roleId, name: row.name || '', phone: row.phone || '',
    email: row.email || '', department: row.department || '',
  })
  showDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        const data: any = { ...formData }
        if (!data.password) delete data.password
        await updateUser(editingId.value, data)
        ElMessage.success('更新成功')
      } else {
        await createUser({ ...formData })
        ElMessage.success('创建成功')
      }
      showDialog.value = false
      loadData()
    } catch (e) {
      // error handled by interceptor
    }
  })
}

async function handleDelete(row: SysAccountVO) {
  try {
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function handleIdentityVerification(row: SysAccountVO, verified: boolean) {
  try {
    await updateUserIdentityVerification(row.id, verified)
    ElMessage.success(verified ? '身份核验完成' : '已取消身份核验')
    loadData()
  } catch (e) {
    // Error message is handled by the request interceptor.
  }
}

function openResetDialog(row: SysAccountVO) {
  currentRow.value = row
  newPassword.value = ''
  showResetDialog.value = true
}

async function handleResetPassword() {
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  try {
    await resetPassword(currentRow.value!.id, newPassword.value)
    ElMessage.success('密码重置成功')
    showResetDialog.value = false
  } catch (e) {
    ElMessage.error('密码重置失败')
  }
}

onMounted(() => { loadData(); loadRoles() })
</script>

<style scoped lang="scss">
.users-page { padding: 16px; }
.search-card { margin-bottom: 12px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.verification-time { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
