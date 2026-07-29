<template>
  <div class="roles-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form class="search-form" :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="角色名称/编码" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button v-permission="'SYSTEM_ROLE'" type="success" @click="openCreateDialog">新增角色</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card>
      <el-table :data="tableData" stripe border>
        <el-table-column prop="roleCode" label="角色编码" min-width="120" />
        <el-table-column prop="roleName" label="角色名称" min-width="120" />
        <el-table-column prop="roleDesc" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'SYSTEM_ROLE'" type="primary" link size="small" @click="openEditDialog(row as any)">编辑</el-button>
            <el-button v-permission="'SYSTEM_ROLE_PERMISSION'" type="success" link size="small" @click="openPermissionDialog(row as any)">分配权限</el-button>
            <el-popconfirm title="确认删除该角色？" @confirm="handleDelete(row as any)">
              <template #reference>
                <el-button v-permission="'SYSTEM_ROLE'" type="danger" link size="small">删除</el-button>
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
    <el-dialog v-model="showDialog" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="80px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="formData.roleCode" :disabled="isEdit" placeholder="如: ADMIN" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="formData.roleName" placeholder="如: 平台管理员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.roleDesc" type="textarea" :rows="3" placeholder="角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showPermissionDialog"
      :title="`分配权限 - ${currentRole?.roleName || ''}`"
      width="680px"
    >
      <el-alert
        title="勾选菜单、页面和按钮/API 权限后保存；权限变更后，相关用户需要重新登录或刷新用户信息后生效。"
        type="info"
        show-icon
        :closable="false"
        class="permission-tip"
      />
      <el-tree
        :key="permissionDialogKey"
        ref="permissionTreeRef"
        v-loading="permissionLoading"
        class="permission-tree"
        :data="permissionTreeData"
        :props="permissionTreeProps"
        node-key="id"
        show-checkbox
        default-expand-all
      >
        <template #default="{ data }">
          <span class="permission-node">
            <span>{{ data.permissionName }}</span>
            <el-tag size="small" effect="plain">{{ data.permissionCode }}</el-tag>
            <el-tag size="small" :type="permissionTypeTag(data.permissionType)">
              {{ permissionTypeLabel(data.permissionType) }}
            </el-tag>
          </span>
        </template>
      </el-tree>
      <template #footer>
        <el-button @click="showPermissionDialog = false">取消</el-button>
        <el-button type="primary" :loading="permissionSaving" @click="handleSavePermissions">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, type FormInstance, type TreeInstance } from 'element-plus'
import {
  pageRoles,
  createRole,
  updateRole,
  deleteRole,
  permissionTree,
  getRolePermissions,
  updateRolePermissions
} from '@/api/system'
import type { SysPermissionVO, SysRoleVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const tableData = ref<SysRoleVO[]>([])
const searchForm = reactive({ keyword: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const showDialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref(0)
const formData = reactive({ roleCode: '', roleName: '', roleDesc: '' })
const showPermissionDialog = ref(false)
const permissionLoading = ref(false)
const permissionSaving = ref(false)
const permissionDialogKey = ref(0)
const currentRole = ref<SysRoleVO | null>(null)
const permissionTreeRef = ref<TreeInstance>()
const permissionTreeData = ref<SysPermissionVO[]>([])
const permissionTreeProps = { children: 'children', label: 'permissionName' }
const formRules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
}

async function loadData() {
  try {
    const res = await pageRoles({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
    })
    tableData.value = res.data.records || []
    pagination.total = res.data.total
  } catch (e) {
    ElMessage.error('加载角色列表失败')
  }
}

function handleSearch() { pagination.pageNum = 1; loadData() }
function handleReset() { searchForm.keyword = ''; pagination.pageNum = 1; loadData() }

function openCreateDialog() {
  isEdit.value = false
  formData.roleCode = ''
  formData.roleName = ''
  formData.roleDesc = ''
  showDialog.value = true
}

function openEditDialog(row: SysRoleVO) {
  isEdit.value = true
  editingId.value = row.id
  formData.roleCode = row.roleCode
  formData.roleName = row.roleName
  formData.roleDesc = row.roleDesc || ''
  showDialog.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value) {
        await updateRole(editingId.value, { ...formData })
        ElMessage.success('更新成功')
      } else {
        await createRole({ ...formData })
        ElMessage.success('创建成功')
      }
      showDialog.value = false
      loadData()
    } catch (e) {
      // error handled by interceptor
    }
  })
}

async function handleDelete(row: SysRoleVO) {
  try {
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

async function openPermissionDialog(row: SysRoleVO) {
  currentRole.value = row
  showPermissionDialog.value = true
  permissionLoading.value = true
  try {
    const [treeRes, checkedRes] = await Promise.all([
      permissionTree(),
      getRolePermissions(row.id)
    ])
    permissionTreeData.value = treeRes.data || []
    permissionDialogKey.value++
    await nextTick()
    permissionTreeRef.value?.setCheckedKeys((checkedRes.data || []) as any, false)
  } catch (e) {
    ElMessage.error('加载角色权限失败')
  } finally {
    permissionLoading.value = false
  }
}

async function handleSavePermissions() {
  if (!currentRole.value || !permissionTreeRef.value) return
  permissionSaving.value = true
  try {
    const checkedKeys = permissionTreeRef.value.getCheckedKeys(false)
    const halfCheckedKeys = permissionTreeRef.value.getHalfCheckedKeys()
    const permissionIds = Array.from(new Set([...checkedKeys, ...halfCheckedKeys]))
      .map(id => Number(id))
      .filter(id => Number.isFinite(id))
    await updateRolePermissions(currentRole.value.id, permissionIds)
    ElMessage.success('权限保存成功')
    showPermissionDialog.value = false
  } catch (e) {
    ElMessage.error('权限保存失败')
  } finally {
    permissionSaving.value = false
  }
}

function permissionTypeTag(type: string) {
  if (type === 'MENU') return 'success'
  if (type === 'BUTTON') return 'warning'
  if (type === 'API') return 'info'
  return ''
}

function permissionTypeLabel(type: string) {
  if (type === 'MENU') return '菜单'
  if (type === 'BUTTON') return '按钮'
  if (type === 'API') return '接口'
  return type || '-'
}

onMounted(() => { loadData() })
</script>

<style scoped lang="scss">
.roles-page { padding: 16px; }
.search-card { margin-bottom: 12px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.permission-tip { margin-bottom: 12px; }
.permission-tree {
  max-height: 560px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 0;
}
.permission-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
