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
          <el-button type="success" @click="openCreateDialog">新增角色</el-button>
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row as any)">编辑</el-button>
            <el-popconfirm title="确认删除该角色？" @confirm="handleDelete(row as any)">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { pageRoles, createRole, updateRole, deleteRole } from '@/api/system'
import type { SysRoleVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

const tableData = ref<SysRoleVO[]>([])
const searchForm = reactive({ keyword: '' })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const showDialog = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref(0)
const formData = reactive({ roleCode: '', roleName: '', roleDesc: '' })
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

onMounted(() => { loadData() })
</script>

<style scoped lang="scss">
.roles-page { padding: 16px; }
.search-card { margin-bottom: 12px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
</style>
