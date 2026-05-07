<script setup lang="ts">
import { computed, reactive } from 'vue'
import VChart from 'vue-echarts'

const trainingForm = reactive({
  dataset: 'NOAA-Full-Features',
  modelType: 'TimeMixer',
  inputLength: 96,
  predLength: 24,
  batchSize: 32,
  learningRate: 0.0001,
  epochs: 80,
  randomSeed: 2026,
  remark: '用于未来 24 小时流量预测',
})

const datasetOptions = [
  { label: 'NOAA-Full-Features', value: 'NOAA-Full-Features' },
  { label: 'WaterPlant-Hourly', value: 'WaterPlant-Hourly' },
]

const modelOptions = [
  { label: 'TimeMixer', value: 'TimeMixer' },
  { label: 'PatchTST', value: 'PatchTST' },
]

const logList = [
  '2026-04-11 10:00:12 - 加载数据集 WaterPlant-Hourly 完成',
  '2026-04-11 10:00:18 - 数据标准化与窗口切分完成',
  '2026-04-11 10:01:02 - Epoch 12/80, loss = 0.2134',
  '2026-04-11 10:02:06 - Epoch 36/80, loss = 0.1347',
  '2026-04-11 10:03:36 - 当前验证集 MAPE = 5.3%',
]

const metrics = [
  { label: 'MAE', value: '0.72' },
  { label: 'RMSE', value: '0.96' },
  { label: 'MAPE', value: '5.3%' },
  { label: '训练耗时', value: '13m 24s' },
]

const lossOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 36, right: 20, top: 30, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['1', '10', '20', '30', '40', '50', '60', '70', '80'],
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
      data: [0.42, 0.31, 0.24, 0.18, 0.15, 0.13, 0.12, 0.11, 0.1],
      lineStyle: { color: '#1677ff', width: 3 },
      areaStyle: { color: 'rgba(22,119,255,0.16)' },
      itemStyle: { color: '#1677ff' },
    },
  ],
}))
</script>

<template>
  <n-space vertical :size="20" class="page-section">
    <div class="page-header">
      <div>
        <h1 class="page-title">训练任务</h1>
        <p class="page-subtitle">
          按照文档中的参数配置面板 + 任务状态与训练结果布局，展示模型训练工程化能力。
        </p>
      </div>
      <n-tag round type="warning">当前任务状态：训练中</n-tag>
    </div>

    <div class="grid-2">
      <n-card :bordered="false" class="content-card" title="参数配置面板">
        <n-form :model="trainingForm" label-placement="top">
          <n-grid :cols="24" :x-gap="16">
            <n-grid-item :span="12">
              <n-form-item label="数据集选择">
                <n-select v-model:value="trainingForm.dataset" :options="datasetOptions" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item :span="12">
              <n-form-item label="模型类型">
                <n-select v-model:value="trainingForm.modelType" :options="modelOptions" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item :span="8">
              <n-form-item label="输入长度">
                <n-input-number v-model:value="trainingForm.inputLength" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item :span="8">
              <n-form-item label="预测步长">
                <n-input-number v-model:value="trainingForm.predLength" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item :span="8">
              <n-form-item label="批大小">
                <n-input-number v-model:value="trainingForm.batchSize" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item :span="8">
              <n-form-item label="学习率">
                <n-input-number v-model:value="trainingForm.learningRate" :step="0.0001" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item :span="8">
              <n-form-item label="训练轮数">
                <n-input-number v-model:value="trainingForm.epochs" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item :span="8">
              <n-form-item label="随机种子">
                <n-input-number v-model:value="trainingForm.randomSeed" style="width: 100%;" />
              </n-form-item>
            </n-grid-item>
            <n-grid-item :span="24">
              <n-form-item label="备注说明">
                <n-input
                  v-model:value="trainingForm.remark"
                  type="textarea"
                  :autosize="{ minRows: 4, maxRows: 6 }"
                />
              </n-form-item>
            </n-grid-item>
          </n-grid>
        </n-form>
        <n-space justify="end">
          <n-button>保存参数</n-button>
          <n-button type="primary">开始训练</n-button>
        </n-space>
      </n-card>

      <n-space vertical :size="16">
        <n-card :bordered="false" class="content-card" title="任务状态">
          <n-grid :cols="2" :x-gap="12" :y-gap="12">
            <n-grid-item v-for="item in metrics" :key="item.label">
              <n-statistic :label="item.label" :value="item.value" />
            </n-grid-item>
          </n-grid>
          <n-descriptions :column="1" label-placement="left" style="margin-top: 18px;">
            <n-descriptions-item label="开始时间">2026-04-11 10:00:00</n-descriptions-item>
            <n-descriptions-item label="结束时间">预计 2026-04-11 10:14:00</n-descriptions-item>
            <n-descriptions-item label="执行节点">GPU-Worker-01</n-descriptions-item>
          </n-descriptions>
        </n-card>

        <n-card :bordered="false" class="content-card" title="训练日志">
          <n-space vertical size="small">
            <n-alert
              v-for="item in logList"
              :key="item"
              type="default"
              :show-icon="false"
              style="border-radius: 12px;"
            >
              {{ item }}
            </n-alert>
          </n-space>
        </n-card>
      </n-space>
    </div>

    <n-card :bordered="false" class="content-card" title="损失曲线">
      <v-chart class="chart-tall" :option="lossOption" autoresize />
    </n-card>
  </n-space>
</template>
