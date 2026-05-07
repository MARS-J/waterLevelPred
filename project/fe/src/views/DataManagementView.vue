<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import VChart from 'vue-echarts'
import { useDialog, useMessage } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import {
  importData,
  listData,
  deleteData,
  batchDelete,
  updateData,
  checkQuality,
  type FlowRawData,
  type QualitySummary,
} from '@/api/data'
import { createDataset } from '@/api/dataset'

const message = useMessage()
const dialog = useDialog()

// Filters
const filters = reactive({
  dateRange: null as [number, number] | null,
  source: '',
  stationCode: '',
  quality: '',
})

const sourceOptions = ['SCADA', '手工录入', '历史导入'].map((v) => ({ label: v, value: v }))
const qualityOptions = ['正常', '待校验', '异常'].map((v) => ({ label: v, value: v }))

// Table state
const tableData = ref<FlowRawData[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  pageSizes: [10, 20, 50],
  showSizePicker: true,
})

// Quality summary
const qualitySummary = ref<QualitySummary | null>(null)

// Upload modal
const showUploadModal = ref(false)
const uploadFile = ref<File | null>(null)
const uploadLoading = ref(false)
const uploadForm = reactive({
  dataSource: '历史导入',
  stationCode: '进水总管',
})

// Edit modal
const showEditModal = ref(false)
const editingRow = ref<Partial<FlowRawData>>({})

// Dataset modal
const showDatasetModal = ref(false)
const datasetForm = reactive({
  name: '',
  description: '',
  startTime: null as number | null,
  endTime: null as number | null,
})

// Charts data
const sourceDistribution = ref<{ name: string; value: number }[]>([])
const trendData = ref<{ time: string; value: number }[]>([])

const columns: DataTableColumns<FlowRawData> = [
  { type: 'selection' },
  { title: 'ID', key: 'id', width: 70 },
  { title: '监测时间', key: 'monitorTime', width: 170 },
  { title: '观测水位', key: 'observedWl', width: 100 },
  { title: '预测水位', key: 'predictedWl', width: 100 },
  { title: '风速', key: 'windSpeed', width: 90 },
  { title: '气压', key: 'airPressure', width: 90 },
  { title: '气温', key: 'airTemperature', width: 90 },
  { title: '水温', key: 'waterTemperature', width: 90 },
  { title: '测点', key: 'stationCode', width: 120 },
  { title: '来源', key: 'dataSource', width: 100 },
  {
    title: '质量状态',
    key: 'qualityStatus',
    width: 100,
    render: (row) => {
      const color =
        row.qualityStatus === '正常'
          ? 'success'
          : row.qualityStatus === '异常'
            ? 'error'
            : 'warning'
      return h('n-tag', { type: color, size: 'small' }, { default: () => row.qualityStatus })
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render: (row) =>
      h('n-space', {}, {
        default: () => [
          h('n-button', { size: 'tiny', onClick: () => openEdit(row) }, { default: () => '编辑' }),
          h('n-button', { size: 'tiny', type: 'error', onClick: () => handleSingleDelete(row.id) }, { default: () => '删除' }),
        ],
      }),
  },
]

import { h } from 'vue'

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page - 1,
      size: pagination.pageSize,
    }
    if (filters.dateRange) {
      params.startTime = new Date(filters.dateRange[0]).toISOString()
      params.endTime = new Date(filters.dateRange[1]).toISOString()
    }
    if (filters.source) params.dataSource = filters.source
    if (filters.stationCode) params.stationCode = filters.stationCode
    if (filters.quality) params.qualityStatus = filters.quality

    const res = await listData(params)
    tableData.value = res.content.map((item: any) => ({
      ...item,
      monitorTime: item.monitorTime?.replace('T', ' ').slice(0, 16) ?? '',
      createdAt: item.createdAt?.replace('T', ' ').slice(0, 16) ?? '',
    }))
    pagination.itemCount = res.totalElements

    // Update charts
    updateCharts(res.content)
  } catch (e: any) {
    message.error('获取数据失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

function updateCharts(data: FlowRawData[]) {
  // Source distribution
  const sourceMap: Record<string, number> = {}
  data.forEach((item) => {
    const key = item.dataSource || '未知'
    sourceMap[key] = (sourceMap[key] || 0) + 1
  })
  sourceDistribution.value = Object.entries(sourceMap).map(([name, value]) => ({ name, value }))

  // Trend (last 50 points reversed to chronological order)
  const sorted = [...data].reverse().slice(-50)
  trendData.value = sorted.map((item) => ({
    time: item.monitorTime?.slice(5, 16) ?? '',
    value: item.observedWl ?? 0,
  }))
}

const qualityChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['48%', '68%'],
      label: { color: '#52627e' },
      data: qualitySummary.value
        ? [
            { name: '正常', value: qualitySummary.value.normal },
            { name: '待校验', value: qualitySummary.value.warning },
            { name: '异常', value: qualitySummary.value.error },
          ]
        : [],
    },
  ],
}))

const sourceChartOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['40%', '60%'],
      label: { color: '#52627e' },
      data: sourceDistribution.value,
    },
  ],
}))

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  xAxis: { type: 'category', data: trendData.value.map((d) => d.time), axisLabel: { color: '#6b7b93' } },
  yAxis: { type: 'value', axisLabel: { color: '#6b7b93' } },
  grid: { left: '10%', right: '5%', bottom: '15%', top: '10%' },
  series: [
    {
      type: 'line',
      data: trendData.value.map((d) => d.value),
      smooth: true,
      areaStyle: { opacity: 0.15 },
      itemStyle: { color: '#2080f0' },
    },
  ],
}))

function handlePageChange(page: number) {
  pagination.page = page
  fetchData()
}

function handlePageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  fetchData()
}

function resetFilters() {
  filters.dateRange = null
  filters.source = ''
  filters.stationCode = ''
  filters.quality = ''
  pagination.page = 1
  fetchData()
}

// Upload
function handleUpload() {
  if (!uploadFile.value) {
    message.warning('请选择文件')
    return
  }
  uploadLoading.value = true
  importData(uploadFile.value, uploadForm.dataSource, uploadForm.stationCode)
    .then((res) => {
      message.success(`导入成功 ${res.successCount} 条，失败 ${res.failCount} 条`)
      if (res.errors.length > 0) {
        console.warn('Import errors:', res.errors)
      }
      showUploadModal.value = false
      uploadFile.value = null
      fetchData()
    })
    .catch((e) => message.error('导入失败: ' + e.message))
    .finally(() => (uploadLoading.value = false))
}

function downloadTemplate() {
  const headers = 'date_time,observed_wl,predicted_wl,wind_speed,air_press,air_temp,water_temp,station_code,data_source,quality_status\n'
  const sample = '2026-04-11 08:00,12.4,12.5,3.2,1013.2,22.5,18.3,进水总管,SCADA,正常\n'
  const blob = new Blob([headers + sample], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'data_template.csv'
  link.click()
  URL.revokeObjectURL(link.href)
}

// Delete
function handleSingleDelete(id: number) {
  dialog.warning({
    title: '确认删除',
    content: '确定删除该条数据吗？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => {
      deleteData(id)
        .then(() => {
          message.success('删除成功')
          fetchData()
        })
        .catch((e) => message.error('删除失败: ' + e.message))
    },
  })
}

