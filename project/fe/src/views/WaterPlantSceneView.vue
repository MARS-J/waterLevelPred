<script setup lang="ts">
import axios from 'axios'
import * as THREE from 'three'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

interface DevicePosition {
  x: number
  y: number
  z: number
}

interface Device {
  key: string
  name: string
  type: string
  current: number
  prediction: number
  trend: string
  status: '稳定' | '关注' | '告警'
  alarm: string
  score: number
  position: DevicePosition
  flowSpeed: number
}

interface SummaryItem {
  label: string
  value: string
}

interface WarningItem {
  name: string
  level: '高' | '中' | '低'
  detail: string
  value: number
}

interface LinkItem {
  from: string
  to: string
}

interface OverviewItem {
  currentValue: number
  peakTime: string
  peakValue: number
  averageValue: number
  rise: number
}

interface SceneData {
  generatedAt: string
  algorithmStatus: string
  summary: SummaryItem[]
  devices: Device[]
  warnings: WarningItem[]
  links: LinkItem[]
  overview: OverviewItem
}

interface FlowParticle {
  mesh: THREE.Mesh
  curve: THREE.CatmullRomCurve3
  offset: number
  speed: number
}

const sceneHost = ref<HTMLDivElement | null>(null)
const loading = ref(false)
const loadError = ref('')
const sceneData = ref<SceneData | null>(null)
const activeKey = ref('')

const statusTypeMap = {
  稳定: 'success',
  关注: 'warning',
  告警: 'error',
} as const

const statusColorMap: Record<Device['status'], string> = {
  稳定: '#38bdf8',
  关注: '#f59e0b',
  告警: '#ef4444',
}

let timer: number | null = null
let frameId = 0
let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let dynamicGroup: THREE.Group | null = null
const deviceMeshes = new Map<string, THREE.Mesh>()
const flowParticles: FlowParticle[] = []
const raycaster = new THREE.Raycaster()
const pointer = new THREE.Vector2()
const clock = new THREE.Clock()

const devices = computed(() => sceneData.value?.devices ?? [])
const warnings = computed(() => sceneData.value?.warnings ?? [])
const summary = computed(() => sceneData.value?.summary ?? [])
const activeDevice = computed(
  () => devices.value.find((item) => item.key === activeKey.value) ?? devices.value[0] ?? null,
)

const fetchSceneData = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/visual/scene/status')
    sceneData.value = response.data?.data ?? null
    if (!activeKey.value && sceneData.value?.devices.length) {
      activeKey.value = sceneData.value.devices[0].key
    }
    loadError.value = ''
    rebuildScene()
  } catch {
    loadError.value = '场景数据加载失败，请检查后端服务。'
  } finally {
    loading.value = false
  }
}

const initScene = () => {
  if (!sceneHost.value) {
    return
  }

  scene = new THREE.Scene()
  scene.background = new THREE.Color('#061321')
  scene.fog = new THREE.Fog('#061321', 18, 36)

  camera = new THREE.PerspectiveCamera(
    42,
    sceneHost.value.clientWidth / sceneHost.value.clientHeight,
    0.1,
    100,
  )
  camera.position.set(0, 13, 24)
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setPixelRatio(window.devicePixelRatio)
  renderer.setSize(sceneHost.value.clientWidth, sceneHost.value.clientHeight)
  sceneHost.value.innerHTML = ''
  sceneHost.value.appendChild(renderer.domElement)

  const ambientLight = new THREE.AmbientLight('#c4ddff', 1.4)
  const directionalLight = new THREE.DirectionalLight('#78b7ff', 2.4)
  directionalLight.position.set(8, 16, 10)
  const pointLight = new THREE.PointLight('#22d3ee', 50, 40)
  pointLight.position.set(-8, 8, 6)
  scene.add(ambientLight, directionalLight, pointLight)

  const ground = new THREE.Mesh(
    new THREE.CircleGeometry(18, 64),
    new THREE.MeshStandardMaterial({
      color: '#0b2138',
      metalness: 0.35,
      roughness: 0.7,
      emissive: '#08192d',
      emissiveIntensity: 0.25,
    }),
  )
  ground.rotation.x = -Math.PI / 2
  ground.position.y = -0.2
  scene.add(ground)

  const ring = new THREE.Mesh(
    new THREE.RingGeometry(10.8, 11.1, 72),
    new THREE.MeshBasicMaterial({ color: '#1d4ed8', transparent: true, opacity: 0.32 }),
  )
  ring.rotation.x = -Math.PI / 2
  ring.position.y = -0.18
  scene.add(ring)

  dynamicGroup = new THREE.Group()
  scene.add(dynamicGroup)

  renderer.domElement.addEventListener('pointerdown', handlePointerDown)
  window.addEventListener('resize', handleResize)
  animate()
}

