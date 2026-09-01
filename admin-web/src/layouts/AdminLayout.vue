<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <div class="sidebar-container" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="logo">
        <span v-if="!appStore.sidebarCollapsed">直饮水平台</span>
        <span v-else>DW</span>
      </div>
      <el-scrollbar>
        <el-menu
          :default-active="activeMenu"
          :collapse="appStore.sidebarCollapsed"
          :unique-opened="true"
          @select="handleMenuSelect"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <template v-for="route in menuRoutes" :key="route.path">
            <!-- 单层菜单 -->
            <el-menu-item v-if="!route.children || route.children.length === 1" :index="resolvePath(route)">
              <el-icon><component :is="route.children?.[0]?.meta?.icon || route.meta?.icon" /></el-icon>
              <template #title>{{ route.children?.[0]?.meta?.title || route.meta?.title }}</template>
            </el-menu-item>
            <!-- 多层菜单 -->
            <el-sub-menu v-else :index="route.path">
              <template #title>
                <el-icon><component :is="route.meta?.icon" /></el-icon>
                <span>{{ route.meta?.title }}</span>
              </template>
              <el-menu-item
                v-for="child in route.children?.filter(c => !c.meta?.hidden)"
                :key="child.path"
                :index="resolvePath(route, child)"
              >
                <el-icon><component :is="child.meta?.icon" /></el-icon>
                <template #title>{{ child.meta?.title }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>
    </div>

    <!-- 右侧主区域 -->
    <div class="main-container" :class="{ collapsed: appStore.sidebarCollapsed }">
      <!-- 顶栏 -->
      <div class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(item, idx) in breadcrumbs" :key="idx" :to="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" icon="UserFilled" />
              <span class="username">{{ userStore.nickname || 'admin' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="cancel-account" class="danger-command">注销账户</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 多页签 -->
      <div class="tags-view">
        <el-scrollbar>
          <div class="tags-scroll">
            <div
              v-for="tag in tagsViewList"
              :key="tag.path"
              class="tag-item"
              :class="{ active: isActive(tag.path) }"
              @click="router.push(tag.path)"
            >
              {{ tag.title }}
              <el-icon v-if="!tag.affix" class="close-icon" @click.stop="closeTag(tag)"><Close /></el-icon>
            </div>
          </div>
        </el-scrollbar>
      </div>

      <!-- 内容区 -->
      <div class="app-main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="route.fullPath" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
    </div>
    <AdminAiAssistant v-if="userStore.hasPermission('AI_ASSISTANT_VIEW')" />

    <el-dialog v-model="profileDialogVisible" title="个人中心" width="480px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="当前账户">{{ userStore.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账户类型">后台管理账户</el-descriptions-item>
      </el-descriptions>
      <el-alert class="profile-tip" type="info" :closable="false"
        title="如不再使用后台账户，可在此申请注销。注销后将立即退出登录，无法恢复该账户。" />
      <template #footer>
        <el-button @click="profileDialogVisible = false">关闭</el-button>
        <el-button type="danger" @click="openCancellationDialog">申请注销账户</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="cancellationDialogVisible" title="注销后台账户" width="560px" :close-on-click-modal="false" destroy-on-close>
      <el-alert
        title="此操作不可恢复"
        type="error"
        :closable="false"
        description="注销成功后，账户将立即失效并退出登录；系统不会因本次注销自动删除设备、订单、工单等业务数据。"
      />
      <div class="cancellation-disclosure">
        <p>为满足后台 AI 助手安全与投诉处理要求，以下最小化 AI 记录会在账户注销后保留至少 6 个月：</p>
        <ul>
          <li>AI 调用审计记录的脱敏摘要、模型及数据工具信息</li>
          <li>用户协议与隐私政策同意记录</li>
          <li>AI 投诉、处理和结果记录</li>
        </ul>
        <p>超过留存期限后由系统清理；其他业务数据依照各自业务留存规则处理。</p>
      </div>
      <el-form label-width="112px" @submit.prevent>
        <el-form-item label="当前密码" required>
          <el-input v-model="cancellationForm.password" type="password" show-password autocomplete="current-password" placeholder="请输入当前账户密码" />
        </el-form-item>
        <el-form-item label="确认文字" required>
          <el-input v-model="cancellationForm.confirmText" placeholder="请输入：注销账户" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="cancellationForm.acknowledged">
            我已阅读上述注销影响及 AI 记录留存说明
          </el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="cancellationSubmitting" @click="cancellationDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="cancellationSubmitting" @click="submitAccountCancellation">确认注销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { cancelCurrentUserAccount } from '@/api/system'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { constantRoutes, asyncRoutes } from '@/router/routes'
import AdminAiAssistant from '@/components/AdminAiAssistant.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const appStore = useAppStore()
const profileDialogVisible = ref(false)
const cancellationDialogVisible = ref(false)
const cancellationSubmitting = ref(false)
const cancellationForm = reactive({
  password: '',
  confirmText: '',
  acknowledged: false
})

function canAccessRoute(route: any) {
  const permission = route.meta?.permission
  if (!permission || userStore.permissions.includes('*') || userStore.roles.includes('SUPER_ADMIN')) {
    return true
  }
  if (Array.isArray(permission)) {
    return permission.some((perm: string) => userStore.permissions.includes(perm))
  }
  return userStore.permissions.includes(permission)
}

function filterMenuRoutes(routes: any[]) {
  return routes
    .filter(route => !route.meta?.hidden && canAccessRoute(route))
    .map(route => {
      const children = route.children
        ?.filter((child: any) => !child.meta?.hidden && canAccessRoute(child))
      return { ...route, children }
    })
    .filter(route => !route.children || route.children.length > 0)
}

// 合并静态和动态路由用于菜单渲染
const allRoutes = computed(() => {
  const staticAdmin = constantRoutes.find(r => r.path === '/')
  const staticChildren = staticAdmin?.children || []
  const dynamicRoutes = asyncRoutes.filter(r => !r.meta?.hidden)
  return [...staticChildren.map(c => ({ path: '/' + c.path, ...c })), ...dynamicRoutes].filter(r => !r.meta?.hidden)
})

const menuRoutes = computed(() => {
  // 简化版：直接展示所有路由
  const staticAdmin = constantRoutes.find(r => r.path === '/')
  const staticChildren = (staticAdmin?.children || []).map(c => ({
    path: '/' + c.path,  // 修复：补全前导 /，避免相对路径导致 404
    meta: c.meta,
    children: c.children
  }))
  return filterMenuRoutes([...staticChildren, ...asyncRoutes])
})

// 当前激活菜单
const activeMenu = computed(() => route.path)

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.map(item => ({
    title: item.meta.title as string,
    path: item.path
  }))
})

