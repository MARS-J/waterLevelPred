import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: {
        title: '登录页',
        guest: true,
      },
    },
    {
      path: '/',
      component: () => import('../layouts/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: {
            title: '首页驾驶舱',
          },
        },
        {
          path: 'data-management',
          name: 'data-management',
          component: () => import('../views/DataManagementView.vue'),
          meta: {
            title: '数据管理',
          },
        },
        {
          path: 'training-tasks',
          name: 'training-tasks',
          component: () => import('../views/TrainingTaskView.vue'),
          meta: {
            title: '训练任务',
          },
        },
        {
          path: 'prediction-analysis',
          name: 'prediction-analysis',
          component: () => import('../views/PredictionAnalysisView.vue'),
          meta: {
            title: '预测分析',
          },
        },
        {
          path: 'model-management',
          name: 'model-management',
          component: () => import('../views/ModelManagementView.vue'),
          meta: {
            title: '模型管理',
          },
        },
        {
          path: 'data-quality',
          name: 'data-quality',
          component: () => import('../views/DataQualityView.vue'),
          meta: {
            title: '数据质量分析',
          },
        },
        {
          path: 'statistics-analysis',
          name: 'statistics-analysis',
          component: () => import('../views/StatisticsAnalysisView.vue'),
          meta: {
            title: '数据统计分析',
          },
        },
        {
          path: 'system-management',
          name: 'system-management',
          component: () => import('../views/SystemManagementView.vue'),
          meta: {
            title: '系统管理',
          },
        },
        {
          path: 'visual-screen',
          name: 'visual-screen',
          component: () => import('../views/VisualScreenView.vue'),
          meta: {
            title: '可视化大屏',
          },
        },
        {
          path: 'water-plant-scene',
          name: 'water-plant-scene',
          component: () => import('../views/WaterPlantSceneView.vue'),
          meta: {
            title: '三维水厂场景',
          },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.guest && authStore.isLoggedIn) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