const clearDynamicScene = () => {
  if (!dynamicGroup) {
    return
  }

  while (dynamicGroup.children.length) {
    const child = dynamicGroup.children[0]
    dynamicGroup.remove(child)
    if (child instanceof THREE.Mesh) {
      child.geometry.dispose()
      if (Array.isArray(child.material)) {
        child.material.forEach((material: THREE.Material) => material.dispose())
      } else {
        child.material.dispose()
      }
    }
  }
  deviceMeshes.clear()
  flowParticles.splice(0, flowParticles.length)
}

const rebuildScene = () => {
  if (!sceneData.value || !dynamicGroup) {
    return
  }

  clearDynamicScene()

  const deviceMap = new Map(sceneData.value.devices.map((item) => [item.key, item]))
  for (const link of sceneData.value.links) {
    const fromDevice = deviceMap.get(link.from)
    const toDevice = deviceMap.get(link.to)
    if (!fromDevice || !toDevice) {
      continue
    }

    const start = new THREE.Vector3(fromDevice.position.x, 0.9, fromDevice.position.z)
    const end = new THREE.Vector3(toDevice.position.x, 0.9, toDevice.position.z)
    const middle = start.clone().lerp(end, 0.5)
    middle.y += 1.8
    const curve = new THREE.CatmullRomCurve3([start, middle, end])
    const tube = new THREE.Mesh(
      new THREE.TubeGeometry(curve, 42, 0.16, 12, false),
      new THREE.MeshStandardMaterial({
        color: '#11345b',
        metalness: 0.45,
        roughness: 0.42,
        emissive: '#0f3c6f',
        emissiveIntensity: 0.5,
      }),
    )
    dynamicGroup.add(tube)

    const flow = new THREE.Mesh(
      new THREE.SphereGeometry(0.2, 20, 20),
      new THREE.MeshBasicMaterial({ color: '#67e8f9' }),
    )
    dynamicGroup.add(flow)
    flowParticles.push({
      mesh: flow,
      curve,
      offset: Math.random(),
      speed: 0.06 + (fromDevice.flowSpeed + toDevice.flowSpeed) * 0.01,
    })
  }

  for (const device of sceneData.value.devices) {
    const material = new THREE.MeshStandardMaterial({
      color: statusColorMap[device.status],
      emissive: statusColorMap[device.status],
      emissiveIntensity: 0.32,
      metalness: 0.4,
      roughness: 0.34,
    })
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(1.8, 1.2, 1.8), material)
    mesh.position.set(device.position.x, device.position.y, device.position.z)
    mesh.userData = { key: device.key }
    deviceMeshes.set(device.key, mesh)
    dynamicGroup.add(mesh)

    const halo = new THREE.Mesh(
      new THREE.CylinderGeometry(1.2, 1.2, 0.08, 48),
      new THREE.MeshBasicMaterial({
        color: statusColorMap[device.status],
        transparent: true,
        opacity: 0.18,
      }),
    )
    halo.position.set(device.position.x, 0.05, device.position.z)
    dynamicGroup.add(halo)
  }

  updateActiveState()
}

const updateActiveState = () => {
  for (const [key, mesh] of deviceMeshes.entries()) {
    const isActive = key === activeKey.value
    mesh.scale.setScalar(isActive ? 1.16 : 1)
    const material = mesh.material as THREE.MeshStandardMaterial
    material.emissiveIntensity = isActive ? 0.85 : 0.32
  }
}

const animate = () => {
  if (!renderer || !scene || !camera) {
    return
  }

  const elapsed = clock.getElapsedTime()
  camera.position.x = Math.sin(elapsed * 0.18) * 4
  camera.position.z = 24 + Math.cos(elapsed * 0.18) * 2.2
  camera.lookAt(0, 1.6, 0)

  for (const particle of flowParticles) {
    particle.offset = (particle.offset + particle.speed * 0.0025) % 1
    const point = particle.curve.getPointAt(particle.offset)
    particle.mesh.position.copy(point)
  }

  renderer.render(scene, camera)
  frameId = window.requestAnimationFrame(animate)
}

