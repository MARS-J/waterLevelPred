<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import VChart from 'vue-echarts'

const backendOnline = ref<boolean | null>(null)
const backendSummary = ref<Record<string, unknown> | null>(null)

const metrics = [
  { title: '当前流量', value: '12.8 m³/s', trend: '+4.6%', type: 'info' as const },
  { title: '当日累计流量', value: '96.3 万 m³', trend: '+2.3%', type: 'success' as const },
  { title: '周平均流量', value: '11.4 m³/s', trend: '-0.8%', type: 'warning' as const },
  { title: '月峰值流量', value: '15.9 m³/s', trend: '+6.1%', type: 'error' as const },
  { title: '最新模型精度', value: '94.7%', trend: 'MAPE 5.3%', type: 'success' as const },
  { title: '当前异常次数', value: '3 次', trend: '较昨日 -2', type: 'warning' as const },
]

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { textStyle: { color: '#6b7b93' } },
  grid: { left: 36, right: 20, top: 36, bottom: 24 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
    axisLine: { lineStyle: { color: '#d7e1ef' } },
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112, 136, 170, 0.14)' } },
  },
  series: [
    {
      name: '历史流量',
      type: 'line',
      smooth: true,
      areaStyle: {
        color: 'rgba(16, 120, 242, 0.16)',
      },
      lineStyle: { width: 3, color: '#1677ff' },
      itemStyle: { color: '#1677ff' },
      data: [9.6, 8.7, 10.4, 12.3, 13.1, 12.7, 11.8],
    },
    {
      name: '未来预测',
      type: 'line',
      smooth: true,
      lineStyle: { width: 3, color: '#18a058', type: 'dashed' },
      itemStyle: { color: '#18a058' },
      data: [null, null, null, 12.3, 13.4, 14.2, 15.1],
    },
  ],
}))

const compareOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { textStyle: { color: '#6b7b93' } },
  grid: { left: 36, right: 20, top: 36, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['T-5', 'T-4', 'T-3', 'T-2', 'T-1', 'T'],
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112, 136, 170, 0.14)' } },
  },
  series: [
    {
      name: '真实值',
      type: 'line',
      smooth: true,
      data: [10.1, 10.8, 11.2, 11.9, 12.6, 13.2],
      lineStyle: { color: '#0e4ecf', width: 3 },
      itemStyle: { color: '#0e4ecf' },
    },
    {
      name: '预测值',
      type: 'line',
      smooth: true,
      data: [9.8, 10.5, 11.4, 11.8, 12.4, 13.0],
      lineStyle: { color: '#13c2c2', width: 3 },
      itemStyle: { color: '#13c2c2' },
    },
  ],
}))

const distributionOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: {
    bottom: 0,
    textStyle: { color: '#6b7b93' },
  },
  series: [
    {
      type: 'pie',
      radius: ['45%', '68%'],
      label: { color: '#51627d' },
      data: [
        { name: '低谷', value: 22 },
        { name: '平稳', value: 46 },
        { name: '高峰', value: 24 },
        { name: '异常', value: 8 },
      ],
    },
  ],
}))

const recentTrainingTasks = [
  { name: 'TimeMixer-20260411-A', status: '已完成', metric: 'MAPE 5.3%', time: '10:30' },
  { name: 'TimeMixer-20260410-B', status: '训练中', metric: 'Epoch 36/80', time: '09:12' },
  { name: 'PatchTST-20260409-C', status: '已完成', metric: 'MAPE 6.1%', time: '昨天' },
]

const recentPredictionTasks = [
  { name: '未来 24h 预测', status: '成功', info: '峰值出现在 18:00-20:00' },
  { name: '生产调度预测', status: '成功', info: '高峰流量偏高 4.8%' },
  { name: '异常回放分析', status: '待确认', info: '建议复核 03:00 时段' },
]

const alertList = [
  '今日 18:00 后流量持续上升，建议关注泵房负荷变化',
  '凌晨 03:00 存在短时异常波动，建议回查原始测点数据',
  '最新模型版本已发布，可用于未来 24 小时预测任务',
]