// 多页签列表
const tagsViewList = computed(() => {
  const tags = [...appStore.tagsView]
  // 添加 Dashboard 常驻
  if (!tags.find(t => t.path === '/dashboard')) {
    tags.unshift({ path: '/dashboard', title: '仪表盘', name: 'Dashboard' })
  }
  return tags
})

// 缓存的视图
const cachedViews = computed(() => tagsViewList.value.map(t => t.name))

// 是否激活
function isActive(path: string) {
  return route.path === path
}

// 关闭页签
function closeTag(tag: { path: string }) {
  appStore.removeTag(tag.path)
  if (route.path === tag.path) {
    const last = tagsViewList.value[tagsViewList.value.length - 1]
    router.push(last ? last.path : '/dashboard')
  }
}

// 解析菜单路径
function resolvePath(route: any, child?: any) {
  if (child) {
    return route.path.endsWith('/') ? `${route.path}${child.path}` : `${route.path}/${child.path}`
  }
  if (route.children && route.children.length === 1) {
    const c = route.children[0]
    return route.path.endsWith('/') ? `${route.path}${c.path}` : `${route.path}/${c.path}`
  }
  return route.path
}

// 菜单点击处理：external 标记的在新窗口打开，其余正常跳转
function handleMenuSelect(index: string) {
  // 查找对应的路由配置
  const allRoutes = [...menuRoutes.value]
  for (const route of allRoutes) {
    if (route.children && route.children.length === 1) {
      const path = resolvePath(route)
      if (path === index && route.meta?.external) {
        window.open(path, '_blank')
        return
      }
    } else if (route.children && route.children.length > 1) {
      for (const child of route.children) {
        const path = resolvePath(route, child)
        if (path === index) {
          if (child.meta?.external) {
            window.open(path, '_blank')
            return
          }
          router.push(path)
          return
        }
      }
    } else if (route.path === index && route.meta?.external) {
      window.open(index, '_blank')
      return
    }
  }
  // 默认：正常路由跳转
  router.push(index)
}

