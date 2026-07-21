<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <p class="error-msg">页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped lang="scss">
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #f5f7fa;
}
.error-content {
  text-align: center;
  .error-code {
    font-size: 72px;
    font-weight: 700;
    color: #409EFF;
    margin-bottom: 16px;
  }
  .error-msg {
    font-size: 20px;
    color: #909399;
    margin-bottom: 32px;
  }
}
</style>
