<script setup lang="ts">
import { computed, ref } from 'vue'
import VChart from 'vue-echarts'

type ReleaseStatus = '已发布' | '候选' | '归档'

interface ModelRow {
  version: string
  modelType: string
  dataset: string
  params: string
  mae: number
  rmse: number
  mape: string
  createdAt: string
  status: ReleaseStatus
}

const activeVersion = ref('TimeMixer-v1.2.0')

const models: ModelRow[] = [
  {
    version: 'TimeMixer-v1.2.0',
    modelType: 'TimeMixer',
    dataset: 'WaterPlant-Hourly',
    params: 'seq=96 / pred=24 / lr=1e-4',
    mae: 0.72,
    rmse: 0.96,
    mape: '5.3%',
    createdAt: '2026-04-11 10:16',
    status: '已发布',
  },
  {
    version: 'TimeMixer-v1.1.3',
    modelType: 'TimeMixer',
    dataset: 'NOAA-Full-Features',
    params: 'seq=96 / pred=24 / lr=2e-4',
    mae: 0.81,
    rmse: 1.08,
    mape: '5.9%',
    createdAt: '2026-04-08 18:42',
    status: '候选',
  },
  {
    version: 'PatchTST-v0.9.2',
    modelType: 'PatchTST',
    dataset: 'WaterPlant-Hourly',
    params: 'seq=96 / pred=24 / patch=16',
    mae: 0.88,
    rmse: 1.12,
    mape: '6.4%',
    createdAt: '2026-04-02 09:15',
    status: '归档',
  },
]

const compareOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { textStyle: { color: '#6b7b93' } },
  grid: { left: 36, right: 20, top: 36, bottom: 24 },
  xAxis: {
    type: 'category',
    data: models.map((item) => item.version),
    axisLabel: { color: '#6b7b93', rotate: 10 },
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#6b7b93' },
    splitLine: { lineStyle: { color: 'rgba(112,136,170,0.14)' } },
  },
  series: [
    {
      name: 'MAE',
      type: 'bar',
      barMaxWidth: 28,
      data: models.map((item) => item.mae),
      itemStyle: { color: '#3b82f6', borderRadius: [8, 8, 0, 0] },
    },
    {
      name: 'RMSE',
      type: 'bar',
      barMaxWidth: 28,
      data: models.map((item) => item.rmse),
      itemStyle: { color: '#93c5fd', borderRadius: [8, 8, 0, 0] },
    },
  ],
}))

const selectedModel = computed(
  () => models.find((item) => item.version === activeVersion.value) ?? models[0],
)

const columns = [
  { title: '版本号', key: 'version' },
  { title: '模型类型', key: 'modelType' },
  { title: '数据集', key: 'dataset' },
  { title: '主要参数', key: 'params' },
  { title: 'MAE', key: 'mae' },
  { title: 'RMSE', key: 'rmse' },
  { title: 'MAPE', key: 'mape' },
  { title: '创建时间', key: 'createdAt' },
]
</script>

<template>
  <n-space vertical :size="20" class="page-section">
    <div class="page-header">
      <div>
        <h1 class="page-title">模型管理</h1>
        <p class="page-subtitle">统一查看模型版本、评估指标、上线状态和实验差异。</p>
      </div>
      <n-space>
        <n-button>导出指标</n-button>
        <n-button type="primary">新建实验</n-button>
      </n-space>
    </div>

    <div class="section-grid">
      <n-card :bordered="false" class="content-card soft-card" title="模型版本列表">
        <n-data-table :columns="columns" :data="models" :pagination="{ pageSize: 5 }">
          <template #empty>
            暂无模型版本
          </template>
        </n-data-table>
      </n-card>

      <div class="section-side">
        <div class="metric-panel">
          <div class="metric-label">当前发布版本</div>
          <div class="metric-value">{{ models[0].version }}</div>
          <div class="metric-caption">用于生产预测的默认版本，误差表现稳定，已通过最近三次回放校验。</div>
        </div>
        <n-card :bordered="false" class="content-card soft-card">
          <n-space vertical :size="14">
            <div>
              <h3 class="panel-title">版本详情</h3>
              <p class="panel-subtitle">查看当前选中模型的训练背景和上线状态。</p>
            </div>
            <n-select
              v-model:value="activeVersion"
              :options="models.map((item) => ({ label: item.version, value: item.version }))"
            />
            <n-descriptions :column="1" label-placement="left">
              <n-descriptions-item label="模型类型">{{ selectedModel.modelType }}</n-descriptions-item>
              <n-descriptions-item label="数据集">{{ selectedModel.dataset }}</n-descriptions-item>
              <n-descriptions-item label="参数">{{ selectedModel.params }}</n-descriptions-item>
              <n-descriptions-item label="上线状态">{{ selectedModel.status }}</n-descriptions-item>
            </n-descriptions>
            <n-space>
              <n-button tertiary>查看详情</n-button>
              <n-button type="primary">设为发布</n-button>
            </n-space>
          </n-space>
        </n-card>
      </div>
    </div>

    <n-card :bordered="false" class="content-card soft-card" title="评估指标对比">
      <v-chart class="chart-tall" :option="compareOption" autoresize />
    </n-card>
  </n-space>
</template>
