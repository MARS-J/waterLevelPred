<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Component } from 'vue'
import type { DropdownOption, MenuOption } from 'naive-ui'
import {
  BarChartOutline,
  ChevronDown,
  DocumentTextOutline,
  FlashOutline,
  GridOutline,
  LayersOutline,
  LogOutOutline,
  PersonCircleOutline,
  PulseOutline,
  PulseSharp,
  SettingsOutline,
  StatsChartOutline,
} from '@vicons/ionicons5'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const collapsed = ref(false)

const renderIcon = (icon: Component) => () => h('span', { style: 'display:flex;align-items:center;' }, [h(icon)])

const menuOptions: MenuOption[] = [
  {
    label: '首页驾驶舱',
    key: '/dashboard',
    icon: renderIcon(GridOutline),
  },
  {
    label: '数据管理',
    key: '/data-management',
    icon: renderIcon(DocumentTextOutline),
  },
  {
    label: '训练任务',
    key: '/training-tasks',
    icon: renderIcon(FlashOutline),
  },
  {
    label: '模型管理',
    key: '/model-management',
    icon: renderIcon(LayersOutline),
  },
  {
    label: '预测分析',
    key: '/prediction-analysis',
    icon: renderIcon(BarChartOutline),
  },
  {
    label: '数据质量分析',
    key: '/data-quality',
    icon: renderIcon(PulseSharp),
  },
  {
    label: '数据统计分析',
    key: '/statistics-analysis',
    icon: renderIcon(StatsChartOutline),
  },
  {
    label: '系统管理',
    key: '/system-management',
    icon: renderIcon(SettingsOutline),
  },
  {
    label: '可视化大屏',
    key: '/visual-screen',
    icon: renderIcon(PulseOutline),
  },
  {
    label: '三维水厂场景',
    key: '/water-plant-scene',
    icon: renderIcon(LayersOutline),
  },
]

const selectedKey = computed(() => route.path)
const breadcrumbs = computed(() =>
  route.matched
    .filter((item) => item.meta?.title)
    .map((item) => ({
      title: String(item.meta.title),
      path: item.path,
    })),
)

const currentTime = computed(() =>
  new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'full',
    timeStyle: 'medium',
  }).format(new Date()),
)

const handleSelect = (key: string) => {
  router.push(key)
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

const userMenuOptions: DropdownOption[] = [
  {
    label: '退出登录',
    key: 'logout',
    icon: () => h(LogOutOutline),
  },
]

const handleUserMenuSelect = (key: string) => {
  if (key === 'logout') {
    handleLogout()
  }
}
</script>

<template>
  <n-layout has-sider class="page-shell">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="84"
      :width="260"
      :collapsed="collapsed"
      show-trigger
      content-style="display:flex;flex-direction:column;background:linear-gradient(180deg,#07192f 0%,#0b2342 100%);"
      @update:collapsed="collapsed = $event"
    >
      <div
        style="padding: 24px 20px 12px; border-bottom: 1px solid rgba(255,255,255,0.08);"
      >
        <div style="font-size: 20px; font-weight: 700; color: #f5fbff;">水厂流量预测系统</div>
        <div style="margin-top: 8px; color: rgba(214, 229, 255, 0.66); font-size: 13px;">
          TimeMixer + 可视化分析平台
        </div>
      </div>
      <div style="flex: 1; padding: 16px 12px;">
        <n-menu
          inverted
          :collapsed="collapsed"
          :collapsed-width="84"
          :collapsed-icon-size="22"
          :options="menuOptions"
          :value="selectedKey"
          @update:value="handleSelect"
        />
      </div>
      <div style="padding: 16px; border-top: 1px solid rgba(255,255,255,0.08);">
        <n-space vertical size="small">
          <n-tag round type="info" size="small">当前用户：{{ authStore.displayName }}</n-tag>
          <n-button ghost type="primary" block @click="handleLogout">
            <template #icon>
              <n-icon><LogOutOutline /></n-icon>
            </template>
            退出登录
          </n-button>
        </n-space>
      </div>
    </n-layout-sider>

    <n-layout content-style="display:flex;flex-direction:column;min-height:100vh;">
      <n-layout-header
        bordered
        style="height: 72px; padding: 0 24px; display:flex; align-items:center; justify-content:space-between; background: rgba(255,255,255,0.82); backdrop-filter: blur(16px);"
      >
        <n-space align="center" :size="18">
          <div class="hero-badge">
            <span class="status-dot online"></span>
            系统在线
          </div>
          <n-breadcrumb>
            <n-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </n-breadcrumb-item>
          </n-breadcrumb>
        </n-space>
        <n-space align="center" :size="12">
          <n-tag round type="success">
            <template #icon>
              <n-icon><PulseOutline /></n-icon>
            </template>
            数据与模型联动展示
          </n-tag>
          <n-text depth="3">{{ currentTime }}</n-text>
          <n-divider vertical style="height: 24px;" />
          <n-dropdown trigger="click" :options="userMenuOptions" @select="handleUserMenuSelect">
            <n-space align="center" :size="8" style="cursor: pointer;">
              <n-avatar :size="34" round>
                <n-icon size="22"><PersonCircleOutline /></n-icon>
              </n-avatar>
              <n-space vertical :size="0">
                <n-text style="font-size: 14px; font-weight: 600; color: #122742;">
                  {{ authStore.displayName }}
                </n-text>
                <n-text depth="3" style="font-size: 12px;">
                  {{ authStore.userRole === 'admin' ? '管理员' : '普通用户' }}
                </n-text>
              </n-space>
              <n-icon size="16" depth="3"><ChevronDown /></n-icon>
            </n-space>
          </n-dropdown>
        </n-space>
      </n-layout-header>

      <n-layout-content
        embedded
        content-style="padding: 24px; background: transparent; flex: 1;"
      >
        <router-view />
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>
