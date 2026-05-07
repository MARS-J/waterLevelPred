<script setup lang="ts">
const users = [
  { name: '管理员', role: '系统管理员', lastLogin: '2026-04-11 09:36', status: '正常' },
  { name: '分析员', role: '数据分析', lastLogin: '2026-04-11 08:58', status: '正常' },
  { name: '调度员', role: '业务查看', lastLogin: '2026-04-10 18:12', status: '停用' },
]

const params = [
  { label: '默认预测步长', value: '24' },
  { label: '训练任务并发数', value: '2' },
  { label: '数据保留周期', value: '365 天' },
]

const logs = [
  '2026-04-11 10:18  发布模型 TimeMixer-v1.2.0',
  '2026-04-11 09:52  执行预测任务 future-24h-0411',
  '2026-04-11 08:20  导入原始流量数据 12,480 条',
]
</script>

<template>
  <n-space vertical :size="20" class="page-section">
    <div class="page-header">
      <div>
        <h1 class="page-title">系统管理</h1>
        <p class="page-subtitle">维持后台管理风格的简洁布局，用于展示用户、参数和操作日志。</p>
      </div>
      <n-button type="primary">新增用户</n-button>
    </div>

    <div class="grid-3">
      <div v-for="item in params" :key="item.label" class="metric-panel">
        <div class="metric-label">{{ item.label }}</div>
        <div class="metric-value">{{ item.value }}</div>
      </div>
    </div>

    <div class="section-grid">
      <n-card :bordered="false" class="content-card soft-card" title="用户管理">
        <n-table :bordered="false" striped class="table-compact">
          <thead>
            <tr>
              <th>用户</th>
              <th>角色</th>
              <th>最近登录</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in users" :key="item.name">
              <td>{{ item.name }}</td>
              <td>{{ item.role }}</td>
              <td>{{ item.lastLogin }}</td>
              <td>
                <n-tag :type="item.status === '正常' ? 'success' : 'default'">{{ item.status }}</n-tag>
              </td>
            </tr>
          </tbody>
        </n-table>
      </n-card>

      <div class="section-side">
        <n-card :bordered="false" class="content-card soft-card">
          <div>
            <h3 class="panel-title">系统参数</h3>
            <p class="panel-subtitle">维持默认运行策略，减少页面上的无意义控制项。</p>
          </div>
          <div class="list-plain" style="margin-top: 16px;">
            <div v-for="item in params" :key="item.label" class="list-plain-item">
              <n-space justify="space-between" align="center">
                <span style="color: #122742; font-weight: 600;">{{ item.label }}</span>
                <span class="muted-text">{{ item.value }}</span>
              </n-space>
            </div>
          </div>
        </n-card>

        <n-card :bordered="false" class="content-card soft-card">
          <div>
            <h3 class="panel-title">操作日志</h3>
            <p class="panel-subtitle">只保留近期关键动作，方便演示和核对。</p>
          </div>
          <div class="list-plain" style="margin-top: 16px;">
            <div v-for="item in logs" :key="item" class="list-plain-item">
              {{ item }}
            </div>
          </div>
        </n-card>
      </div>
    </div>
  </n-space>
</template>
