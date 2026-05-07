<script setup lang="ts">
import axios from 'axios'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import VChart from 'vue-echarts'

interface KpiItem {
  label: string
  value: string
  unit: string
  meta: string
}

interface PointItem {
  time: string
  value: number
}

interface WarningItem {
  name: string
  level: '高' | '中' | '低'
  detail: string
  value: number
}

interface TaskItem {
  title: string
  status: string
  time: string
  detail: string
}

interface RankingItem {
  station: string
  score: number
  status: string
}

interface StatusItem {
  name: string
  value: number
}

interface PredictionSummary {
  currentValue: number
  peakTime: string
  peakValue: number
  averageValue: number
  rise: number
}

interface ScreenData {
  modelName: string
  refreshSeconds: number
  algorithmStatus: string
  generatedAt: string
  headline: string
  kpis: KpiItem[]
  trendSeries: PointItem[]
  prediction: {
    actual: PointItem[]
    forecast: PointItem[]
    summary: PredictionSummary
  }
  warnings: WarningItem[]
  tasks: TaskItem[]
  ranking: RankingItem[]
  statusDistribution: StatusItem[]
  stageStability: StatusItem[]
  accuracy: number
}

const loading = ref(false)
const loadError = ref('')
const screenData = ref<ScreenData | null>(null)

let timer: number | null = null

const fetchScreenData = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/visual/screen/summary')
    screenData.value = response.data?.data ?? null
    loadError.value = ''
  } catch {
    loadError.value = '大屏数据加载失败，请检查后端与算法服务状态。'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchScreenData()
  timer = window.setInterval(fetchScreenData, 30000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})

const kpis = computed(() => screenData.value?.kpis ?? [])
const warnings = computed(() => screenData.value?.warnings ?? [])
const tasks = computed(() => screenData.value?.tasks ?? [])
const ranking = computed(() => screenData.value?.ranking ?? [])
const predictionSummary = computed<PredictionSummary>(() =>
  screenData.value?.prediction.summary ?? {
    currentValue: 0,
    peakTime: '--',
    peakValue: 0,
    averageValue: 0,
    rise: 0,
  },
)

const trendOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis' },
  grid: { left: 24, right: 12, top: 26, bottom: 22 },
  xAxis: {
    type: 'category',
    data: (screenData.value?.trendSeries ?? []).map((item) => item.time),
    axisLabel: { color: 'rgba(207,225,248,0.7)' },
    axisLine: { lineStyle: { color: 'rgba(123,164,221,0.16)' } },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: 'rgba(207,225,248,0.7)' },
    splitLine: { lineStyle: { color: 'rgba(123,164,221,0.12)' } },
  },
  series: [
    {
      name: '历史水位',
      type: 'line',
      smooth: true,
      data: (screenData.value?.trendSeries ?? []).map((item) => item.value),
      lineStyle: { color: '#4cc9f0', width: 3 },
      itemStyle: { color: '#4cc9f0' },
      areaStyle: { color: 'rgba(76,201,240,0.12)' },
    },
  ],
}))

const predictionOption = computed(() => {
  const actual = screenData.value?.prediction.actual ?? []
  const forecast = screenData.value?.prediction.forecast ?? []
  const labels = [...actual.map((item) => item.time), ...forecast.map((item) => item.time)]
  const actualSeries = [...actual.map((item) => item.value), ...Array.from({ length: forecast.length }, () => null)]
  const forecastSeries = [
    ...Array.from({ length: Math.max(0, actual.length - 1) }, () => null),
    ...(actual.length ? [actual[actual.length - 1].value] : []),
    ...forecast.map((item) => item.value),
  ]

  return {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: { textStyle: { color: 'rgba(220,234,255,0.76)' } },
    grid: { left: 26, right: 16, top: 36, bottom: 24 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { color: 'rgba(207,225,248,0.7)' },
      axisLine: { lineStyle: { color: 'rgba(123,164,221,0.16)' } },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: 'rgba(207,225,248,0.7)' },
      splitLine: { lineStyle: { color: 'rgba(123,164,221,0.12)' } },
    },
    series: [
      {
        name: '实际',
        type: 'line',
        smooth: true,
        data: actualSeries,
        lineStyle: { color: '#7dd3fc', width: 2 },
        itemStyle: { color: '#7dd3fc' },
      },
      {
        name: '预测',
        type: 'line',
        smooth: true,
        data: forecastSeries,
        lineStyle: { color: '#60a5fa', width: 3 },
        itemStyle: { color: '#60a5fa' },
        areaStyle: { color: 'rgba(96,165,250,0.12)' },
      },
    ],
  }
})

const donutOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['60%', '76%'],
      label: { color: 'rgba(220,234,255,0.78)' },
      itemStyle: { borderWidth: 4, borderColor: '#09192b' },
      data: (screenData.value?.statusDistribution ?? []).map((item) => ({
        name: item.name,
        value: item.value,
        itemStyle: {
          color:
            item.name === '稳定'
              ? '#38bdf8'
              : item.name === '关注'
                ? '#f59e0b'
                : '#ef4444',
        },
      })),
    },
  ],
}))

const gaugeOption = computed(() => ({
  backgroundColor: 'transparent',
  series: [
    {
      type: 'gauge',
      startAngle: 220,
      endAngle: -40,
      min: 0,
      max: 100,
      progress: { show: true, width: 12, itemStyle: { color: '#60a5fa' } },
      axisLine: { lineStyle: { width: 12, color: [[1, 'rgba(145,178,221,0.14)']] } },
      pointer: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { show: false },
      detail: {
        valueAnimation: false,
        formatter: '{value}%',
        color: '#f5fbff',
        fontSize: 28,
        offsetCenter: [0, '6%'],
      },
      title: {
        offsetCenter: [0, '58%'],
        color: 'rgba(204,220,244,0.7)',
      },
      data: [{ value: screenData.value?.accuracy ?? 0, name: '可信度' }],
    },
  ],
}))

const stageOption = computed(() => ({
  backgroundColor: 'transparent',
  tooltip: { trigger: 'axis' },
  grid: { left: 26, right: 16, top: 26, bottom: 28 },
  xAxis: {
    type: 'category',
    data: (screenData.value?.stageStability ?? []).map((item) => item.name),
    axisLabel: { color: 'rgba(207,225,248,0.7)' },
    axisLine: { lineStyle: { color: 'rgba(123,164,221,0.16)' } },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: 'rgba(207,225,248,0.7)' },
    splitLine: { lineStyle: { color: 'rgba(123,164,221,0.12)' } },
  },
  series: [
    {
      type: 'bar',
      data: (screenData.value?.stageStability ?? []).map((item) => item.value),
      barMaxWidth: 24,
      itemStyle: { color: '#38bdf8', borderRadius: [8, 8, 0, 0] },
    },
  ],
}))

const tagTypeByLevel = (level: WarningItem['level']) => {
  if (level === '高') {
    return 'error'
  }
  if (level === '中') {
    return 'warning'
  }
  return 'success'
}

const taskTagType = (status: string) => {
  if (status.includes('恢复')) {
    return 'warning'
  }
  if (status.includes('关注')) {
    return 'error'
  }
  if (status.includes('完成') || status.includes('正常')) {
    return 'success'
  }
  return 'info'
}
</script>

