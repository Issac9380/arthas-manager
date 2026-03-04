import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/cluster',
    name: 'Cluster',
    component: () => import('@/views/ClusterView.vue'),
    meta: { title: '集群管理' }
  },
  {
    path: '/diagnosis',
    name: 'Diagnosis',
    component: () => import('@/views/DiagnosisView.vue'),
    meta: { title: '诊断中心' }
  },
  {
    path: '/tools',
    name: 'Tools',
    component: () => import('@/views/ToolsView.vue'),
    meta: { title: '工具管理' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
