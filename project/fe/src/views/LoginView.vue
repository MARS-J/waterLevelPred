<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { LockClosedOutline, PersonOutline } from '@vicons/ionicons5'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const message = useMessage()
const authStore = useAuthStore()

const loading = ref(false)
const formValue = reactive({
  username: '',
  password: '',
})

const featureList = [
  {
    title: '一体化数据管理',
    description: '覆盖原始流量数据接入、清洗、质量检测与数据集构建。',
  },
  {
    title: 'TimeMixer 训练分析',
    description: '支持模型参数配置、训练日志追踪与评估指标展示。',
  },
  {
    title: '预测结果联动可视化',
    description: '通过驾驶舱与分析页展示真实值、预测值和误差表现。',
  }
]

const handleLogin = async () => {
  if (!formValue.username || !formValue.password) {
    message.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  const success = await authStore.login(formValue.username, formValue.password)
  loading.value = false

  if (success) {
    message.success(`欢迎回来，${authStore.displayName}`)
    router.push('/dashboard')
  } else {
    message.error(authStore.error || '登录失败')
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="login-heading">
        <h1>基于 TimeMixer 的水厂流量预测及可视化分析平台</h1>
        <p>
          面向水厂历史流量数据，构建从数据管理、模型训练、预测分析到可视化展示的完整业务闭环。
          页面采用后台管理系统与数据驾驶舱结合的设计方式，兼顾规范性与演示效果。
        </p>
      </div>

      <div class="login-feature-grid">
        <div v-for="item in featureList" :key="item.title" class="login-feature">
          <n-space vertical :size="8">
            <n-text style="font-size: 16px; color: #f6fbff; font-weight: 600;">
              {{ item.title }}
            </n-text>
            <n-text depth="3" style="color: rgba(221, 234, 255, 0.72); line-height: 1.8;">
              {{ item.description }}
            </n-text>
          </n-space>
        </div>
      </div>
    </div>

    <div class="login-form-panel">
      <n-card class="login-card" :bordered="false" content-style="padding: 36px;">
        <n-space vertical :size="24">
          <div>
            <n-text style="font-size: 28px; font-weight: 700; color: #122742;">系统登录</n-text>
            <p style="margin-top: 10px; color: #5f6f88; line-height: 1.8;">
              统一进入水厂流量预测分析平台，体验数据管理、训练建模和预测可视化等核心模块。
            </p>
          </div>

          <n-form :model="formValue" size="large">
            <n-form-item label="用户名">
              <n-input v-model:value="formValue.username" placeholder="请输入用户名">
                <template #prefix>
                  <n-icon><PersonOutline /></n-icon>
                </template>
              </n-input>
            </n-form-item>
            <n-form-item label="密码">
              <n-input
                v-model:value="formValue.password"
                type="password"
                show-password-on="click"
                placeholder="请输入密码"
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <n-icon><LockClosedOutline /></n-icon>
                </template>
              </n-input>
            </n-form-item>
            <n-space vertical :size="14">
              <n-button type="primary" size="large" block :loading="loading" @click="handleLogin">
                登录系统
              </n-button>
              <n-text depth="3">默认账号：admin / admin123</n-text>
            </n-space>
          </n-form>

          <n-alert type="info" :show-icon="false">
            推荐演示顺序：登录页 → 首页驾驶舱 → 数据分析 → 训练任务 → 预测分析
          </n-alert>
        </n-space>
      </n-card>
    </div>
  </div>
</template>