onMounted(async () => {
  try {
    const response = await axios.get('/api/system/overview')
    backendOnline.value = true
    backendSummary.value = response.data?.data ?? null
  } catch {
    backendOnline.value = false
  }
})
</script>

<template>
  <n-space vertical :size="20" class="page-section">
    <div class="page-header">
      <div>
        <h1 class="page-title">首页驾驶舱</h1>
        <p class="page-subtitle">
          以指标卡、趋势图、任务状态与告警信息快速呈现系统监测、预测与分析能力。
        </p>
      </div>
      <n-card :bordered="false" class="content-card" style="min-width: 300px;">
        <n-space vertical :size="12">
          <n-text strong>后端服务状态</n-text>
          <div v-if="backendOnline === true" class="hero-badge">
            <span class="status-dot online"></span>
            已连接后端服务
          </div>
          <div v-else-if="backendOnline === false" class="hero-badge" style="background: rgba(208,48,80,0.1); color:#d03050;">
            <span class="status-dot offline"></span>
            后端暂未连接
          </div>
          <n-descriptions v-if="backendSummary" :column="1" label-placement="left" size="small">
            <n-descriptions-item label="项目名称">
              {{ backendSummary.projectName }}
            </n-descriptions-item>
            <n-descriptions-item label="架构">
              {{ backendSummary.architecture }}
            </n-descriptions-item>
            <n-descriptions-item label="算法地址">
              {{ backendSummary.algorithmBaseUrl }}
            </n-descriptions-item>
          </n-descriptions>
        </n-space>
      </n-card>
    </div>

    <div class="metric-grid">
      <n-card
        v-for="item in metrics"
        :key="item.title"
        :bordered="false"
        class="content-card"
        content-style="padding: 18px 20px;"
      >
        <n-space vertical :size="10">
          <n-text depth="3">{{ item.title }}</n-text>
          <n-text style="font-size: 30px; font-weight: 700; color: #13233b;">{{ item.value }}</n-text>
          <n-tag round :type="item.type">{{ item.trend }}</n-tag>
        </n-space>
      </n-card>
    </div>

    <div class="grid-2">
      <n-card :bordered="false" class="content-card chart-card" title="近 24 小时流量趋势">
        <v-chart class="chart" :option="trendOption" autoresize />
      </n-card>
      <n-card :bordered="false" class="content-card chart-card" title="真实值 / 预测值对比">
        <v-chart class="chart" :option="compareOption" autoresize />
      </n-card>
    </div>

    <div class="grid-3">
      <n-card :bordered="false" class="content-card chart-card" title="峰谷分布">
        <v-chart class="chart" :option="distributionOption" autoresize />
      </n-card>
      <n-card :bordered="false" class="content-card" title="最近训练任务">
        <n-timeline>
          <n-timeline-item
            v-for="item in recentTrainingTasks"
            :key="item.name"
            :title="item.name"
            :content="`${item.status} · ${item.metric}`"
            :time="item.time"
            :type="item.status === '训练中' ? 'warning' : 'success'"
          />
        </n-timeline>
      </n-card>
      <n-card :bordered="false" class="content-card" title="告警与提示">
        <n-space vertical>
          <n-alert
            v-for="item in alertList"
            :key="item"
            type="warning"
            :show-icon="false"
            style="border-radius: 14px;"
          >
            {{ item }}
          </n-alert>
        </n-space>
      </n-card>
    </div>

    <n-card :bordered="false" class="content-card" title="最近预测任务">
      <n-table :bordered="false" striped>
        <thead>
          <tr>
            <th>任务名称</th>
            <th>状态</th>
            <th>结果摘要</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in recentPredictionTasks" :key="item.name">
            <td>{{ item.name }}</td>
            <td>
              <n-tag :type="item.status === '成功' ? 'success' : 'warning'">{{ item.status }}</n-tag>
            </td>
            <td>{{ item.info }}</td>
          </tr>
        </tbody>
      </n-table>
    </n-card>
  </n-space>
</template>
