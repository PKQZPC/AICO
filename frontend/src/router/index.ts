// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/personal',
    name: 'PersonalScenario',
    component: () => import('@/views/PersonalScenario.vue')
  },
  {
    path: '/personal-workbench',
    name: 'PersonalWorkbench',
    component: () => import('@/views/PersonalWorkbench.vue')
  },
  {
    path: '/expert',
    name: 'ExpertScenario',
    component: () => import('@/views/ExpertScenario.vue')
  },
  {
    path: '/expert-client',
    redirect: '/expert'
  },
  {
    path: '/parent-chat',
    name: 'ParentChat',
    component: () => import('@/views/ParentChat.vue')
  },
  {
    path: '/expert-chat',
    name: 'ExpertChat',
    component: () => import('@/views/ExpertChat.vue')
  },
  {
    path: '/decision-tree',
    name: 'DecisionTree',
    component: () => import('@/views/DecisionTree.vue')
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/Statistics.vue')
  },
  {
    path: '/aico-alignment',
    name: 'AicoAlignment',
    component: () => import('@/views/AicoAlignment.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