function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择数据')
    return
  }
  dialog.warning({
    title: '批量删除',
    content: `确定删除选中的 ${selectedRowKeys.value.length} 条数据吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => {
      batchDelete(selectedRowKeys.value)
        .then(() => {
          message.success('批量删除成功')
          selectedRowKeys.value = []
          fetchData()
        })
        .catch((e) => message.error('删除失败: ' + e.message))
    },
  })
}

// Edit
function openEdit(row: FlowRawData) {
  editingRow.value = { ...row }
  showEditModal.value = true
}

function handleEditSubmit() {
  const id = editingRow.value.id
  if (!id) return
  updateData(id, {
    monitorTime: editingRow.value.monitorTime,
    observedWl: editingRow.value.observedWl,
    predictedWl: editingRow.value.predictedWl,
    windSpeed: editingRow.value.windSpeed,
    airPressure: editingRow.value.airPressure,
    airTemperature: editingRow.value.airTemperature,
    waterTemperature: editingRow.value.waterTemperature,
    stationCode: editingRow.value.stationCode,
    dataSource: editingRow.value.dataSource,
    qualityStatus: editingRow.value.qualityStatus,
  })
    .then(() => {
      message.success('更新成功')
      showEditModal.value = false
      fetchData()
    })
    .catch((e) => message.error('更新失败: ' + e.message))
}

// Quality check
function handleQualityCheck() {
  checkQuality()
    .then((res) => {
      qualitySummary.value = res
      message.success('质量检测完成')
    })
    .catch((e) => message.error('检测失败: ' + e.message))
}

// Dataset
function handleCreateDataset() {
  if (!datasetForm.name || !datasetForm.startTime || !datasetForm.endTime) {
    message.warning('请填写完整信息')
    return
  }
  createDataset({
    name: datasetForm.name,
    description: datasetForm.description,
    startTime: new Date(datasetForm.startTime).toISOString(),
    endTime: new Date(datasetForm.endTime).toISOString(),
  })
    .then(() => {
      message.success('数据集创建成功')
      showDatasetModal.value = false
      datasetForm.name = ''
      datasetForm.description = ''
      datasetForm.startTime = null
      datasetForm.endTime = null
    })
    .catch((e) => message.error('创建失败: ' + e.message))
}

onMounted(() => {
  fetchData()
  handleQualityCheck()
})
</script>

<template>
  <n-space vertical :size="20" class="page-section">
    <div class="page-header">
      <div>
        <h1 class="page-title">数据管理</h1>
        <p class="page-subtitle">
          提供原始流量数据的导入、筛选、预览、质量检测与数据集生成入口。
        </p>
      </div>
      <n-space>
        <n-button @click="downloadTemplate">下载模板</n-button>
        <n-button type="primary" @click="showUploadModal = true">导入数据</n-button>
      </n-space>
    </div>

    <n-card :bordered="false" class="content-card">
      <n-grid :cols="24" :x-gap="16" :y-gap="16">
        <n-grid-item :span="6">
          <n-date-picker
            v-model:value="filters.dateRange"
            type="datetimerange"
            clearable
            style="width: 100%;"
          />
        </n-grid-item>
        <n-grid-item :span="4">
          <n-select
            v-model:value="filters.source"
            :options="sourceOptions"
            clearable
            placeholder="数据来源"
          />
        </n-grid-item>
        <n-grid-item :span="4">
          <n-input v-model:value="filters.stationCode" placeholder="测点名称" />
        </n-grid-item>
        <n-grid-item :span="4">
          <n-select
            v-model:value="filters.quality"
            :options="qualityOptions"
            clearable
            placeholder="质量状态"
          />
        </n-grid-item>
        <n-grid-item :span="6">
          <n-space justify="end">
            <n-button @click="resetFilters">重置</n-button>
            <n-button type="primary" @click="fetchData">查询</n-button>
            <n-button type="warning" @click="handleQualityCheck">发起质量检测</n-button>
            <n-button type="success" @click="showDatasetModal = true">生成数据集</n-button>
          </n-space>
        </n-grid-item>
      </n-grid>
    </n-card>

    <div class="grid-3">
      <n-card :bordered="false" class="content-card">
        <n-statistic label="数据记录数" :value="pagination.itemCount" />
      </n-card>
      <n-card :bordered="false" class="content-card">
        <n-statistic
          label="正常数据占比"
          :value="qualitySummary ? qualitySummary.normalRate.toFixed(1) : '0.0'"
        >
          <template #suffix>%</template>
        </n-statistic>
      </n-card>
      <n-card :bordered="false" class="content-card">
        <n-statistic label="已选记录" :value="selectedRowKeys.length" />
      </n-card>
    </div>

    <div class="grid-2">
      <n-card :bordered="false" class="content-card" title="数据表格区">
        <n-space justify="space-between" style="margin-bottom: 16px;">
          <n-space>
            <n-button tertiary type="error" @click="handleBatchDelete">批量删除</n-button>
          </n-space>
          <n-text depth="3">共 {{ pagination.itemCount }} 条数据</n-text>
        </n-space>
        <n-data-table
          :columns="columns"
          :data="tableData"
          :row-key="(row: FlowRawData) => row.id"
          :pagination="pagination"
          :loading="loading"
          remote
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
          @update:checked-row-keys="selectedRowKeys = $event as number[]"
        />
      </n-card>

      <n-space vertical :size="16">
        <n-card :bordered="false" class="content-card" title="数据质量概览">
          <div class="chart">
            <v-chart class="chart" :option="qualityChartOption" autoresize />
          </div>
          <n-descriptions :column="1" label-placement="left" style="margin-top: 8px;">
            <n-descriptions-item label="总记录数">{{ qualitySummary?.total ?? 0 }}</n-descriptions-item>
            <n-descriptions-item label="正常">{{ qualitySummary?.normal ?? 0 }}</n-descriptions-item>
            <n-descriptions-item label="待校验">{{ qualitySummary?.warning ?? 0 }}</n-descriptions-item>
            <n-descriptions-item label="异常">{{ qualitySummary?.error ?? 0 }}</n-descriptions-item>
            <n-descriptions-item label="训练可用性">
              <n-tag :type="qualitySummary?.suitableForTraining ? 'success' : 'warning'">
                {{ qualitySummary?.suitableForTraining ? '适合训练' : '需谨慎' }}
              </n-tag>
            </n-descriptions-item>
          </n-descriptions>
        </n-card>

        <n-card :bordered="false" class="content-card" title="来源分布">
          <div class="chart">
            <v-chart class="chart" :option="sourceChartOption" autoresize />
          </div>
        </n-card>

        <n-card :bordered="false" class="content-card" title="流量趋势">
          <div class="chart">
            <v-chart class="chart" :option="trendChartOption" autoresize />
          </div>
        </n-card>
      </n-space>
    </div>

    <!-- Upload Modal -->
    <n-modal v-model:show="showUploadModal" title="导入数据" preset="card" style="width: 520px;">
      <n-space vertical>
        <n-form label-placement="left" label-width="100">
          <n-form-item label="选择文件">
            <n-upload
              :max="1"
              accept=".csv"
              @before-upload="(options: any) => { uploadFile = options.file.file; return false }"
            >
              <n-button>{{ uploadFile ? uploadFile.name : '点击上传 CSV' }}</n-button>
            </n-upload>
          </n-form-item>
          <n-form-item label="数据来源">
            <n-input v-model:value="uploadForm.dataSource" />
          </n-form-item>
          <n-form-item label="测点名称">
            <n-input v-model:value="uploadForm.stationCode" />
          </n-form-item>
        </n-form>
        <n-space justify="end">
          <n-button @click="showUploadModal = false">取消</n-button>
          <n-button type="primary" :loading="uploadLoading" @click="handleUpload">确认导入</n-button>
        </n-space>
      </n-space>
    </n-modal>

    <!-- Edit Modal -->
    <n-modal v-model:show="showEditModal" title="编辑数据" preset="card" style="width: 520px;">
      <n-form label-placement="left" label-width="100">
        <n-form-item label="监测时间">
          <n-input v-model:value="editingRow.monitorTime" />
        </n-form-item>
        <n-form-item label="观测水位">
          <n-input-number v-model:value="editingRow.observedWl" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="预测水位">
          <n-input-number v-model:value="editingRow.predictedWl" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="风速">
          <n-input-number v-model:value="editingRow.windSpeed" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="气压">
          <n-input-number v-model:value="editingRow.airPressure" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="气温">
          <n-input-number v-model:value="editingRow.airTemperature" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="水温">
          <n-input-number v-model:value="editingRow.waterTemperature" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="测点">
          <n-input v-model:value="editingRow.stationCode" />
        </n-form-item>
        <n-form-item label="来源">
          <n-input v-model:value="editingRow.dataSource" />
        </n-form-item>
        <n-form-item label="质量状态">
          <n-select v-model:value="editingRow.qualityStatus" :options="qualityOptions" />
        </n-form-item>
      </n-form>
      <n-space justify="end">
        <n-button @click="showEditModal = false">取消</n-button>
        <n-button type="primary" @click="handleEditSubmit">保存</n-button>
      </n-space>
    </n-modal>

    <!-- Dataset Modal -->
    <n-modal v-model:show="showDatasetModal" title="生成数据集" preset="card" style="width: 520px;">
      <n-form label-placement="left" label-width="100">
        <n-form-item label="数据集名称">
          <n-input v-model:value="datasetForm.name" placeholder="请输入名称" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="datasetForm.description" type="textarea" />
        </n-form-item>
        <n-form-item label="时间范围起">
          <n-date-picker v-model:value="datasetForm.startTime" type="datetime" style="width: 100%;" />
        </n-form-item>
        <n-form-item label="时间范围止">
          <n-date-picker v-model:value="datasetForm.endTime" type="datetime" style="width: 100%;" />
        </n-form-item>
      </n-form>
      <n-space justify="end">
        <n-button @click="showDatasetModal = false">取消</n-button>
        <n-button type="primary" @click="handleCreateDataset">创建</n-button>
      </n-space>
    </n-modal>
  </n-space>
</template>
