<script setup lang="ts">
import { computed, reactive } from 'vue'
import VChart from 'vue-echarts'

const formValue = reactive({
  modelVersion: 'TimeMixer-v1.2.0',
  range: '未来 24 小时',
  step: 24,
})

const modelOptions = [
  { label: 'TimeMixer-v1.2.0', value: 'TimeMixer-v1.2.0' },
  { label: 'TimeMixer-v1.1.3', value: 'TimeMixer-v1.1.3' },
]

const rangeOptions = [
  { label: '未来 12 小时', value: '未来 12 小时' },
  { label: '未来 24 小时', value: '未来 24 小时' },
  { label: '未来 48 小时', value: '未来 48 小时' },
]

const resultTable = [
  { time: '2026-04-11 14:00', actual: 12.3, predict: 12.1, error: 0.2, risk: '低' },
  { time: '2026-04-11 16:00', actual: 13.0, predict: 13.4, error: 0.4, risk: '中' },
  { time: '2026-04-11 18:00', actual: 14.2, predict: 14.9, error: 0.7, risk: '高' },
  { time: '2026-04-11 20:00', actual: 13.6, predict: 13.8, error: 0.2, risk: '中' },
]

const compareOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { textStyle: { color: '#6b7b93' } },
  grid: { left: 36, right: 20, top: 36, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00'],
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
      lineStyle: { color: '#0e4ecf', width: 3 },
      data: [11.2, 11.5, 12.1, 12.3, 13.0, 14.2, 13.6],
    },
    {
      name: '预测值',
      type: 'line',
      smooth: true,
      lineStyle: { color: '#18a058', width: 3, type: 'dashed' },
      data: [11.0, 11.4, 12.0, 12.1, 13.4, 14.9, 13.8],
    },
  ],
}))

const futureTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 20, top: 36, bottom: 24 },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['当前', '+4h', '+8h', '+12h', '+16h', '+20h', '+24h'],
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112, 136, 170, 0.14)' } },
  },
  series: [
    {
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(19,194,194,0.18)' },
      lineStyle: { color: '#13c2c2', width: 3 },
      itemStyle: { color: '#13c2c2' },
      data: [12.6, 12.9, 13.4, 14.0, 14.8, 15.0, 14.3],
    },
  ],
}))

const errorOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 20, top: 36, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['14:00', '16:00', '18:00', '20:00'],
    axisLabel: { color: '#6b7b93' },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112, 136, 170, 0.14)' } },
  },
  series: [
    {
      type: 'bar',
      barWidth: 24,
      itemStyle: {
        color: '#f0a020',
        borderRadius: [8, 8, 0, 0],
      },
      data: [0.2, 0.4, 0.7, 0.2],
    },
  ],
}))

const insightList = [
  '未来 24 小时整体呈上升趋势，预计 18:00-20:00 达到高峰。',
  '晚高峰时段误差显著增大，建议结合节假日与调度因素进行特征补充。',
  '当前版本模型对平稳时段拟合效果较好，可优先用于日常调度辅助。',
]
</script>

<template>
  <n-space vertical :size="20" class="page-section">
    <div class="page-header">
      <div>
        <h1 class="page-title">预测分析</h1>
        <p class="page-subtitle">
          通过预测参数选择、结果图表区与明细分析区展示未来流量趋势与误差表现。
        </p>
      </div>
      <n-space>
        <n-button>导出结果</n-button>
        <n-button type="primary">执行预测</n-button>
      </n-space>
    </div>

    <n-card :bordered="false" class="content-card">
      <n-grid :cols="24" :x-gap="16" :y-gap="16">
        <n-grid-item :span="8">
          <n-form-item label="模型版本">
            <n-select v-model:value="formValue.modelVersion" :options="modelOptions" />
          </n-form-item>
        </n-grid-item>
        <n-grid-item :span="8">
          <n-form-item label="预测范围">
            <n-select v-model:value="formValue.range" :options="rangeOptions" />
          </n-form-item>
        </n-grid-item>
        <n-grid-item :span="8">
          <n-form-item label="未来步长">
            <n-input-number v-model:value="formValue.step" style="width: 100%;" />
          </n-form-item>
        </n-grid-item>
      </n-grid>
    </n-card>

    <div class="grid-2">
      <n-card :bordered="false" class="content-card chart-card" title="实际值与预测值对比">
        <v-chart class="chart" :option="compareOption" autoresize />
      </n-card>
      <n-card :bordered="false" class="content-card chart-card" title="未来预测趋势">
        <v-chart class="chart" :option="futureTrendOption" autoresize />
      </n-card>
    </div>

    <div class="grid-2">
      <n-card :bordered="false" class="content-card" title="误差分布">
        <v-chart class="chart" :option="errorOption" autoresize />
      </n-card>
      <n-card :bordered="false" class="content-card" title="结果解释区">
        <n-space vertical>
          <n-alert
            v-for="item in insightList"
            :key="item"
            type="info"
            :show-icon="false"
            style="border-radius: 14px;"
          >
            {{ item }}
          </n-alert>
        </n-space>
      </n-card>
    </div>

    <n-card :bordered="false" class="content-card" title="预测明细与误差分析">
      <n-table :bordered="false" striped>
        <thead>
          <tr>
            <th>时段</th>
            <th>真实值</th>
            <th>预测值</th>
            <th>绝对误差</th>
            <th>风险等级</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in resultTable" :key="item.time">
            <td>{{ item.time }}</td>
            <td>{{ item.actual }}</td>
            <td>{{ item.predict }}</td>
            <td>{{ item.error }}</td>
            <td>
              <n-tag
                :type="item.risk === '高' ? 'error' : item.risk === '中' ? 'warning' : 'success'"
              >
                {{ item.risk }}
              </n-tag>
            </td>
          </tr>
        </tbody>
      </n-table>
    </n-card>
  </n-space>
</template>
