<script setup lang="ts">
import { computed, reactive } from 'vue'
import VChart from 'vue-echarts'

const filterState = reactive({
  period: '近30天',
  point: '进水总管',
  mode: '工作日',
})

const periodOptions = [
  { label: '近7天', value: '近7天' },
  { label: '近30天', value: '近30天' },
  { label: '近90天', value: '近90天' },
]

const pointOptions = [
  { label: '进水总管', value: '进水总管' },
  { label: '泵房入口', value: '泵房入口' },
]

const modeOptions = [
  { label: '工作日', value: '工作日' },
  { label: '周末', value: '周末' },
  { label: '全部', value: '全部' },
]

const hourlyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 20, top: 30, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['0', '4', '8', '12', '16', '20', '24'],
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112,136,170,0.14)' } },
  },
  series: [
    {
      type: 'line',
      smooth: true,
      data: [8.2, 7.6, 9.8, 11.3, 12.7, 12.1, 9.4],
      lineStyle: { color: '#2563eb', width: 3 },
      itemStyle: { color: '#2563eb' },
      areaStyle: { color: 'rgba(37,99,235,0.12)' },
    },
  ],
}))

const weeklyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 20, top: 30, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112,136,170,0.14)' } },
  },
  series: [
    {
      type: 'bar',
      data: [10.8, 11.2, 10.9, 11.4, 11.8, 9.6, 9.3],
      itemStyle: { color: '#94a3b8', borderRadius: [8, 8, 0, 0] },
      barMaxWidth: 26,
    },
  ],
}))

const monthlyOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 20, top: 30, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['1月', '2月', '3月', '4月', '5月', '6月'],
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112,136,170,0.14)' } },
  },
  series: [
    {
      type: 'line',
      smooth: true,
      data: [9.2, 9.8, 10.4, 10.9, 11.6, 12.1],
      lineStyle: { color: '#0f766e', width: 3 },
      itemStyle: { color: '#0f766e' },
    },
  ],
}))

const heatmapOption = computed(() => ({
  tooltip: { position: 'top' },
  grid: { height: '76%', top: '8%' },
  xAxis: {
    type: 'category',
    data: ['0', '4', '8', '12', '16', '20'],
    splitArea: { show: true },
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    splitArea: { show: true },
    axisLabel: { color: '#6b7b93' },
  },
  visualMap: {
    min: 6,
    max: 15,
    calculable: false,
    orient: 'horizontal',
    left: 'center',
    bottom: 0,
  },
  series: [
    {
      type: 'heatmap',
      data: [
        [0, 0, 7], [1, 0, 6], [2, 0, 10], [3, 0, 12], [4, 0, 13], [5, 0, 11],
        [0, 1, 7], [1, 1, 7], [2, 1, 11], [3, 1, 12], [4, 1, 14], [5, 1, 12],
        [0, 2, 8], [1, 2, 7], [2, 2, 11], [3, 2, 12], [4, 2, 13], [5, 2, 12],
        [0, 3, 8], [1, 3, 7], [2, 3, 10], [3, 3, 11], [4, 3, 13], [5, 3, 12],
        [0, 4, 8], [1, 4, 8], [2, 4, 11], [3, 4, 13], [4, 4, 15], [5, 4, 13],
        [0, 5, 7], [1, 5, 7], [2, 5, 9], [3, 5, 10], [4, 5, 11], [5, 5, 10],
        [0, 6, 7], [1, 6, 6], [2, 6, 9], [3, 6, 10], [4, 6, 10], [5, 6, 9],
      ],
      label: { show: false },
    },
  ],
}))
</script>

<template>
  <n-space vertical :size="20" class="page-section">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据统计分析</h1>
        <p class="page-subtitle">从日内、周内、月度和时段分布四个维度观察历史流量规律。</p>
      </div>
    </div>

    <n-card :bordered="false" class="content-card soft-card">
      <n-grid :cols="24" :x-gap="16">
        <n-grid-item :span="8">
          <n-select v-model:value="filterState.period" :options="periodOptions" />
        </n-grid-item>
        <n-grid-item :span="8">
          <n-select v-model:value="filterState.point" :options="pointOptions" />
        </n-grid-item>
        <n-grid-item :span="8">
          <n-select v-model:value="filterState.mode" :options="modeOptions" />
        </n-grid-item>
      </n-grid>
    </n-card>

    <div class="grid-2">
      <n-card :bordered="false" class="content-card soft-card" title="日内小时分布">
        <v-chart class="chart" :option="hourlyOption" autoresize />
      </n-card>
      <n-card :bordered="false" class="content-card soft-card" title="周内工作日分布">
        <v-chart class="chart" :option="weeklyOption" autoresize />
      </n-card>
    </div>

    <div class="grid-2">
      <n-card :bordered="false" class="content-card soft-card" title="月度趋势变化">
        <v-chart class="chart" :option="monthlyOption" autoresize />
      </n-card>
      <n-card :bordered="false" class="content-card soft-card" title="时段热力分布">
        <v-chart class="chart" :option="heatmapOption" autoresize />
      </n-card>
    </div>
  </n-space>
</template>