const handleResize = () => {
  if (!renderer || !camera || !sceneHost.value) {
    return
  }

  const width = sceneHost.value.clientWidth
  const height = sceneHost.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

const handlePointerDown = (event: PointerEvent) => {
  if (!renderer || !camera) {
    return
  }

  const bounds = renderer.domElement.getBoundingClientRect()
  pointer.x = ((event.clientX - bounds.left) / bounds.width) * 2 - 1
  pointer.y = -((event.clientY - bounds.top) / bounds.height) * 2 + 1
  raycaster.setFromCamera(pointer, camera)
  const intersects = raycaster.intersectObjects(Array.from(deviceMeshes.values()))
  if (intersects[0]?.object.userData.key) {
    activeKey.value = String(intersects[0].object.userData.key)
  }
}

watch(activeKey, () => {
  updateActiveState()
})

onMounted(async () => {
  await nextTick()
  initScene()
  fetchSceneData()
  timer = window.setInterval(fetchSceneData, 30000)
})

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer)
  }
  window.cancelAnimationFrame(frameId)
  window.removeEventListener('resize', handleResize)
  renderer?.domElement.removeEventListener('pointerdown', handlePointerDown)
  clearDynamicScene()
  renderer?.dispose()
})
</script>

<template>
  <div class="scene-page">
    <div class="scene-shell">
      <div class="scene-topbar">
        <div>
          <h1 class="scene-title">三维水厂场景</h1>
          <p class="scene-subtitle">基于 Three.js 渲染真实场景联动，用设备状态与预测结果驱动空间表达。</p>
        </div>
        <n-space>
          <n-tag round type="info">Three.js 场景</n-tag>
          <n-tag round :type="sceneData?.algorithmStatus === '运行中' ? 'success' : 'warning'">
            算法 {{ sceneData?.algorithmStatus ?? '检查中' }}
          </n-tag>
        </n-space>
      </div>

      <n-alert v-if="loadError" type="error" style="margin-bottom: 16px;">
        {{ loadError }}
      </n-alert>

      <div class="scene-layout">
        <div class="scene-card scene-stage">
          <n-spin :show="loading">
            <div ref="sceneHost" class="scene-canvas"></div>
            <div class="scene-overlay">
              <div class="scene-overlay-badge">
                更新时间 {{ sceneData?.generatedAt ?? '--' }}
              </div>
              <div class="scene-overlay-tip">点击 3D 设备或下方芯片可切换详情</div>
            </div>
            <div class="scene-device-chips">
              <n-tag
                v-for="item in devices"
                :key="item.key"
                round
                checkable
                :checked="item.key === activeKey"
                :type="statusTypeMap[item.status]"
                @click="activeKey = item.key"
              >
                {{ item.name }}
              </n-tag>
            </div>
          </n-spin>
        </div>

        <div class="scene-side">
          <div class="scene-card scene-panel">
            <h3 class="panel-title">场景总览</h3>
            <p class="panel-subtitle">从在线设备、风险节点和刷新延迟观察当前联动状态。</p>
            <div class="scene-kpi-row">
              <div v-for="item in summary" :key="item.label" class="scene-kpi">
                <div class="metric-label">{{ item.label }}</div>
                <div class="metric-value">{{ item.value }}</div>
              </div>
            </div>
          </div>

          <div class="scene-card scene-panel">
            <h3 class="panel-title">设备详情</h3>
            <p class="panel-subtitle">右侧面板与 Three.js 场景保持同步。</p>
            <n-empty v-if="!activeDevice" description="暂无设备数据" />
            <n-descriptions v-else :column="1" label-placement="left" style="margin-top: 16px;">
              <n-descriptions-item label="设备名称">{{ activeDevice.name }}</n-descriptions-item>
              <n-descriptions-item label="类型">{{ activeDevice.type }}</n-descriptions-item>
              <n-descriptions-item label="当前值">{{ activeDevice.current.toFixed(3) }} m</n-descriptions-item>
              <n-descriptions-item label="预测值">{{ activeDevice.prediction.toFixed(3) }} m</n-descriptions-item>
              <n-descriptions-item label="变化趋势">{{ activeDevice.trend }}</n-descriptions-item>
              <n-descriptions-item label="设备评分">{{ activeDevice.score }}</n-descriptions-item>
              <n-descriptions-item label="状态">
                <n-tag round :type="statusTypeMap[activeDevice.status]">{{ activeDevice.status }}</n-tag>
              </n-descriptions-item>
              <n-descriptions-item label="最近告警">{{ activeDevice.alarm }}</n-descriptions-item>
            </n-descriptions>
          </div>

          <div class="scene-card scene-panel">
            <h3 class="panel-title">预测摘要</h3>
            <p class="panel-subtitle">抽取本轮场景联动使用的关键预测结论。</p>
            <div class="summary-grid">
              <div class="summary-item">
                <span>当前水位</span>
                <strong>{{ sceneData?.overview.currentValue?.toFixed(3) ?? '--' }} m</strong>
              </div>
              <div class="summary-item">
                <span>峰值时刻</span>
                <strong>{{ sceneData?.overview.peakTime ?? '--' }}</strong>
              </div>
              <div class="summary-item">
                <span>峰值水位</span>
                <strong>{{ sceneData?.overview.peakValue?.toFixed(3) ?? '--' }} m</strong>
              </div>
              <div class="summary-item">
                <span>平均预测</span>
                <strong>{{ sceneData?.overview.averageValue?.toFixed(3) ?? '--' }} m</strong>
              </div>
            </div>
          </div>

          <div class="scene-card scene-panel">
            <h3 class="panel-title">重点告警</h3>
            <p class="panel-subtitle">保留最有解释价值的场景告警，避免页面拥挤。</p>
            <div class="warning-list">
              <div v-for="item in warnings" :key="item.name" class="warning-item">
                <div>
                  <div class="warning-title">{{ item.name }}</div>
                  <div class="panel-subtitle">{{ item.detail }}</div>
                </div>
                <n-tag round :type="item.level === '高' ? 'error' : item.level === '中' ? 'warning' : 'success'">
                  {{ item.level }}
                </n-tag>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.scene-page {
  color: #e7f2ff;
}

