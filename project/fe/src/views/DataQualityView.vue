<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'

const summary = [
  { label: '完整率', value: '97.8%', caption: '缺失点主要集中在凌晨采集窗口' },
  { label: '异常占比', value: '2.1%', caption: '高峰时段出现局部尖峰' },
  { label: '连续性评分', value: '91', caption: '整体连续，少量时段有断点' },
  { label: '训练可用性', value: '可用', caption: '建议先完成缺失值填补' },
]

const missingOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['48%', '68%'],
      label: { color: '#52627e' },
      data: [
        { name: '有效数据', value: 97.8 },
        { name: '缺失数据', value: 2.2 },
      ],
    },
  ],
}))

const anomalyOption = computed(() => ({
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
      data: [4, 6, 3, 2, 5, 8, 7],
      barMaxWidth: 28,
      itemStyle: { color: '#64748b', borderRadius: [8, 8, 0, 0] },
    },
  ],
}))

const continuityOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 20, top: 30, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00'],
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'value',
    min: 70,
    max: 100,
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112,136,170,0.14)' } },
  },
  series: [
    {
      type: 'line',
      smooth: true,
      data: [98, 93, 96, 99, 94, 91],
      lineStyle: { color: '#0f766e', width: 3 },
      itemStyle: { color: '#0f766e' },
      areaStyle: { color: 'rgba(15,118,110,0.12)' },
    },
  ],
}))

const anomalies = [
  { time: '2026-04-11 03:10', type: '缺失', suggestion: '用邻近窗口补值后复核趋势' },
  { time: '2026-04-11 18:20', type: '尖峰', suggestion: '结合调度记录判断是否为真实高峰' },
  { time: '2026-04-10 02:40', type: '重复', suggestion: '建议保留首条并标记来源差异' },
]
</script>

<template>
  <n-space vertical :size="20" class="page-section">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据质量分析</h1>
        <p class="page-subtitle">以摘要卡、异常统计、缺失分布和建议结论支撑训练前质量判断。</p>
      </div>
    </div>

    <div class="metric-grid">
      <div v-for="item in summary" :key="item.label" class="metric-panel">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
        <div class="metric-caption">{{ item.caption }}</div>
      </div>
    </div>

    <div class="grid-3">
      <n-card :bordered="false" class="content-card soft-card" title="缺失值占比">
        <v-chart class="chart" :option="missingOption" autoresize />
      </n-card>
      <n-card :bordered="false" class="content-card soft-card" title="异常值数量">
        <v-chart class="chart" :option="anomalyOption" autoresize />
      </n-card>
      <n-card :bordered="false" class="content-card soft-card" title="时间连续性">
        <v-chart class="chart" :option="continuityOption" autoresize />
      </n-card>
    </div>

    <div class="section-grid">
      <n-card :bordered="false" class="content-card soft-card" title="异常记录列表">
        <div class="list-plain">
          <div v-for="item in anomalies" :key="item.time" class="list-plain-item">
            <n-space justify="space-between" align="center">
              <div>
                <div style="font-weight: 600; color: #122742;">{{ item.time }}</div>
                <div class="panel-subtitle" style="margin-top: 4px;">{{ item.suggestion }}</div>
              </div>
              <n-tag :type="item.type === '尖峰' ? 'warning' : 'default'">{{ item.type }}</n-tag>
            </n-space>
          </div>
        </div>
      </n-card>

      <div class="section-side">
        <n-card :bordered="false" class="content-card soft-card">
          <div>
            <h3 class="panel-title">结果说明</h3>
            <p class="panel-subtitle">当前数据整体可进入训练，但建议先完成缺失值补齐和尖峰复核。</p>
          </div>
          <n-space vertical style="margin-top: 16px;">
            <n-alert type="info" :show-icon="false">缺失比例低于 3%，对整体趋势影响可控。</n-alert>
            <n-alert type="warning" :show-icon="false">晚高峰波动更明显，建议单独关注该时段。</n-alert>
            <n-alert type="success" :show-icon="false">连续性评分超过 90，适合作为主训练集。</n-alert>
          </n-space>
        </n-card>
      </div>
    </div>
  </n-space>
</template>