// 监听路由变化，添加页签
watch(() => route.path, (path) => {
  if (path !== '/login' && path !== '/404' && path !== '/403') {
    appStore.addTag({
      path,
      title: (route.meta.title as string) || '未知',
      name: (route.name as string) || ''
    })
  }
}, { immediate: true })

// 下拉菜单
async function handleCommand(command: string) {
  if (command === 'profile') {
    profileDialogVisible.value = true
    return
  }
  if (command === 'cancel-account') {
    openCancellationDialog()
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 取消
    }
  }
}

function resetCancellationForm() {
  cancellationForm.password = ''
  cancellationForm.confirmText = ''
  cancellationForm.acknowledged = false
}

function openCancellationDialog() {
  profileDialogVisible.value = false
  resetCancellationForm()
  cancellationDialogVisible.value = true
}

async function submitAccountCancellation() {
  if (!cancellationForm.acknowledged) {
    ElMessage.warning('请先阅读并确认注销影响及 AI 记录留存说明')
    return
  }
  if (cancellationForm.confirmText.trim() !== '注销账户') {
    ElMessage.warning('请在确认文字中输入“注销账户”')
    return
  }

  try {
    await ElMessageBox.confirm(
      '注销后将立即退出登录，且该后台账户无法恢复。是否继续？',
      '请再次确认',
      { confirmButtonText: '继续注销', cancelButtonText: '返回', type: 'warning' }
    )
  } catch {
    return
  }

  cancellationSubmitting.value = true
  try {
    await cancelCurrentUserAccount({
      password: cancellationForm.password,
      confirmText: cancellationForm.confirmText.trim()
    })
    cancellationDialogVisible.value = false
    await userStore.logout()
    ElMessage.success('账户已注销，已退出登录')
    router.replace('/login')
  } catch {
    // Error details are displayed by the HTTP interceptor.
  } finally {
    cancellationSubmitting.value = false
  }
}
</script>

<style scoped lang="scss">
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar-container {
  width: 220px;
  height: 100%;
  background: #304156;
  transition: width 0.3s;
  overflow: hidden;
  flex-shrink: 0;

  &.collapsed {
    width: 64px;
  }

  .logo {
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 18px;
    font-weight: 700;
    background: #2b3a4d;
    white-space: nowrap;
  }

  :deep(.el-menu) {
    border-right: none;
  }
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-left 0.3s;
}

.header {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      color: #5a5e66;
      &:hover { color: #409eff; }
    }
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;

      .username {
        font-size: 14px;
        color: #303133;
      }
    }
  }
}

.tags-view {
  height: 36px;
  background: #fff;
  border-bottom: 1px solid #d8dce5;
  box-shadow: 0 1px 3px rgba(0, 21, 41, 0.08);

  .tags-scroll {
    display: flex;
    align-items: center;
    height: 36px;
    padding: 0 8px;
    white-space: nowrap;
  }

  .tag-item {
    display: inline-flex;
    align-items: center;
    height: 26px;
    padding: 0 10px;
    margin-right: 6px;
    border: 1px solid #d8dce5;
    border-radius: 3px;
    font-size: 12px;
    color: #495060;
    cursor: pointer;
    background: #fff;
    white-space: nowrap;

    &.active {
      background: #409eff;
      color: #fff;
      border-color: #409eff;
    }

    .close-icon {
      margin-left: 4px;
      font-size: 12px;
      border-radius: 50%;
      &:hover {
        background: rgba(0,0,0,0.2);
      }
    }
  }
}

.app-main {
  flex: 1;
  overflow-y: auto;
  background: #f0f2f5;
  padding: 0;
}

.profile-tip {
  margin-top: 16px;
}

.cancellation-disclosure {
  margin: 16px 0;
  padding: 12px 14px;
  border: 1px solid #f0d7d1;
  border-radius: 4px;
  background: #fff8f6;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;

  p {
    margin: 0;
  }

  ul {
    margin: 8px 0;
    padding-left: 20px;
  }
}

:deep(.danger-command) {
  color: #f56c6c;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