.scene-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scene-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.scene-title {
  margin: 0;
  font-size: 32px;
  font-weight: 700;
}

.scene-subtitle,
.panel-subtitle {
  color: rgba(203, 222, 244, 0.72);
  line-height: 1.7;
}

.scene-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(360px, 420px);
  gap: 16px;
}

.scene-card {
  border: 1px solid rgba(113, 158, 216, 0.12);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(9, 25, 43, 0.92) 0%, rgba(7, 19, 34, 0.94) 100%);
  box-shadow: 0 24px 80px rgba(6, 16, 30, 0.24);
}

.scene-stage {
  position: relative;
  padding: 16px;
}

.scene-canvas {
  position: relative;
  height: 680px;
  border-radius: 20px;
  overflow: hidden;
  background:
    radial-gradient(circle at top, rgba(43, 111, 198, 0.16), transparent 52%),
    linear-gradient(180deg, #081524 0%, #06111d 100%);
}

.scene-overlay {
  pointer-events: none;
  position: absolute;
  top: 28px;
  left: 28px;
  right: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.scene-overlay-badge,
.scene-overlay-tip {
  padding: 8px 12px;
  border-radius: 14px;
  background: rgba(6, 19, 34, 0.72);
  color: rgba(223, 236, 255, 0.88);
  font-size: 13px;
}

.scene-device-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.scene-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scene-panel {
  padding: 20px;
}

.panel-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.scene-kpi-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.scene-kpi,
.summary-item,
.warning-item {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(113, 158, 216, 0.12);
  background: rgba(255, 255, 255, 0.03);
}

.metric-label {
  color: rgba(203, 222, 244, 0.62);
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.summary-item span {
  display: block;
  color: rgba(203, 222, 244, 0.62);
  font-size: 13px;
}

.summary-item strong {
  display: block;
  margin-top: 10px;
  font-size: 20px;
  font-weight: 700;
}

.warning-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.warning-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.warning-title {
  font-weight: 600;
}

@media (max-width: 1400px) {
  .scene-layout {
    grid-template-columns: 1fr;
  }

  .scene-canvas {
    height: 540px;
  }
}
</style>