<template>
  <div class="screen-page">
    <n-spin :show="loading">
      <div class="screen-header">
        <div>
          <h1 class="screen-title">水位预测可视化大屏</h1>
          <p class="screen-subtitle">聚焦模型输出、告警优先级和任务链路状态，适合展示与答辩场景。</p>
          <div class="screen-marquee">
            {{ screenData?.headline ?? '正在同步大屏数据...' }}
          </div>
        </div>
        <n-space>
          <n-tag round type="info">{{ screenData?.modelName ?? 'TimeMixer' }}</n-tag>
          <n-tag round :type="screenData?.algorithmStatus === '运行中' ? 'success' : 'warning'">
            算法 {{ screenData?.algorithmStatus ?? '检查中' }}
          </n-tag>
          <n-tag round type="default">刷新 {{ screenData?.refreshSeconds ?? 30 }}s</n-tag>
        </n-space>
      </div>

      <n-alert v-if="loadError" type="error" style="margin-bottom: 16px;">
        {{ loadError }}
      </n-alert>

      <div class="screen-kpi-grid">
        <div v-for="item in kpis" :key="item.label" class="screen-kpi">
          <div class="screen-kpi-label">{{ item.label }}</div>
          <div class="screen-kpi-value">
            {{ item.value }}
            <span class="screen-kpi-unit">{{ item.unit }}</span>
          </div>
          <div class="screen-kpi-meta">{{ item.meta }}</div>
        </div>
      </div>

      <div class="screen-grid">
        <div class="screen-column">
          <div class="screen-panel">
            <h3 class="screen-panel-title">历史趋势</h3>
            <p class="screen-panel-subtitle">查看最近 24 个采样点的历史水位走势。</p>
            <v-chart class="screen-chart" :option="trendOption" autoresize />
          </div>

          <div class="screen-panel">
            <h3 class="screen-panel-title">重点站点评分</h3>
            <p class="screen-panel-subtitle">按设备稳定度、预测偏移与风险状态综合排序。</p>
            <div class="screen-list">
              <div v-for="item in ranking" :key="item.station" class="screen-list-item">
                <div>
                  <div class="screen-item-title">{{ item.station }}</div>
                  <div class="screen-panel-subtitle">{{ item.status }}</div>
                </div>
                <n-tag type="info" round>{{ item.score }}</n-tag>
              </div>
            </div>
          </div>
        </div>

        <div class="screen-column">
          <div class="screen-main-card">
            <div class="screen-card-row">
              <div>
                <h3 class="screen-panel-title">未来 24 小时预测</h3>
                <p class="screen-panel-subtitle">用真实后端接口返回的水位预测结果驱动中间主图。</p>
              </div>
              <div class="screen-generated-at">
                更新于 {{ screenData?.generatedAt ?? '--' }}
              </div>
            </div>
            <v-chart class="screen-chart-large" :option="predictionOption" autoresize />
          </div>

          <div class="screen-split-2">
            <div class="screen-panel">
              <h3 class="screen-panel-title">预测摘要</h3>
              <p class="screen-panel-subtitle">抽取峰值、均值和抬升幅度，便于快速讲清预测结论。</p>
              <div class="summary-grid">
                <div class="summary-item">
                  <span>当前值</span>
                  <strong>{{ predictionSummary.currentValue.toFixed(3) }} m</strong>
                </div>
                <div class="summary-item">
                  <span>峰值时刻</span>
                  <strong>{{ predictionSummary.peakTime }}</strong>
                </div>
                <div class="summary-item">
                  <span>峰值水位</span>
                  <strong>{{ predictionSummary.peakValue.toFixed(3) }} m</strong>
                </div>
                <div class="summary-item">
                  <span>抬升幅度</span>
                  <strong>{{ predictionSummary.rise.toFixed(3) }} m</strong>
                </div>
              </div>
            </div>

            <div class="screen-panel">
              <h3 class="screen-panel-title">工艺阶段稳定度</h3>
              <p class="screen-panel-subtitle">反映各处理阶段在当前预测窗口内的稳定性评分。</p>
              <v-chart class="screen-chart" :option="stageOption" autoresize />
            </div>
          </div>
        </div>

        <div class="screen-column">
          <div class="screen-panel">
            <h3 class="screen-panel-title">运行状态结构</h3>
            <p class="screen-panel-subtitle">稳定、关注、告警三类状态占比。</p>
            <v-chart class="screen-chart" :option="donutOption" autoresize />
          </div>

          <div class="screen-panel">
            <h3 class="screen-panel-title">模型表现</h3>
            <p class="screen-panel-subtitle">根据历史观测与基线偏差估算当前可信度。</p>
            <v-chart class="screen-chart" :option="gaugeOption" autoresize />
          </div>

          <div class="screen-panel">
            <h3 class="screen-panel-title">预警信息</h3>
            <p class="screen-panel-subtitle">展示后端实时生成的高价值告警与说明。</p>
            <div class="screen-list">
              <div v-for="item in warnings" :key="item.name" class="screen-list-item">
                <div>
                  <div class="screen-item-title">{{ item.name }}</div>
                  <div class="screen-panel-subtitle">{{ item.detail }}</div>
                </div>
                <n-tag :type="tagTypeByLevel(item.level)" round>
                  {{ item.level }}
                </n-tag>
              </div>
            </div>
          </div>

          <div class="screen-panel">
            <h3 class="screen-panel-title">任务状态</h3>
            <p class="screen-panel-subtitle">覆盖数据更新、预测推理、摘要生成和告警分析链路。</p>
            <div class="screen-list">
              <div v-for="item in tasks" :key="item.title" class="task-item">
                <div class="task-head">
                  <span class="screen-item-title">{{ item.title }}</span>
                  <n-tag round size="small" :type="taskTagType(item.status)">
                    {{ item.status }}
                  </n-tag>
                </div>
                <div class="task-meta">{{ item.time }}</div>
                <div class="screen-panel-subtitle">{{ item.detail }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<style scoped>
.screen-page {
  color: #f5fbff;
}

.screen-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.screen-title {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.screen-subtitle,
.screen-panel-subtitle,
.task-meta {
  color: rgba(207, 225, 248, 0.72);
  line-height: 1.7;
}

.screen-marquee {
  margin-top: 14px;
  padding: 12px 16px;
  border: 1px solid rgba(96, 165, 250, 0.2);
  border-radius: 16px;
  background: rgba(10, 24, 45, 0.7);
  color: #d5e8ff;
}

.screen-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.screen-kpi,
.screen-panel,
.screen-main-card {
  border: 1px solid rgba(113, 158, 216, 0.12);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(9, 25, 43, 0.92) 0%, rgba(7, 19, 34, 0.94) 100%);
  box-shadow: 0 24px 80px rgba(6, 16, 30, 0.26);
}

.screen-kpi {
  padding: 20px 22px;
}

.screen-kpi-label {
  color: rgba(207, 225, 248, 0.64);
  font-size: 13px;
}

.screen-kpi-value {
  margin-top: 10px;
  font-size: 34px;
  font-weight: 700;
}

.screen-kpi-unit {
  margin-left: 8px;
  font-size: 14px;
  font-weight: 500;
}

.screen-kpi-meta {
  margin-top: 10px;
  color: #93c5fd;
  font-size: 13px;
}

.screen-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.screen-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.screen-panel,
.screen-main-card {
  padding: 20px;
}

.screen-card-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.screen-generated-at {
  color: rgba(207, 225, 248, 0.62);
  font-size: 13px;
}

.screen-panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.screen-chart {
  height: 260px;
  margin-top: 12px;
}

.screen-chart-large {
  height: 360px;
  margin-top: 12px;
}

.screen-split-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.screen-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.screen-list-item,
.task-item {
  padding: 14px 16px;
  border: 1px solid rgba(113, 158, 216, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.03);
}

.screen-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.screen-item-title {
  font-weight: 600;
}

.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 4px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.summary-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(113, 158, 216, 0.12);
}

.summary-item span {
  display: block;
  color: rgba(207, 225, 248, 0.66);
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 10px;
  font-size: 20px;
  font-weight: 700;
}

@media (max-width: 1400px) {
  .screen-kpi-grid,
  .screen-grid,
  .screen-split-2 {
    grid-template-columns: 1fr;
  }
}
</style>
